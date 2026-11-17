package com.pishi.iweather.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pishi.iweather.MainActivity
import com.pishi.iweather.R
import com.pishi.iweather.data.POPULAR_IRAN_CITIES
import com.pishi.iweather.data.WeatherApi
import com.pishi.iweather.data.WeatherStore
import com.pishi.iweather.util.Insights
import com.pishi.iweather.util.SettingsStore
import com.pishi.iweather.util.strings
import java.time.LocalDate
import java.time.LocalTime

/**
 * Periodic background check: fetches fresh weather for the saved city and posts
 * notifications for newly-appearing alerts of the user-enabled types only.
 * Honest limits: WorkManager timing is approximate (OS-controlled), quiet hours
 * are respected, and each alert type notifies at most once per day per city.
 */
class AlertCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val settings = SettingsStore(ctx).load()
        if (!settings.notifEnabled) return Result.success()
        if (settings.quietEnabled && inQuietHours(settings.quietStart, settings.quietEnd)) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val city = WeatherStore.savedCity(ctx) ?: POPULAR_IRAN_CITIES.first()
        val api = WeatherApi()
        val raw = try {
            api.fetchRaw(city)
        } catch (e: Exception) {
            return Result.retry()
        }
        val fetchedAt = System.currentTimeMillis()
        val report = try {
            api.parseReport(city, raw, fetchedAt)
        } catch (e: Exception) {
            return Result.failure()
        }
        WeatherStore.saveCache(ctx, city, raw, fetchedAt)

        val fresh = Insights.alerts(report, settings)
            .filter { it.type in settings.notifTypes }
        if (fresh.isEmpty()) return Result.success()

        val prefs = ctx.getSharedPreferences("iweather_alerts_seen", Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val seen = prefs.getStringSet("seen", emptySet()) ?: emptySet()
        val newSeen = seen.toMutableSet()
        var posted = 0
        for (alert in fresh) {
            if (posted >= 3) break
            val key = "$today|${city.name}|${alert.type.name}"
            if (key in seen) continue
            post(ctx, city.name, alert.emoji, alert.text, key.hashCode())
            newSeen += key
            posted++
        }
        // Keep the seen-set bounded: drop keys older than today.
        val pruned = newSeen.filter { it.startsWith(today) }.toSet()
        prefs.edit().putStringSet("seen", pruned).apply()
        return Result.success()
    }

    private fun inQuietHours(start: Int, end: Int): Boolean {
        val h = LocalTime.now().hour
        return if (start <= end) h in start until end else h >= start || h < end
    }

    private fun post(context: Context, cityName: String, emoji: String, text: String, id: Int) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, strings(SettingsStore(context).load().lang).chAlerts, NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_weather)
            .setContentTitle("$emoji $cityName")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission revoked between check and post; nothing to do.
        }
    }

    companion object {
        const val CHANNEL_ID = "iweather_alerts"
    }
}

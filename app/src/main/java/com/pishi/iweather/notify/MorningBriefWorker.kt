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
import com.pishi.iweather.data.wmoInfo
import com.pishi.iweather.util.Fmt
import com.pishi.iweather.util.Insights
import com.pishi.iweather.util.SettingsStore
import com.pishi.iweather.util.strings

/**
 * Daily "morning brief": one notification at the user's chosen hour with today's conditions,
 * what to wear and the rain window — built from the same on-device rules as the app, no AI call.
 * WorkManager timing is approximate (the OS may shift it by minutes under Doze / battery saver).
 */
class MorningBriefWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val st = SettingsStore(ctx).load()
        if (!st.briefEnabled) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val city = WeatherStore.savedCity(ctx) ?: POPULAR_IRAN_CITIES.first()
        val api = WeatherApi()
        val report = try {
            val raw = api.fetchRaw(city)
            val now = System.currentTimeMillis()
            api.parseReport(city, raw, now).also { WeatherStore.saveCache(ctx, city, raw, now) }
        } catch (e: Exception) {
            WeatherStore.cachedReport(ctx, city, api) ?: return Result.retry()
        }

        val s = strings(st.lang)
        val cur = report.current
        val info = wmoInfo(cur.weatherCode, cur.isDay)
        val label = if (st.isFa) info.labelFa else info.labelEn
        val today = report.daily.firstOrNull()

        val title = "${info.emoji} ${city.name} · ${Fmt.temp(cur.temperature, st)}"
        val lines = ArrayList<String>()
        lines += buildString {
            append(label)
            append(" · ").append(s.feelsLike).append(' ').append(Fmt.temp(cur.feelsLike, st))
            if (today != null) append(" · ").append(Fmt.temp(today.tempMin, st)).append(" / ").append(Fmt.temp(today.tempMax, st))
        }
        val clothing = Insights.clothing(report, st)
        lines += "${clothing.emoji} ${clothing.text}"
        Insights.rainWindow(report, st)?.let { lines += "${it.emoji} ${it.text}" }
        val body = lines.joinToString("\n")

        post(ctx, s.chBrief, title, body)
        return Result.success()
    }

    private fun post(context: Context, channelName: String, title: String, body: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT))
        }
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_weather)
            .setContentTitle(title)
            .setContentText(body.substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the post; nothing to do.
        }
    }

    companion object {
        const val CHANNEL_ID = "iweather_brief"
        private const val NOTIFICATION_ID = 7001
    }
}

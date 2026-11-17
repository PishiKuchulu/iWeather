package com.pishi.iweather.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pishi.iweather.util.AppSettings
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Keeps the periodic alert-check work and the daily morning-brief work in sync with the user's settings. */
object AlertScheduler {
    private const val WORK_NAME = "iweather_alert_checks"
    private const val BRIEF_WORK = "iweather_morning_brief"

    fun reschedule(context: Context, settings: AppSettings) {
        val wm = WorkManager.getInstance(context)

        if (!settings.notifEnabled) {
            wm.cancelUniqueWork(WORK_NAME)
        } else {
            val request = PeriodicWorkRequestBuilder<AlertCheckWorker>(
                settings.notifIntervalH.toLong().coerceAtLeast(1), TimeUnit.HOURS,
            ).build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        if (!settings.briefEnabled) {
            wm.cancelUniqueWork(BRIEF_WORK)
        } else {
            // First run at the next occurrence of the chosen hour, then every 24 h.
            val request = PeriodicWorkRequestBuilder<MorningBriefWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(millisUntilNext(settings.briefHour), TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniquePeriodicWork(BRIEF_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }

    /** Milliseconds from now until the next [hour]:00 local time (zone-aware, so DST changes are handled). */
    internal fun millisUntilNext(hour: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
        var next = now.withHour(hour.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now.plusMinutes(1))) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }
}

package com.pishi.iweather.notify

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
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

    /** Both workers fetch weather: defer to connectivity instead of waking up to fail (AUD-005). */
    private val networkConstraints: Constraints
        get() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun reschedule(context: Context, settings: AppSettings) {
        val wm = WorkManager.getInstance(context)

        if (!settings.notifEnabled) {
            wm.cancelUniqueWork(WORK_NAME)
        } else {
            val request = PeriodicWorkRequestBuilder<AlertCheckWorker>(
                settings.notifIntervalH.toLong().coerceAtLeast(1), TimeUnit.HOURS,
            )
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        if (!settings.briefEnabled) {
            wm.cancelUniqueWork(BRIEF_WORK)
        } else {
            // First run at the next occurrence of the chosen hour, then every 24 h.
            val request = PeriodicWorkRequestBuilder<MorningBriefWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(millisUntilNext(settings.briefHour), TimeUnit.MILLISECONDS)
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(BRIEF_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}

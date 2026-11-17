package com.pishi.iweather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pishi.iweather.data.POPULAR_IRAN_CITIES
import com.pishi.iweather.data.WeatherApi
import com.pishi.iweather.data.WeatherStore
import java.util.concurrent.TimeUnit

/** Renders all placed widgets and owns their background refresh schedule. */
object WidgetUpdater {

    private val PROVIDERS: List<Pair<Class<*>, WidgetSize>> = listOf(
        SmallWidget::class.java to WidgetSize.SMALL,
        MediumWidget::class.java to WidgetSize.MEDIUM,
        LargeWidget::class.java to WidgetSize.LARGE,
    )

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        for ((cls, size) in PROVIDERS) {
            val ids = manager.getAppWidgetIds(ComponentName(context, cls))
            for (id in ids) updateOne(context, manager, cls, size, id)
        }
    }

    fun updateOne(
        context: Context,
        manager: AppWidgetManager,
        providerClass: Class<*>,
        size: WidgetSize,
        appWidgetId: Int,
    ) {
        val options = runCatching { manager.getAppWidgetOptions(appWidgetId) }.getOrNull()
        val views = WidgetRenderer.render(context, appWidgetId, size, providerClass, options)
        manager.updateAppWidget(appWidgetId, views)
    }

    /** Called after the app itself fetched fresh data. */
    fun requestUpdate(context: Context) = updateAll(context)

    /** Re-render one widget, resolving its size from its provider info (used by the config screen). */
    fun updateById(context: Context, appWidgetId: Int) {
        val manager = AppWidgetManager.getInstance(context)
        val info = manager.getAppWidgetInfo(appWidgetId) ?: return
        val entry = PROVIDERS.firstOrNull { it.first.name == info.provider.className } ?: return
        updateOne(context, manager, entry.first, entry.second, appWidgetId)
    }

    /** Manual refresh from a widget button: fetch fresh data, then re-render. */
    fun enqueueRefresh(context: Context) {
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build())
    }

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "iweather_widget_refresh", ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }
}

/**
 * Fetches fresh weather for the app city + every widget-pinned city (deduped),
 * stores it in the shared/per-city caches, then re-renders all widgets.
 * Without network it simply re-renders from cache.
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val api = WeatherApi()
        val cities = LinkedHashMap<String, com.pishi.iweather.data.City>()
        val appCity = WeatherStore.savedCity(ctx) ?: POPULAR_IRAN_CITIES.first()
        cities["${appCity.latitude},${appCity.longitude}"] = appCity
        val manager = AppWidgetManager.getInstance(ctx)
        val widgetIds = WidgetConfigStore.allIds(ctx)
        for (id in widgetIds) {
            WidgetConfigStore.load(ctx, id).city()?.let { c ->
                cities["${c.latitude},${c.longitude}"] = c
            }
        }
        for (city in cities.values) {
            try {
                val raw = api.fetchRaw(city)
                if (city.latitude == appCity.latitude && city.longitude == appCity.longitude) {
                    WeatherStore.saveCache(ctx, city, raw, System.currentTimeMillis())
                }
                WidgetData.save(ctx, city, raw)
            } catch (e: Exception) {
                // Offline: widgets keep showing cached data.
            }
        }
        WidgetUpdater.updateAll(ctx)
        return Result.success()
    }
}

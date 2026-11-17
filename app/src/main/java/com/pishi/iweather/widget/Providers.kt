package com.pishi.iweather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

abstract class IWeatherWidgetProvider(private val size: WidgetSize) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            WidgetUpdater.updateOne(context, manager, this::class.java, size, id)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle,
    ) {
        WidgetUpdater.updateOne(context, manager, this::class.java, size, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetRenderer.ACTION_REFRESH) {
            WidgetUpdater.enqueueRefresh(context)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetConfigStore.delete(context, id)
    }
}

class SmallWidget : IWeatherWidgetProvider(WidgetSize.SMALL)
class MediumWidget : IWeatherWidgetProvider(WidgetSize.MEDIUM)
class LargeWidget : IWeatherWidgetProvider(WidgetSize.LARGE)

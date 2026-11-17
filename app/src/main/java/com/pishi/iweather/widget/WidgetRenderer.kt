package com.pishi.iweather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.pishi.iweather.MainActivity
import com.pishi.iweather.R
import com.pishi.iweather.data.POPULAR_IRAN_CITIES
import com.pishi.iweather.data.SkyKind
import com.pishi.iweather.data.WeatherStore
import com.pishi.iweather.data.wmoInfo
import com.pishi.iweather.util.Dates
import com.pishi.iweather.util.Fmt
import com.pishi.iweather.util.Insights
import com.pishi.iweather.util.SettingsStore
import com.pishi.iweather.util.strings
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Builds the RemoteViews for one widget instance from the shared caches. */
object WidgetRenderer {

    const val ACTION_REFRESH = "com.pishi.iweather.widget.ACTION_REFRESH"

    fun render(
        context: Context,
        appWidgetId: Int,
        size: WidgetSize,
        providerClass: Class<*>,
        options: Bundle? = null,
    ): RemoteViews {
        val st = SettingsStore(context).load()
        val s = strings(st.lang)
        val cfg = WidgetConfigStore.load(context, appWidgetId)
        val city = cfg.city() ?: WeatherStore.savedCity(context) ?: POPULAR_IRAN_CITIES.first()
        val report = WidgetData.load(context, city)

        if (report == null) {
            val views = RemoteViews(context.packageName, R.layout.widget_empty)
            views.setTextViewText(R.id.w_no_data, s.wNoData)
            views.setInt(R.id.widget_root, "setBackgroundResource", bgFor(cfg.style, null))
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            return views
        }

        val cur = report.current
        val info = wmoInfo(cur.weatherCode, cur.isDay)
        val label = if (st.isFa) info.labelFa else info.labelEn
        val today = report.daily.firstOrNull()
        val tall = size == WidgetSize.LARGE &&
            (options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0) >= 200
        val layout = when (size) {
            WidgetSize.SMALL -> R.layout.widget_small
            WidgetSize.MEDIUM -> R.layout.widget_medium
            WidgetSize.LARGE -> if (tall) R.layout.widget_large_tall else R.layout.widget_large
        }
        val views = RemoteViews(context.packageName, layout)
        views.setInt(R.id.widget_root, "setBackgroundResource", bgFor(cfg.style, info.kind))
        views.setTextViewText(R.id.w_city, city.name)
        views.setTextViewText(R.id.w_emoji, info.emoji)
        views.setTextViewText(R.id.w_temp, Fmt.temp(cur.temperature, st))
        views.setTextViewText(R.id.w_cond, label)
        if (today != null) {
            views.setTextViewText(
                R.id.w_minmax,
                "${Fmt.temp(today.tempMin, st)} / ${Fmt.temp(today.tempMax, st)}",
            )
        }
        val updated = SimpleDateFormat("HH:mm", Locale.US).format(Date(report.fetchedAtMillis))
        views.setTextViewText(R.id.w_updated, "${s.wUpdated} ${Fmt.num(updated, st.lang)}")
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))

        if (size != WidgetSize.SMALL) {
            views.setOnClickPendingIntent(
                R.id.w_refresh,
                refreshIntent(context, providerClass, appWidgetId),
            )
            if (cfg.showFeels) {
                views.setTextViewText(R.id.w_feels, "${s.feelsLike} ${Fmt.temp(cur.feelsLike, st)}")
            } else {
                views.setViewVisibility(R.id.w_feels, View.GONE)
            }
        }

        if (size == WidgetSize.MEDIUM && cfg.showHourly) {
            val hourIds = intArrayOf(R.id.w_h1, R.id.w_h2, R.id.w_h3, R.id.w_h4)
            report.hourly.take(4).forEachIndexed { i, h ->
                val hi = wmoInfo(h.weatherCode, h.isDay)
                views.setTextViewText(
                    hourIds[i],
                    "${Fmt.clock(h.hourLabel, st)}\n${hi.emoji} ${Fmt.temp(h.temperature, st)}\n${Fmt.percent(h.precipProbability, st.lang)}",
                )
            }
        }

        if (size == WidgetSize.LARGE) {
            views.setTextViewText(
                R.id.w_details,
                "💨 ${Fmt.wind(cur.windSpeed, st)}   💧 ${Fmt.percent(cur.humidity, st.lang)}",
            )
            val extra = StringBuilder()
            today?.let { extra.append("☀️ UV ${Fmt.num(it.uvMax.roundToInt(), st.lang)}") }
            if (cfg.showAqi) {
                report.aqiUs?.let {
                    if (extra.isNotEmpty()) extra.append("   ")
                    extra.append("🍃 AQI ${Fmt.num(it, st.lang)}")
                }
            }
            views.setTextViewText(R.id.w_extra, extra.toString())
            val dayIds = intArrayOf(R.id.w_d1, R.id.w_d2, R.id.w_d3, R.id.w_d4, R.id.w_d5)
            report.daily.take(5).forEachIndexed { i, d ->
                val di = wmoInfo(d.weatherCode)
                val date = runCatching { LocalDate.parse(d.dateIso) }.getOrNull()
                val dayName = if (date != null) Dates.weekday(date, st.lang) else ""
                views.setTextViewText(
                    dayIds[i],
                    "$dayName  ${di.emoji}  ${Fmt.temp(d.tempMin, st)} – ${Fmt.temp(d.tempMax, st)}",
                )
            }
            if (tall) {
                if (cfg.showHourly) {
                    val hourIds = intArrayOf(R.id.w_h1, R.id.w_h2, R.id.w_h3, R.id.w_h4, R.id.w_h5, R.id.w_h6)
                    report.hourly.take(6).forEachIndexed { i, h ->
                        val hi = wmoInfo(h.weatherCode, h.isDay)
                        views.setTextViewText(
                            hourIds[i],
                            "${Fmt.clock(h.hourLabel, st)}\n${hi.emoji} ${Fmt.temp(h.temperature, st)}\n${Fmt.percent(h.precipProbability, st.lang)}",
                        )
                    }
                }
                today?.let { d ->
                    // Empty/odd ISO strings (polar day/night, missing data) must not crash the widget.
                    val rise = Fmt.isoHm(d.sunriseIso)?.let { Fmt.clock(it, st) } ?: "—"
                    val set = Fmt.isoHm(d.sunsetIso)?.let { Fmt.clock(it, st) } ?: "—"
                    views.setTextViewText(R.id.w_sun, "🌅 $rise    🌇 $set")
                }
                val firstAlert = Insights.alerts(report, st).firstOrNull()
                views.setTextViewText(
                    R.id.w_alerts,
                    firstAlert?.let { "${it.emoji} ${it.text}" } ?: s.noAlerts,
                )
            }
        }
        return views
    }

    private fun bgFor(style: WidgetStyle, kind: SkyKind?): Int = when (style) {
        WidgetStyle.GLASS -> R.drawable.widget_bg_glass
        WidgetStyle.TRANSPARENT, WidgetStyle.MINIMAL -> R.drawable.widget_bg_transparent
        WidgetStyle.MATERIAL -> R.drawable.widget_bg_material
        WidgetStyle.DYNAMIC -> when (kind) {
            SkyKind.CLEAR_DAY -> R.drawable.widget_bg_clear_day
            SkyKind.CLEAR_NIGHT -> R.drawable.widget_bg_clear_night
            SkyKind.CLOUDY -> R.drawable.widget_bg_cloudy
            SkyKind.RAIN -> R.drawable.widget_bg_rain
            SkyKind.SNOW -> R.drawable.widget_bg_snow
            SkyKind.FOG -> R.drawable.widget_bg_fog
            SkyKind.STORM -> R.drawable.widget_bg_storm
            null -> R.drawable.widget_bg_glass
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun refreshIntent(context: Context, providerClass: Class<*>, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, providerClass)
            .setAction(ACTION_REFRESH)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
            context, appWidgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

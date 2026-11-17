package com.pishi.iweather.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import com.pishi.iweather.ui.theme.IWeatherTheme
import com.pishi.iweather.ui.theme.Luna
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pishi.iweather.data.POPULAR_IRAN_CITIES
import com.pishi.iweather.data.WeatherStore
import com.pishi.iweather.data.wmoInfo
import com.pishi.iweather.util.Fmt
import com.pishi.iweather.util.SettingsStore
import com.pishi.iweather.util.strings

/** Per-widget configuration screen, shown by the launcher when a widget is added (and re-openable from the launcher). */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Standard contract: default to CANCELED until the user saves.
        setResult(Activity.RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val settings = SettingsStore(this).load()
        val initial = WidgetConfigStore.load(this, appWidgetId)
        setContent {
            IWeatherTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (settings.isFa) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Luna.Bg) {
                        ConfigContent(
                            initial = initial,
                            settings = settings,
                            onSave = { cfg ->
                                WidgetConfigStore.save(this@WidgetConfigActivity, appWidgetId, cfg)
                                WidgetUpdater.updateById(this@WidgetConfigActivity, appWidgetId)
                                val result = Intent()
                                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                setResult(Activity.RESULT_OK, result)
                                finish()
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ConfigContent(
        initial: WidgetConfig,
        settings: com.pishi.iweather.util.AppSettings,
        onSave: (WidgetConfig) -> Unit,
    ) {
        val s = strings(settings.lang)
        var cfg by remember { mutableStateOf(initial) }
        val appCity = WeatherStore.savedCity(this@WidgetConfigActivity)
        val previewCity = cfg.city() ?: appCity ?: POPULAR_IRAN_CITIES.first()
        val previewReport = remember(cfg) { WidgetData.load(this@WidgetConfigActivity, previewCity) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(s.wTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            // Live preview (content + style), mirroring what the widget will show.
            val kind = previewReport?.let { wmoInfo(it.current.weatherCode, it.current.isDay).kind }
            val previewBrush = previewBrush(cfg.style, kind)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(previewBrush, RoundedCornerShape(24.dp))
                    .padding(16.dp),
            ) {
                Text(previewCity.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (previewReport != null) {
                    val info = wmoInfo(previewReport.current.weatherCode, previewReport.current.isDay)
                    Text(
                        "${info.emoji} ${Fmt.temp(previewReport.current.temperature, settings)}",
                        color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (settings.isFa) info.labelFa else info.labelEn,
                        color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp,
                    )
                    if (cfg.showFeels) {
                        Text(
                            "${s.feelsLike} ${Fmt.temp(previewReport.current.feelsLike, settings)}",
                            color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp,
                        )
                    }
                    if (cfg.showAqi) {
                        previewReport.aqiUs?.let {
                            Text("🍃 AQI ${Fmt.num(it, settings.lang)}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(s.wNoData, color = Color.White, fontSize = 13.sp)
                }
            }

            Text(s.wCity, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = cfg.followAppCity,
                    onClick = { cfg = cfg.copy(followAppCity = true) },
                )
                Text("${s.wFollowApp}${appCity?.let { " (${it.name})" } ?: ""}")
            }
            for (city in POPULAR_IRAN_CITIES) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !cfg.followAppCity && cfg.cityName == city.name,
                        onClick = {
                            cfg = cfg.copy(
                                followAppCity = false,
                                cityName = city.name,
                                cityLat = city.latitude,
                                cityLon = city.longitude,
                            )
                        },
                    )
                    Text(city.name)
                }
            }

            Text(s.wStyle, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((style, label) in listOf(
                    WidgetStyle.DYNAMIC to s.wStyleDynamic,
                    WidgetStyle.GLASS to s.wStyleGlass,
                    WidgetStyle.MATERIAL to s.wStyleMaterial,
                )) {
                    FilterChip(
                        selected = cfg.style == style,
                        onClick = { cfg = cfg.copy(style = style) },
                        label = { Text(label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((style, label) in listOf(
                    WidgetStyle.TRANSPARENT to s.wStyleTransparent,
                    WidgetStyle.MINIMAL to s.wStyleMinimal,
                )) {
                    FilterChip(
                        selected = cfg.style == style,
                        onClick = { cfg = cfg.copy(style = style) },
                        label = { Text(label) },
                    )
                }
            }

            ToggleRow(s.wShowFeels, cfg.showFeels) { cfg = cfg.copy(showFeels = it) }
            ToggleRow(s.wShowHourly, cfg.showHourly) { cfg = cfg.copy(showHourly = it) }
            ToggleRow(s.wShowAqi, cfg.showAqi) { cfg = cfg.copy(showAqi = it) }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onSave(cfg) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(s.save) }
        }
    }

    @Composable
    private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }

    private fun previewBrush(style: WidgetStyle, kind: com.pishi.iweather.data.SkyKind?): Brush {
        val colors = when (style) {
            WidgetStyle.MATERIAL -> listOf(Color(0xFF1B2A4A), Color(0xFF1B2A4A))
            WidgetStyle.GLASS -> listOf(Color(0xFF3D5A78), Color(0xFF55779C))
            WidgetStyle.TRANSPARENT, WidgetStyle.MINIMAL -> listOf(Color(0xFF888888), Color(0xFFAAAAAA))
            WidgetStyle.DYNAMIC -> when (kind) {
                com.pishi.iweather.data.SkyKind.CLEAR_DAY -> listOf(Color(0xFF2F6FD0), Color(0xFF7FB8EC))
                com.pishi.iweather.data.SkyKind.CLEAR_NIGHT -> listOf(Color(0xFF0B1026), Color(0xFF27355F))
                com.pishi.iweather.data.SkyKind.RAIN -> listOf(Color(0xFF2E4E66), Color(0xFF5E84A6))
                com.pishi.iweather.data.SkyKind.SNOW -> listOf(Color(0xFF6E879C), Color(0xFFB9CBDC))
                com.pishi.iweather.data.SkyKind.STORM -> listOf(Color(0xFF1F2735), Color(0xFF46536A))
                com.pishi.iweather.data.SkyKind.FOG -> listOf(Color(0xFF6E7B86), Color(0xFFACB8C0))
                else -> listOf(Color(0xFF54687F), Color(0xFF93A9BF))
            }
        }
        return Brush.verticalGradient(colors)
    }
}

package com.pishi.iweather.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.pishi.iweather.ui.theme.Luna
import com.pishi.iweather.ui.theme.lunaGlass
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pishi.iweather.util.AlertType
import com.pishi.iweather.util.AppSettings
import com.pishi.iweather.util.AqiStandard
import com.pishi.iweather.util.AppStrings
import com.pishi.iweather.util.Fmt
import com.pishi.iweather.util.Lang
import com.pishi.iweather.util.PrecipUnit
import com.pishi.iweather.util.PressureUnit
import com.pishi.iweather.util.TempUnit
import com.pishi.iweather.util.VisUnit
import com.pishi.iweather.util.WindUnit
import com.pishi.iweather.util.strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: IWeatherUiState,
    onBack: () -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onTestConnection: () -> Unit,
    onClearCache: () -> Unit,
    onClearAllData: () -> Unit,
    onResetSettings: () -> Unit,
    onNotifToggle: (Boolean) -> Unit,
    onBriefToggle: (Boolean) -> Unit,
    lastFetchMillis: Long,
    appVersion: String,
) {
    val st = state.settings
    val s = strings(st.lang)
    val context = LocalContext.current
    var confirmClearData by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Luna.Bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = Color.White)
                }
                Text("⚙️ ${s.settings}", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))

            // ---- General / language ----
            SettingsCard {
                SectionTitle2("🌐 ${s.sGeneral}")
                Text(s.sLanguage, color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = st.lang == Lang.FA,
                        onClick = { onUpdateSettings { it.copy(lang = Lang.FA, langChosen = true) } },
                        label = { Text("فارسی") },
                    )
                    FilterChip(
                        selected = st.lang == Lang.EN,
                        onClick = { onUpdateSettings { it.copy(lang = Lang.EN, langChosen = true) } },
                        label = { Text("English") },
                    )
                }
            }

            // ---- Units ----
            SettingsCard {
                SectionTitle2("📐 ${s.sUnits}")
                UnitRow(s.sTempUnit) {
                    Chip("°C", st.tempUnit == TempUnit.C) { onUpdateSettings { it.copy(tempUnit = TempUnit.C) } }
                    Chip("°F", st.tempUnit == TempUnit.F) { onUpdateSettings { it.copy(tempUnit = TempUnit.F) } }
                }
                UnitRow(s.sWindUnit) {
                    Chip("km/h", st.windUnit == WindUnit.KMH) { onUpdateSettings { it.copy(windUnit = WindUnit.KMH) } }
                    Chip("m/s", st.windUnit == WindUnit.MS) { onUpdateSettings { it.copy(windUnit = WindUnit.MS) } }
                    Chip("mph", st.windUnit == WindUnit.MPH) { onUpdateSettings { it.copy(windUnit = WindUnit.MPH) } }
                    Chip("kn", st.windUnit == WindUnit.KN) { onUpdateSettings { it.copy(windUnit = WindUnit.KN) } }
                }
                UnitRow(s.sPressureUnit) {
                    Chip("hPa", st.pressureUnit == PressureUnit.HPA) { onUpdateSettings { it.copy(pressureUnit = PressureUnit.HPA) } }
                    Chip("mbar", st.pressureUnit == PressureUnit.MBAR) { onUpdateSettings { it.copy(pressureUnit = PressureUnit.MBAR) } }
                    Chip("mmHg", st.pressureUnit == PressureUnit.MMHG) { onUpdateSettings { it.copy(pressureUnit = PressureUnit.MMHG) } }
                    Chip("inHg", st.pressureUnit == PressureUnit.INHG) { onUpdateSettings { it.copy(pressureUnit = PressureUnit.INHG) } }
                }
                UnitRow(s.sPrecipUnit) {
                    Chip("mm", st.precipUnit == PrecipUnit.MM) { onUpdateSettings { it.copy(precipUnit = PrecipUnit.MM) } }
                    Chip("in", st.precipUnit == PrecipUnit.IN) { onUpdateSettings { it.copy(precipUnit = PrecipUnit.IN) } }
                }
                UnitRow(s.sVisUnit) {
                    Chip("km", st.visUnit == VisUnit.KM) { onUpdateSettings { it.copy(visUnit = VisUnit.KM) } }
                    Chip("mi", st.visUnit == VisUnit.MI) { onUpdateSettings { it.copy(visUnit = VisUnit.MI) } }
                }
                UnitRow(s.sTimeFormat) {
                    Chip(s.sTime24, st.time24) { onUpdateSettings { it.copy(time24 = true) } }
                    Chip(s.sTime12, !st.time24) { onUpdateSettings { it.copy(time24 = false) } }
                }
            }

            // ---- Appearance ----
            SettingsCard {
                SectionTitle2("🎨 ${s.sAppearance}")
                SwitchRow(s.sDynamicSky, s.sDynamicSkyDesc, st.dynamicSky) { v ->
                    onUpdateSettings { it.copy(dynamicSky = v) }
                }
                SwitchRow(s.sAnimations, s.sAnimationsDesc, st.animations) { v ->
                    onUpdateSettings { it.copy(animations = v) }
                }
                SwitchRow(s.sCompact, s.sCompactDesc, st.compact) { v ->
                    onUpdateSettings { it.copy(compact = v) }
                }
            }

            // ---- Air & health ----
            SettingsCard {
                SectionTitle2("🍃 ${s.sAirHealth}")
                UnitRow(s.sAqiStandard) {
                    Chip(s.aqiUs, st.aqiStandard == AqiStandard.US) { onUpdateSettings { it.copy(aqiStandard = AqiStandard.US) } }
                    Chip(s.aqiEu, st.aqiStandard == AqiStandard.EU) { onUpdateSettings { it.copy(aqiStandard = AqiStandard.EU) } }
                }
                SwitchRow(s.sHealthAdvice, s.sHealthAdviceDesc, st.healthAdvice) { v ->
                    onUpdateSettings { it.copy(healthAdvice = v) }
                }
            }

            // ---- Notifications ----
            SettingsCard {
                SectionTitle2("🔔 ${s.sNotifications}")
                SwitchRow(s.sNotifEnable, s.sNotifEnableDesc, st.notifEnabled) { v -> onNotifToggle(v) }
                if (st.notifEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Text(s.sNotifTypes, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for ((type, label) in alertTypeLabels(s)) {
                            val selected = type in st.notifTypes
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onUpdateSettings {
                                        val next = if (selected) it.notifTypes - type else it.notifTypes + type
                                        it.copy(notifTypes = next)
                                    }
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(s.sThresholds, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    StepperRow(s.sThTempHigh, st.thTempHigh, 30..45, st) { v ->
                        onUpdateSettings { it.copy(thTempHigh = v) }
                    }
                    StepperRow(s.sThTempLow, st.thTempLow, -30..5, st) { v ->
                        onUpdateSettings { it.copy(thTempLow = v) }
                    }
                    StepperRow(s.sThWind, st.thWind, 40..120, st, step = 5) { v ->
                        onUpdateSettings { it.copy(thWind = v) }
                    }
                    StepperRow(s.sThAqi, st.thAqi, 50..300, st, step = 10) { v ->
                        onUpdateSettings { it.copy(thAqi = v) }
                    }
                    UnitRow(s.sInterval) {
                        for (h in listOf(1, 3, 6, 12)) {
                            Chip("${Fmt.num(h, st.lang)} ${s.sHoursUnit}", st.notifIntervalH == h) {
                                onUpdateSettings { it.copy(notifIntervalH = h) }
                            }
                        }
                    }
                    SwitchRow(s.sQuiet, s.sQuietDesc, st.quietEnabled) { v ->
                        onUpdateSettings { it.copy(quietEnabled = v) }
                    }
                    if (st.quietEnabled) {
                        StepperRow("${s.sFrom}", st.quietStart, 0..23, st) { v ->
                            onUpdateSettings { it.copy(quietStart = v) }
                        }
                        StepperRow("${s.sTo}", st.quietEnd, 0..23, st) { v ->
                            onUpdateSettings { it.copy(quietEnd = v) }
                        }
                    }
                }
            }

            // ---- Morning brief ----
            SettingsCard {
                SectionTitle2("🌅 ${s.sBrief}")
                SwitchRow(s.sBrief, s.sBriefDesc, st.briefEnabled) { v -> onBriefToggle(v) }
                if (st.briefEnabled) {
                    StepperRow(s.sBriefHour, st.briefHour, 0..23, st) { v ->
                        onUpdateSettings { it.copy(briefHour = v) }
                    }
                }
            }

            // ---- Data & source ----
            SettingsCard {
                SectionTitle2("🛰️ ${s.sData}")
                InfoRow(s.sProvider, s.sProviderName)
                Text(s.sAirSource, color = Color.White.copy(alpha = 0.85f), fontSize = 12.5.sp)
                Spacer(Modifier.height(6.dp))
                val lastFetchText = if (lastFetchMillis == 0L) s.sNever
                else Fmt.num(
                    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(lastFetchMillis)),
                    st.lang,
                )
                InfoRow(s.sLastFetch, lastFetchText)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onTestConnection,
                        enabled = state.testState != TestState.TESTING,
                    ) {
                        Text(if (state.testState == TestState.TESTING) s.sTesting else s.sTestConnection)
                    }
                    OutlinedButton(onClick = onClearCache) { Text(s.sClearCache) }
                }
                when (state.testState) {
                    TestState.OK -> Text(s.sTestOk, color = Color(0xFF69F0AE), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    TestState.FAIL -> Text(s.sTestFail, color = Color(0xFFFF8A80), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    else -> Unit
                }
            }

            // ---- Widgets ----
            SettingsCard {
                SectionTitle2("🧩 ${s.sWidgets}")
                Text(s.sWidgetsBody, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp)
            }

            // ---- Privacy ----
            SettingsCard {
                SectionTitle2("🔒 ${s.sPrivacy}")
                Text(s.sPrivacyBody, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { confirmClearData = true }) { Text(s.sClearData) }
            }

            // ---- About ----
            SettingsCard {
                SectionTitle2("ℹ️ ${s.sAbout}")
                InfoRow(s.sVersion, appVersion)
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/PishiKuchulu/iWeather/releases"),
                    )
                    runCatching { context.startActivity(intent) }
                }) { Text(s.sCheckUpdate) }
                Spacer(Modifier.height(8.dp))
                Text(s.sSources, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(s.sSourcesBody, color = Color.White.copy(alpha = 0.92f), fontSize = 12.5.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { confirmReset = true }) { Text(s.sReset) }
            }

            Spacer(Modifier.navigationBarsPadding().height(30.dp))
        }
    }

    if (confirmClearData) {
        AlertDialog(
            onDismissRequest = { confirmClearData = false },
            title = { Text(s.sClearData) },
            text = { Text(s.sPrivacyBody) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearData = false
                    onClearAllData()
                }) { Text(s.sClearData) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearData = false }) { Text(s.close) }
            },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(s.sReset) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onResetSettings()
                }) { Text(s.sReset) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(s.close) }
            },
        )
    }
}

private fun alertTypeLabels(s: AppStrings): List<Pair<AlertType, String>> = listOf(
    AlertType.RAIN to s.typeRain,
    AlertType.SNOW to s.typeSnow,
    AlertType.STORM to s.typeStorm,
    AlertType.HEAT to s.typeHeat,
    AlertType.COLD to s.typeCold,
    AlertType.WIND to s.typeWind,
    AlertType.DUST to s.typeDust,
    AlertType.AQI to s.typeAqi,
    AlertType.UV to s.typeUv,
)

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .lunaGlass(22.dp)
            .padding(16.dp),
    ) { content() }
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun SectionTitle2(text: String) {
    Text(text, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun UnitRow(label: String, chips: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 13.5.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun SwitchRow(title: String, desc: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            if (!desc.isNullOrBlank()) {
                Text(desc, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    st: AppSettings,
    step: Int = 1,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        TextButton(
            onClick = { onChange((value - step).coerceIn(range)) },
            enabled = value > range.first,
        ) { Text("−", fontSize = 18.sp) }
        Text(Fmt.num(value, st.lang), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        TextButton(
            onClick = { onChange((value + step).coerceIn(range)) },
            enabled = value < range.last,
        ) { Text("+", fontSize = 18.sp) }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

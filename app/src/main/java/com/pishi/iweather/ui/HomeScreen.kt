package com.pishi.iweather.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pishi.iweather.data.City
import com.pishi.iweather.data.FavoritesStore
import com.pishi.iweather.data.HourPoint
import com.pishi.iweather.data.POPULAR_IRAN_CITIES
import com.pishi.iweather.data.WeatherReport
import com.pishi.iweather.data.wmoInfo
import com.pishi.iweather.ui.theme.Luna
import com.pishi.iweather.ui.theme.lunaGlass
import com.pishi.iweather.util.AppSettings
import com.pishi.iweather.util.AppStrings
import com.pishi.iweather.util.Dates
import com.pishi.iweather.util.Fmt
import com.pishi.iweather.util.Insights
import com.pishi.iweather.util.Lang
import com.pishi.iweather.util.strings
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// Fixed LazyColumn positions (used by the feature cards to scroll to a section).
private const val ITEM_ALERTS = 5
private const val ITEM_ACTIVITIES = 7

@Composable
fun HomeScreen(
    state: IWeatherUiState,
    onRefresh: () -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectCity: (City) -> Unit,
    onMyLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onChooseLanguage: (Lang) -> Unit,
    onTestConnection: () -> Unit,
    onClearCache: () -> Unit,
    onClearAllData: () -> Unit,
    onResetSettings: () -> Unit,
    onConsumeMessage: () -> Unit,
    onNotifToggle: (Boolean) -> Unit,
    onBriefToggle: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFavoriteCity: (City) -> Unit,
    onAddFavorite: (City) -> Unit,
    onMoveFavorite: (City, Boolean) -> Unit,
    onRemoveFavorite: (City) -> Unit,
    onRefreshFavorites: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
    lastFetchMillis: Long,
    appVersion: String,
) {
    val st = state.settings
    val s = strings(st.lang)
    val direction = if (st.isFa) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        BackHandler(enabled = st.langChosen && state.screen == AppScreen.SETTINGS) { onCloseSettings() }
        Box(Modifier.fillMaxSize().background(Luna.Bg)) {
            when {
                !st.langChosen -> LanguageGate(onChooseLanguage)
                state.screen == AppScreen.SETTINGS -> SettingsScreen(
                    state = state,
                    onBack = onCloseSettings,
                    onUpdateSettings = onUpdateSettings,
                    onTestConnection = onTestConnection,
                    onClearCache = onClearCache,
                    onClearAllData = onClearAllData,
                    onResetSettings = onResetSettings,
                    onNotifToggle = onNotifToggle,
                    onBriefToggle = onBriefToggle,
                    onCheckUpdate = onCheckUpdate,
                    lastFetchMillis = lastFetchMillis,
                    appVersion = appVersion,
                )
                else -> HomeContent(
                    state = state,
                    onRefresh = onRefresh,
                    onSearchToggle = onSearchToggle,
                    onQueryChange = onQueryChange,
                    onSelectCity = onSelectCity,
                    onMyLocation = onMyLocation,
                    onOpenSettings = onOpenSettings,
                    onToggleFavorite = onToggleFavorite,
                    onToggleFavoriteCity = onToggleFavoriteCity,
                    onMoveFavorite = onMoveFavorite,
                    onRemoveFavorite = onRemoveFavorite,
                    onRefreshFavorites = onRefreshFavorites,
                )
            }
            // One-shot messages (cache cleared / data cleared / settings reset / saved cities)
            val snackbar = remember { SnackbarHostState() }
            val messageText = when (state.message) {
                "cache_cleared" -> s.sCacheCleared
                "data_cleared" -> s.sDataCleared
                "reset_done" -> s.sResetDone
                "fav_added" -> s.favAdded
                "fav_removed" -> s.favRemoved
                "fav_limit" -> s.favLimit
                "fav_exists" -> s.favExists
                "upd_uptodate" -> s.updUpToDate
                "upd_error" -> s.updError
                "upd_signer" -> s.updSignerError
                else -> null
            }
            LaunchedEffect(state.message) {
                if (messageText != null) {
                    snackbar.showSnackbar(messageText)
                    onConsumeMessage()
                }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp),
            )
            // In-app updater dialog (appears over home and settings alike).
            when (val upd = state.update) {
                is UpdateUi.Available -> AlertDialog(
                    onDismissRequest = onDismissUpdate,
                    title = { Text(s.updTitle) },
                    text = { Text("${s.updNewVersion}: ${upd.info.version}") },
                    confirmButton = { TextButton(onClick = onDownloadUpdate) { Text(s.updDownload) } },
                    dismissButton = { TextButton(onClick = onDismissUpdate) { Text(s.updLater) } },
                )
                is UpdateUi.Downloading -> AlertDialog(
                    onDismissRequest = { },
                    title = { Text(s.updTitle) },
                    text = {
                        Column {
                            Text("${s.updNewVersion}: ${upd.info.version}")
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(progress = { upd.percent / 100f }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Text("${Fmt.num(upd.percent, st.lang)}${if (st.isFa) "٪" else "%"}", fontSize = 13.sp)
                        }
                    },
                    confirmButton = { },
                )
                is UpdateUi.Ready -> AlertDialog(
                    onDismissRequest = onDismissUpdate,
                    title = { Text(s.updTitle) },
                    text = { Text("${s.updNewVersion}: ${upd.info.version}") },
                    confirmButton = { TextButton(onClick = onInstallUpdate) { Text(s.updInstall) } },
                    dismissButton = { TextButton(onClick = onDismissUpdate) { Text(s.updLater) } },
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun LanguageGate(onChooseLanguage: (Lang) -> Unit) {
    Box(Modifier.fillMaxSize().background(Luna.Bg)) {
        SceneArt(SceneKind.CLEAR, night = true, animated = true, thunder = false, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "iWeather",
                color = Luna.TextSoft, fontSize = 44.sp, fontWeight = FontWeight.ExtraLight,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "زبان برنامه رو انتخاب کن · Choose your language",
                color = Luna.TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            LunaPillButton("فارسی", onClick = { onChooseLanguage(Lang.FA) })
            Spacer(Modifier.height(14.dp))
            LunaPillButton("English", onClick = { onChooseLanguage(Lang.EN) })
            Spacer(Modifier.height(18.dp))
            Text(
                "بعداً از تنظیمات هم می‌تونی عوضش کنی · You can change it later in Settings",
                color = Luna.TextMuted.copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: IWeatherUiState,
    onRefresh: () -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectCity: (City) -> Unit,
    onMyLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFavoriteCity: (City) -> Unit,
    onMoveFavorite: (City, Boolean) -> Unit,
    onRemoveFavorite: (City) -> Unit,
    onRefreshFavorites: () -> Unit,
) {
    val st = state.settings
    val s = strings(st.lang)
    val report = state.report
    var showCities by remember { mutableStateOf(false) }

    // Back closes the topmost overlay instead of leaving the app.
    BackHandler(enabled = showCities) { showCities = false }
    BackHandler(enabled = state.showSearch) { onSearchToggle(false) }

    Box(Modifier.fillMaxSize().background(Luna.Bg)) {
        when {
            state.loading && report == null -> Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = Luna.VioletLight)
                Spacer(Modifier.height(14.dp))
                Text(s.loading, color = Luna.TextSoft, fontSize = 16.sp)
            }

            state.hasError && report == null -> Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📡", fontSize = 44.sp)
                Spacer(Modifier.height(10.dp))
                Text(s.errorTitle, color = Luna.Content, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(s.errorBody, color = Luna.TextSoft, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                LunaPillButton(s.retry, onClick = onRefresh)
            }

            report != null -> ReportContent(
                report = report,
                state = state,
                onOpenSettings = onOpenSettings,
                onMyLocation = onMyLocation,
                onSearchClick = { onSearchToggle(true) },
                onOpenCities = { showCities = true },
                onToggleFavorite = onToggleFavorite,
            )
        }

        if (showCities && report != null) {
            CitiesOverlay(
                state = state,
                currentCityName = report.city.name,
                onSelectCity = { city ->
                    showCities = false
                    onSelectCity(city)
                },
                onToggleFavoriteCity = onToggleFavoriteCity,
                onMoveFavorite = onMoveFavorite,
                onRemoveFavorite = onRemoveFavorite,
                onRefreshFavorites = onRefreshFavorites,
                onClose = { showCities = false },
            )
        }

        if (state.showSearch) {
            SearchOverlay(
                state = state,
                onQueryChange = onQueryChange,
                onSelectCity = onSelectCity,
                onToggleFavoriteCity = onToggleFavoriteCity,
                onClose = { onSearchToggle(false) },
            )
        }
    }
}

@Composable
private fun ReportContent(
    report: WeatherReport,
    state: IWeatherUiState,
    onOpenSettings: () -> Unit,
    onMyLocation: () -> Unit,
    onSearchClick: () -> Unit,
    onOpenCities: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val st = state.settings
    val s = strings(st.lang)
    val cur = report.current
    val info = wmoInfo(cur.weatherCode, cur.isDay)
    val label = if (st.isFa) info.labelFa else info.labelEn
    val scene = remember(report) { sceneFor(report) }
    val cityLocal = remember(report) { cityLocalNow(report) }
    val alerts = remember(report, st) { Insights.alerts(report, st) }
    val clothing = remember(report, st) { Insights.clothing(report, st) }
    val comfort = remember(report) { Insights.comfortScore(report) }
    val isFav = state.favorites.any { FavoritesStore.same(it, report.city) }
    val gap = if (st.compact) 10.dp else 14.dp
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val side = Modifier.padding(horizontal = 20.dp)
    val anim = st.animations

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap),
            contentPadding = PaddingValues(bottom = navBottom + 104.dp),
        ) {
            // 0 — hero: illustrated live scene + greeting + city + mood + "my location"
            item {
                LunaHero(
                    report = report, scene = scene, st = st, s = s, cityLocal = cityLocal,
                    mood = Insights.moodLine(report, st.lang),
                    isFav = isFav, hasAlerts = alerts.isNotEmpty(),
                    stale = state.stale, loading = state.loading,
                    onMyLocation = onMyLocation, onToggleFavorite = onToggleFavorite,
                    onAlertsClick = { scope.launch { listState.animateScrollToItem(ITEM_ALERTS) } },
                )
            }
            // 1 — comfort score card (ring + condition)
            item { AnimatedCard(0, anim) { Box(side) { LunaScoreCard(report, st, s, comfort, label) } } }
            // 2 — four quick stats
            item { AnimatedCard(1, anim) { Box(side) { LunaStatRow(report, st, s) } } }
            // 3 — temperature wave
            item { AnimatedCard(2, anim) { Box(side) { LunaWaveCard(report, st, s) } } }
            // 4 — feature cards
            item {
                AnimatedCard(3, anim) {
                    Box(side) {
                        LunaFeatureRow(
                            s = s,
                            clothing = "${clothing.emoji} ${clothing.text}",
                            insight = alerts.firstOrNull()?.let { "${it.emoji} ${it.text}" } ?: s.featInsightEmpty,
                            onClothing = { scope.launch { listState.animateScrollToItem(ITEM_ACTIVITIES) } },
                            onInsight = { scope.launch { listState.animateScrollToItem(ITEM_ALERTS) } },
                        )
                    }
                }
            }
            // 5 — alerts
            item {
                GlassCard(side) {
                    SectionTitle(s.alertsTitle)
                    if (alerts.isEmpty()) {
                        Text(s.noAlerts, color = Luna.Content, fontSize = 14.sp)
                    } else {
                        for (a in alerts) {
                            Text("${a.emoji} ${a.text}", color = Luna.Content, fontSize = 13.5.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
            // 6 — hourly strip (48 h)
            item {
                GlassCard(side) {
                    SectionTitle(s.hourlyTitle)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(report.hourly) { idx, h ->
                            HourChip(h, idx == 0, st, s)
                        }
                    }
                }
            }
            // 7 — activities
            item {
                GlassCard(side) {
                    SectionTitle(s.activitiesTitle)
                    val acts = remember(report, st.lang) { Insights.activities(report, st.lang) }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (rowActs in acts.chunked(3)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                for (a in rowActs) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        ScoreRing(a.score, a.emoji, anim)
                                        Text(a.title, color = Luna.Content, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                        Text(a.note, color = Luna.Content.copy(alpha = 0.8f), fontSize = 10.5.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 8 — details grid
            item {
                GlassCard(side) {
                    SectionTitle(s.detailsTitle)
                    DetailsGrid(report, st, s)
                }
            }
            // 9 — air quality (only when the CAMS call succeeded)
            val air = report.air
            if (air != null && (air.usAqi != null || air.euAqi != null)) {
                item {
                    GlassCard(side) {
                        SectionTitle(s.airTitle)
                        AirCard(report, st)
                    }
                }
            }
            // 10 — sun & moon
            item {
                GlassCard(side) {
                    SectionTitle(s.sunTitle)
                    SunMoonCard(report, st)
                }
            }
            // 11 — 16-day list
            item {
                GlassCard(side) {
                    SectionTitle(s.dailyTitle)
                    DailyList(report, st)
                }
            }
            // footer
            item {
                val fetchedLocal = cityLocalFromEpoch(report.fetchedAtMillis, report.utcOffsetSec)
                val timeStr = Fmt.clock(fetchedLocal.hour * 60 + fetchedLocal.minute, st)
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text(
                        s.sourceLine.format(timeStr),
                        color = Luna.TextMuted, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                    if (state.stale) {
                        Text(
                            s.staleNote, color = Color(0xFFFFC107), fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Floating pill navigation — every item is wired.
        LunaNav(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            LunaNavItem(Icons.Filled.Home, s.navHome, active = true) { scope.launch { listState.animateScrollToItem(0) } }
            LunaNavItem(Icons.Filled.Search, s.searchTitle, active = false, onClick = onSearchClick)
            LunaNavItem(Icons.Filled.LocationOn, s.navCities, active = false, onClick = onOpenCities)
            LunaNavItem(Icons.Filled.Settings, s.settings, active = false, onClick = onOpenSettings)
        }
    }
}

@Composable
private fun HourChip(h: HourPoint, isNow: Boolean, st: AppSettings, s: AppStrings) {
    val hi = wmoInfo(h.weatherCode, h.isDay)
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Luna.Content.copy(alpha = if (isNow) 0.16f else 0.07f), shape)
            .then(
                if (isNow) Modifier.border(1.dp, Luna.StrokeC, shape) else Modifier,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(Fmt.clock(h.hourLabel, st), color = Luna.Content.copy(alpha = 0.85f), fontSize = 11.5.sp)
        Text(hi.emoji, fontSize = 19.sp)
        Text(Fmt.temp(h.temperature, st), color = Luna.Content, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("${s.feelsLike} ${Fmt.temp(h.feelsLike, st)}", color = Luna.Content.copy(alpha = 0.75f), fontSize = 10.5.sp)
        Text("💧 ${Fmt.percent(h.precipProbability, st.lang)}", color = Color(0xFFB3E5FC), fontSize = 11.sp)
        Text("💨 ${Fmt.wind(h.windSpeed, st)}", color = Luna.Content.copy(alpha = 0.8f), fontSize = 10.5.sp)
    }
}

@Composable
private fun DetailsGrid(report: WeatherReport, st: AppSettings, s: AppStrings) {
    val cur = report.current
    val firstHour = report.hourly.firstOrNull()
    val uvToday = report.daily.firstOrNull()?.uvMax ?: 0.0
    val details = listOf(
        "💧 ${s.dHumidity}" to Fmt.percent(cur.humidity, st.lang),
        "🌡️ ${s.dDewPoint}" to Fmt.temp(cur.dewPoint, st),
        "💨 ${s.dWind}" to "${Fmt.wind(cur.windSpeed, st)} · ${s.windFrom.format(Insights.windDirection(cur.windDirection, st.lang))}",
        "🌪️ ${s.dGusts}" to Fmt.wind(cur.windGusts, st),
        "☁️ ${s.dCloud}" to Fmt.percent(cur.cloudCover, st.lang),
        "🎈 ${s.dPressure}" to "${Fmt.pressure(cur.pressure, st)} · ${Insights.pressureTrend(report, st.lang)}",
        "👀 ${s.dVisibility}" to (firstHour?.let { Fmt.visibility(it.visibilityM, st) } ?: "—"),
        "🌧️ ${s.dPrecipNow}" to Fmt.precip(cur.precipitation, st),
        "☀️ ${s.dUvMax}" to "${Fmt.num(uvToday.roundToInt(), st.lang)} · ${Insights.uvLabel(uvToday, st.lang)}",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in details.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((iconLabel, value) in row) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Luna.Content.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                    ) {
                        Text(iconLabel, color = Luna.Content.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text(value, color = Luna.Content, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().lunaGlass(26.dp).padding(18.dp)) { content() }
    }
}

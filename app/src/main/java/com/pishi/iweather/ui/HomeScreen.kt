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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
    onRemoveFavorite: (City) -> Unit,
    onRefreshFavorites: () -> Unit,
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
                Text(s.errorTitle, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
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
    val side = Modifier.padding(horizontal = 18.dp)
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
                        Text(s.noAlerts, color = Color.White, fontSize = 14.sp)
                    } else {
                        for (a in alerts) {
                            Text("${a.emoji} ${a.text}", color = Color.White, fontSize = 13.5.sp, modifier = Modifier.padding(vertical = 2.dp))
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
                                        Text(a.title, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                        Text(a.note, color = Color.White.copy(alpha = 0.8f), fontSize = 10.5.sp, textAlign = TextAlign.Center)
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

// ============================ Luna sections ============================

@Composable
private fun LunaHero(
    report: WeatherReport,
    scene: SceneKind,
    st: AppSettings,
    s: AppStrings,
    cityLocal: java.time.LocalDateTime,
    mood: String,
    isFav: Boolean,
    hasAlerts: Boolean,
    stale: Boolean,
    loading: Boolean,
    onMyLocation: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAlertsClick: () -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val serif = FontFamily.Serif
    Box(Modifier.fillMaxWidth()) {
        if (st.dynamicSky) {
            SceneArt(
                scene = scene,
                night = !report.current.isDay,
                animated = st.animations,
                thunder = report.current.weatherCode >= 95,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1104f / 660f)
                    .background(Brush.verticalGradient(listOf(Color(0xFF1B1360), Luna.Bg))),
            )
        }
        // Soft scrim on the text side keeps the copy readable over bright day scenes.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        if (rtl) listOf(Color.Transparent, Luna.Bg.copy(alpha = 0.55f))
                        else listOf(Luna.Bg.copy(alpha = 0.55f), Color.Transparent),
                    ),
                ),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "iWeather",
                        color = Luna.TextSoft, fontSize = 30.sp, fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        Dates.full(cityLocal.toLocalDate(), st.lang),
                        color = Luna.TextDim, fontSize = 12.sp,
                    )
                }
                LunaRoundButton(LunaPaths.BELL, s.alertsTitle, onAlertsClick, badge = hasAlerts)
                Spacer(Modifier.width(8.dp))
                LunaRoundButton(LunaPaths.PIN, s.myLocation, onMyLocation)
            }
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth(0.6f)) {
                Text(
                    greetingFor(cityLocal.hour, s),
                    color = Luna.TextSoft, fontFamily = serif, fontSize = 26.sp, lineHeight = 30.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        report.city.name,
                        color = Luna.TextSoft, fontFamily = serif, fontSize = 34.sp, lineHeight = 38.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(36.dp)
                            .clickable(onClick = onToggleFavorite),
                        contentAlignment = Alignment.Center,
                    ) {
                        LIcon(
                            LunaPaths.STAR,
                            tint = if (isFav) Luna.Gold else Luna.Lavender,
                            iconSize = 22.dp, strokeWidth = 1.6f,
                            fill = if (isFav) Luna.Gold else null,
                        )
                    }
                }
                val subtitle = listOf(report.city.admin1, report.city.country)
                    .filter { it.isNotBlank() }
                    .joinToString(if (st.isFa) "، " else ", ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = Luna.TextDim, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier
                        .padding(vertical = 12.dp)
                        .width(28.dp)
                        .height(1.dp)
                        .background(Luna.Violet),
                )
                Text(
                    mood,
                    color = Color(0xFFB4AEDC), fontSize = 14.sp, lineHeight = 20.sp,
                    maxLines = 4, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LunaPillButton(
                    text = s.myLocation.removePrefix("📍").trim(),
                    onClick = onMyLocation,
                    leadingPath = LunaPaths.PIN,
                )
                if (loading) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(color = Luna.VioletLight, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
            if (stale) {
                Spacer(Modifier.height(10.dp))
                Chip(s.staleBanner, Color(0xFFFFC107))
            }
        }
    }
}

@Composable
private fun LunaScoreCard(report: WeatherReport, st: AppSettings, s: AppStrings, comfort: Int, label: String) {
    val cur = report.current
    val today = report.daily.firstOrNull()
    Row(
        modifier = Modifier.fillMaxWidth().lunaGlass(30.dp).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LunaRing(progress = comfort / 100f, animated = st.animations, modifier = Modifier.size(116.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.temp(cur.temperature, st), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
                Text("${s.feelsLike} ${Fmt.temp(cur.feelsLike, st)}", color = Luna.TextSoft, fontSize = 11.sp)
                if (comfort >= 85) {
                    Spacer(Modifier.height(2.dp))
                    LIcon(LunaPaths.CROWN, Luna.Gold, iconSize = 16.dp, strokeWidth = 1.5f, fill = Luna.Gold.copy(alpha = 0.55f))
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Luna.Text, fontSize = 21.sp, fontWeight = FontWeight.Medium, lineHeight = 25.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "${s.comfortTitle}: ${Insights.comfortLabel(comfort, st.lang)} (${Fmt.percent(comfort, st.lang)})",
                color = Luna.TextMuted, fontSize = 14.sp, lineHeight = 20.sp,
            )
            if (today != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Fmt.temp(today.tempMin, st)} / ${Fmt.temp(today.tempMax, st)}",
                    color = Luna.TextDim, fontSize = 13.sp,
                )
            }
        }
        LunaStar(Modifier.align(Alignment.Top))
    }
}

@Composable
private fun LunaStatRow(report: WeatherReport, st: AppSettings, s: AppStrings) {
    val cur = report.current
    val first = report.hourly.firstOrNull()
    val pressureDiff = if (report.hourly.size >= 7) report.hourly[6].pressure - report.hourly[0].pressure else 0.0
    val trendArrow = when {
        pressureDiff >= 1.5 -> "↗"
        pressureDiff <= -1.5 -> "↘"
        else -> "→"
    }
    val dewLabel = if (st.isFa) "شبنم" else "Dew"
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LunaStat(
            LunaPaths.DROPLET, s.dHumidity, Fmt.percent(cur.humidity, st.lang),
            "$dewLabel ${Fmt.temp(cur.dewPoint, st)}", Luna.Teal, Modifier.weight(1f),
        )
        LunaStat(
            LunaPaths.WIND, s.dWind, Fmt.wind(cur.windSpeed, st),
            Insights.windDirection(cur.windDirection, st.lang), Luna.Lilac, Modifier.weight(1f),
        )
        LunaStat(
            LunaPaths.GAUGE, s.dPressure, Fmt.pressure(cur.pressure, st),
            trendArrow, if (pressureDiff <= -1.5) Luna.Pink else Luna.Teal, Modifier.weight(1f), highlight = true,
        )
        LunaStat(
            LunaPaths.EYE, s.dVisibility, first?.let { Fmt.visibility(it.visibilityM, st) } ?: "—",
            "☁ ${Fmt.percent(cur.cloudCover, st.lang)}", Luna.Lilac, Modifier.weight(1f),
        )
    }
}

@Composable
private fun LunaStat(
    path: String,
    label: String,
    value: String,
    sub: String,
    subColor: Color,
    modifier: Modifier,
    highlight: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxHeight().lunaGlass(22.dp, cyan = highlight).padding(horizontal = 10.dp, vertical = 14.dp),
    ) {
        LIcon(path, Luna.Lavender, iconSize = 26.dp, strokeWidth = 1.5f)
        Spacer(Modifier.height(10.dp))
        Text(label, color = Luna.TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp)
        Spacer(Modifier.height(4.dp))
        Text(sub, color = subColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val WAVE_GUIDES = listOf(0.125f, 0.375f, 0.625f, 0.875f)

@Composable
private fun LunaWaveCard(report: WeatherReport, st: AppSettings, s: AppStrings) {
    var long by remember { mutableStateOf(false) }
    val hours = remember(report, long) { report.hourly.take(if (long) 48 else 24) }
    if (hours.size < 2) return
    val temps = hours.map { it.temperature }
    val lo = temps.min()
    val hi = temps.max()
    val maxRain = hours.maxOf { it.precipProbability }

    Column(Modifier.fillMaxWidth().lunaGlass(30.dp).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LunaIconBox(boxSize = 44.dp) {
                LIcon(LunaPaths.SPARKLES, Color(0xFFE3DCFF), iconSize = 24.dp, strokeWidth = 1.5f, fill = Color(0x40CBBFFF))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.waveTitle, color = Luna.Text, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                Text(if (long) s.waveNext48 else s.waveNext24, color = Luna.TextDim, fontSize = 12.sp)
            }
            Text(
                "${Fmt.temp(lo, st)} – ${Fmt.temp(hi, st)}",
                color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium,
            )
            Box(
                Modifier.size(38.dp).clickable { long = !long },
                contentAlignment = Alignment.Center,
            ) {
                LIcon(LunaPaths.EXPAND, Color(0xFFC4BCF5), iconSize = 20.dp, strokeWidth = 1.8f)
            }
        }
        Spacer(Modifier.height(10.dp))
        WaveCanvas(hours, Modifier.fillMaxWidth().height(150.dp))
        Row(Modifier.fillMaxWidth()) {
            for (f in WAVE_GUIDES) {
                val idx = (f * (hours.size - 1)).roundToInt().coerceIn(0, hours.size - 1)
                Text(
                    Fmt.clock(hours[idx].hourLabel, st),
                    color = Color(0xFFC1BBEA), fontSize = 11.5.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f), maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WaveLegend(Luna.Pink, s.chartMax, Fmt.temp(hi, st), Modifier.weight(1f))
            WaveLegend(Luna.Cyan, s.chartMin, Fmt.temp(lo, st), Modifier.weight(1f))
            WaveLegend(Luna.Lilac, s.legendFeels, Fmt.temp(hours.first().feelsLike, st), Modifier.weight(1f))
            WaveLegend(Luna.Blue, s.legendRain, Fmt.percent(maxRain, st.lang), Modifier.weight(1f))
        }
    }
}

@Composable
private fun WaveLegend(color: Color, label: String, value: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 4.dp).size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, color = Color(0xFFDDD8F7), fontSize = 12.sp, maxLines = 1)
            Text(value, color = Color(0xFFC2BCE8), fontSize = 12.sp, maxLines = 1)
        }
    }
}

/** Smooth temperature curve with gradient stroke, feels-like dashes, rain bars and time guides. */
@Composable
private fun WaveCanvas(hours: List<HourPoint>, modifier: Modifier) {
    val temps = hours.map { it.temperature }
    val feels = hours.map { it.feelsLike }
    val lo = minOf(temps.min(), feels.min())
    val hi = maxOf(temps.max(), feels.max())
    val range = (hi - lo).coerceAtLeast(1.0)
    val n = hours.size
    Canvas(modifier) {
        val mirror = layoutDirection == LayoutDirection.Rtl
        val w = size.width
        val h = size.height
        val topPad = 16.dp.toPx()
        val botPad = 8.dp.toPx()
        val ch = h - topPad - botPad
        fun px(i: Int): Float {
            val x = w * i / (n - 1)
            return if (mirror) w - x else x
        }
        fun py(v: Double): Float = topPad + ((hi - v) / range * ch).toFloat()
        fun curve(values: List<Double>, closeToBottom: Boolean): Path {
            val p = Path()
            p.moveTo(px(0), py(values[0]))
            for (i in 1 until n) {
                val mx = (px(i - 1) + px(i)) / 2f
                p.cubicTo(mx, py(values[i - 1]), mx, py(values[i]), px(i), py(values[i]))
            }
            if (closeToBottom) {
                p.lineTo(px(n - 1), h)
                p.lineTo(px(0), h)
                p.close()
            }
            return p
        }

        // time guides
        for (f in WAVE_GUIDES) {
            drawLine(
                Color.White.copy(alpha = 0.18f),
                Offset(w * f, 0f), Offset(w * f, h),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx())),
            )
        }
        // rain-chance bars
        val barW = (w / n) * 0.5f
        hours.forEachIndexed { i, hp ->
            val bh = hp.precipProbability / 100f * (ch * 0.4f)
            if (bh > 1f) {
                drawLine(
                    Luna.Blue.copy(alpha = 0.35f),
                    Offset(px(i), h), Offset(px(i), h - bh),
                    strokeWidth = barW, cap = StrokeCap.Butt,
                )
            }
        }
        // filled area
        drawPath(
            curve(temps, closeToBottom = true),
            Brush.verticalGradient(listOf(Color(0xFF8E6BFF).copy(alpha = 0.45f), Color.Transparent)),
        )
        val line = curve(temps, closeToBottom = false)
        val strokeBrush = Brush.horizontalGradient(listOf(Color(0xFFBF93FF), Color(0xFF6F7FFF), Color(0xFF3FDCFF)))
        drawPath(line, strokeBrush, alpha = 0.28f, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
        drawPath(line, strokeBrush, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        // feels-like (dashed)
        drawPath(
            curve(feels, closeToBottom = false),
            Luna.Pink.copy(alpha = 0.75f),
            style = Stroke(
                width = 1.6.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 7.dp.toPx())),
            ),
        )
        // extremes + "now" marker
        val maxIdx = temps.indexOf(temps.max())
        val minIdx = temps.indexOf(temps.min())
        drawCircle(Luna.Pink, 4.dp.toPx(), Offset(px(maxIdx), py(temps[maxIdx])))
        drawCircle(Luna.Cyan, 4.dp.toPx(), Offset(px(minIdx), py(temps[minIdx])))
        drawCircle(Color.White, 5.dp.toPx(), Offset(px(0), py(temps[0])))
        drawCircle(Luna.Violet, 5.dp.toPx(), Offset(px(0), py(temps[0])), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun LunaFeatureRow(
    s: AppStrings,
    clothing: String,
    insight: String,
    onClothing: () -> Unit,
    onInsight: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LunaFeature(LunaPaths.SHIRT, s.featClothTitle, clothing, onClothing, Modifier.weight(1f))
        LunaFeature(LunaPaths.INSIGHTS, s.featInsightTitle, insight, onInsight, Modifier.weight(1f))
    }
}

@Composable
private fun LunaFeature(path: String, title: String, desc: String, onClick: () -> Unit, modifier: Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(
        modifier = modifier
            .fillMaxHeight()
            .lunaGlass(26.dp)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LunaIconBox(boxSize = 42.dp) { LIcon(path, Color(0xFFD6CDFF), iconSize = 22.dp, strokeWidth = 1.5f) }
            Spacer(Modifier.width(10.dp))
            Text(title, color = Luna.Text, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 2)
            LIcon(
                LunaPaths.CHEVRON, Color(0xFFC9C1F5),
                modifier = Modifier.graphicsLayer { scaleX = if (rtl) -1f else 1f },
                iconSize = 18.dp, strokeWidth = 2f,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(desc, color = Luna.TextMuted, fontSize = 12.5.sp, lineHeight = 18.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HourChip(h: HourPoint, isNow: Boolean, st: AppSettings, s: AppStrings) {
    val hi = wmoInfo(h.weatherCode, h.isDay)
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White.copy(alpha = if (isNow) 0.16f else 0.07f), shape)
            .then(
                if (isNow) Modifier.border(1.dp, Luna.StrokeC, shape) else Modifier,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(Fmt.clock(h.hourLabel, st), color = Color.White.copy(alpha = 0.85f), fontSize = 11.5.sp)
        Text(hi.emoji, fontSize = 19.sp)
        Text(Fmt.temp(h.temperature, st), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("${s.feelsLike} ${Fmt.temp(h.feelsLike, st)}", color = Color.White.copy(alpha = 0.75f), fontSize = 10.5.sp)
        Text("💧 ${Fmt.percent(h.precipProbability, st.lang)}", color = Color(0xFFB3E5FC), fontSize = 11.sp)
        Text("💨 ${Fmt.wind(h.windSpeed, st)}", color = Color.White.copy(alpha = 0.8f), fontSize = 10.5.sp)
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
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                    ) {
                        Text(iconLabel, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text(value, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
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
        Column(Modifier.fillMaxWidth().lunaGlass(26.dp).padding(16.dp)) { content() }
    }
}

// ============================ overlays ============================

@Composable
private fun CitiesOverlay(
    state: IWeatherUiState,
    currentCityName: String,
    onSelectCity: (City) -> Unit,
    onRemoveFavorite: (City) -> Unit,
    onRefreshFavorites: () -> Unit,
    onClose: () -> Unit,
) {
    val st = state.settings
    val s = strings(st.lang)
    // Refresh saved cities' temperatures each time the sheet opens.
    LaunchedEffect(Unit) { onRefreshFavorites() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6090A22))
            .clickable(onClick = onClose),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(18.dp)
                .lunaGlass(28.dp)
                .clickable(enabled = false) {}
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.navCities, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                    Text("✕", color = Color.White, fontSize = 16.sp)
                }
            }
            Text("${s.currentCity}: $currentCityName", color = Luna.Lavender, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                item { OverlayHeader(s.favoritesTitle) }
                if (state.favorites.isEmpty()) {
                    item {
                        Text(s.favoritesEmpty, color = Luna.TextMuted, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 6.dp))
                    }
                } else {
                    items(state.favorites) { city ->
                        FavoriteRow(city, state.favInfo[favKey(city)], st, { onSelectCity(city) }, { onRemoveFavorite(city) })
                    }
                }
                item {
                    Spacer(Modifier.height(10.dp))
                    OverlayHeader(s.popularHeader)
                }
                items(POPULAR_IRAN_CITIES) { city ->
                    CityRow(city, st) { onSelectCity(city) }
                }
            }
        }
    }
}

@Composable
private fun OverlayHeader(text: String) {
    Text(text, color = Luna.TextDim, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun FavoriteRow(
    city: City,
    info: FavInfo?,
    st: AppSettings,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(info?.let { wmoInfo(it.weatherCode, it.isDay).emoji } ?: "⭐", fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(city.name, color = Color.White, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = listOf(city.admin1, city.country).filter { it.isNotBlank() }.joinToString(if (st.isFa) "، " else ", ")
            if (subtitle.isNotBlank()) Text(subtitle, color = Luna.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (info != null) {
            Text(Fmt.temp(info.temperature, st), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
        Box(Modifier.size(40.dp).clickable(onClick = onRemove), contentAlignment = Alignment.Center) {
            LIcon(LunaPaths.STAR, Luna.Gold, iconSize = 20.dp, strokeWidth = 1.6f, fill = Luna.Gold)
        }
    }
}

@Composable
private fun SearchOverlay(
    state: IWeatherUiState,
    onQueryChange: (String) -> Unit,
    onSelectCity: (City) -> Unit,
    onClose: () -> Unit,
) {
    val st = state.settings
    val s = strings(st.lang)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6090A22))
            .clickable(onClick = onClose),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .imePadding()
                .padding(18.dp)
                .lunaGlass(28.dp)
                .clickable(enabled = false) {}
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.searchTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                    Text("✕", color = Color.White, fontSize = 16.sp)
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text(s.searchHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            if (state.searching) {
                Text(s.searching, color = Luna.TextSoft, fontSize = 13.sp)
            }
            LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                if (state.query.isBlank()) {
                    item { OverlayHeader(s.popularCities) }
                    items(POPULAR_IRAN_CITIES) { city ->
                        CityRow(city, st) { onSelectCity(city) }
                    }
                } else {
                    items(state.searchResults) { city ->
                        CityRow(city, st) { onSelectCity(city) }
                    }
                    if (!state.searching && state.searchResults.isEmpty() && state.query.trim().length >= 2) {
                        item {
                            Text(s.cityNotFound, color = Color(0xFFFFCC80), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityRow(city: City, st: AppSettings, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LIcon(LunaPaths.PIN, Luna.Lavender, iconSize = 18.dp, strokeWidth = 1.7f)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(city.name, color = Color.White, fontSize = 14.5.sp)
            val subtitle = listOf(city.admin1, city.country).filter { it.isNotBlank() }
                .joinToString(if (st.isFa) "، " else ", ")
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = Luna.TextDim, fontSize = 12.sp)
            }
        }
    }
}

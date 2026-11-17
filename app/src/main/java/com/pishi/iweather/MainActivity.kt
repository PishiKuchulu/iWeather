package com.pishi.iweather

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.location.LocationManagerCompat
import com.pishi.iweather.ui.HomeScreen
import com.pishi.iweather.ui.IWeatherViewModel
import com.pishi.iweather.ui.theme.IWeatherTheme
import com.pishi.iweather.util.Lang
import com.pishi.iweather.util.SettingsStore
import com.pishi.iweather.widget.WidgetUpdater

class MainActivity : ComponentActivity() {

    private val viewModel: IWeatherViewModel by viewModels()

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchLocation()
        else toast(
            fa = "بدون دسترسی به موقعیت، شهر رو دستی انتخاب کن.",
            en = "Without location access, pick your city manually.",
        )
    }

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Don't leave the switches "on" when nothing can be delivered.
            viewModel.updateSettings { it.copy(notifEnabled = false, briefEnabled = false) }
            toast(
                fa = "بدون اجازهٔ اعلان، هشدارها و پیام صبحگاهی نمی‌تونن ارسال بشن.",
                en = "Without notification permission, alerts and the morning brief can't be delivered.",
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars (Android 15 enforces this anyway); the UI pads for the insets itself.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        WidgetUpdater.schedulePeriodic(this)
        // longVersionCode needs API 28 but minSdk is 26 -> use the compat helper.
        val appVersion = runCatching {
            val info = packageManager.getPackageInfo(packageName, 0)
            "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
        }.getOrDefault("1.5.0")
        setContent {
            val state by viewModel.state.collectAsState()
            // Day/night for AUTO: the current location's own day flag; before the first report
            // lands, fall back to this device's clock (06:00–18:59 counts as day).
            val isDay = state.report?.current?.isDay ?: (java.time.LocalTime.now().hour in 6..18)
            val dark = com.pishi.iweather.ui.theme.themeIsDark(state.settings.themeMode, isDay)
            androidx.compose.runtime.SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (dark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                )
            }
            IWeatherTheme(mode = state.settings.themeMode, isDay = isDay) {
                HomeScreen(
                    state = state,
                    onRefresh = { viewModel.refresh() },
                    onSearchToggle = { viewModel.setShowSearch(it) },
                    onQueryChange = { viewModel.onQueryChange(it) },
                    onSelectCity = { viewModel.selectCity(it) },
                    onMyLocation = { requestLocation() },
                    onOpenSettings = { viewModel.openSettings() },
                    onCloseSettings = { viewModel.closeSettings() },
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onChooseLanguage = { viewModel.chooseLanguage(it) },
                    onTestConnection = { viewModel.testConnection() },
                    onClearCache = { viewModel.clearCache() },
                    onClearAllData = { viewModel.clearAllData() },
                    onResetSettings = { viewModel.resetSettings() },
                    onConsumeMessage = { viewModel.consumeMessage() },
                    onNotifToggle = { enabled -> onNotifToggle(enabled) },
                    onBriefToggle = { enabled -> onBriefToggle(enabled) },
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onToggleFavoriteCity = { viewModel.toggleFavorite(it) },
                    onAddFavorite = { viewModel.addFavorite(it) },
                    onMoveFavorite = { city, up -> viewModel.moveFavorite(city, up) },
                    onRemoveFavorite = { viewModel.removeFavorite(it) },
                    onRefreshFavorites = { viewModel.refreshFavorites() },
                    lastFetchMillis = state.report?.fetchedAtMillis ?: viewModel.lastFetchMillis(),
                    appVersion = appVersion,
                )
            }
        }
    }

    private fun ensureNotificationPermission(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun onNotifToggle(enabled: Boolean) {
        ensureNotificationPermission(enabled)
        viewModel.updateSettings { it.copy(notifEnabled = enabled) }
    }

    private fun onBriefToggle(enabled: Boolean) {
        ensureNotificationPermission(enabled)
        viewModel.updateSettings { it.copy(briefEnabled = enabled) }
    }

    private fun toast(fa: String, en: String) {
        val lang = SettingsStore(this).load().lang
        Toast.makeText(this, if (lang == Lang.FA) fa else en, Toast.LENGTH_LONG).show()
    }

    private fun requestLocation() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) fetchLocation() else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /**
     * Uses a recent last-known fix when there is one (<= 30 min old); otherwise asks the system for a
     * fresh single fix. A stale last-known location is only the final fallback — the old code used any
     * cached fix, even one from another city.
     */
    @Suppress("MissingPermission")
    private fun fetchLocation() {
        val lm = getSystemService(LocationManager::class.java)
        if (lm == null) {
            notFound()
            return
        }
        var stale: Location? = null
        for (p in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull() ?: continue
            if (System.currentTimeMillis() - loc.time <= 30 * 60_000L) {
                viewModel.onMyLocation(loc.latitude, loc.longitude)
                return
            }
            if (stale == null || loc.time > stale.time) stale = loc
        }

        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val provider = when {
            runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) -> LocationManager.NETWORK_PROVIDER
            fine && runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) -> LocationManager.GPS_PROVIDER
            else -> null
        }
        if (provider == null) {
            if (stale != null) viewModel.onMyLocation(stale.latitude, stale.longitude) else notFound()
            return
        }
        toast(fa = "در حال پیدا کردن موقعیت…", en = "Finding your location…")
        val fallback = stale
        // Typed null: LocationManagerCompat has two getCurrentLocation overloads that differ
        // only in the CancellationSignal type (android.os vs androidx.core.os); a bare null
        // is ambiguous and the lambda's type cannot be inferred.
        val cancelSignal: android.os.CancellationSignal? = null
        try {
            LocationManagerCompat.getCurrentLocation(lm, provider, cancelSignal, ContextCompat.getMainExecutor(this)) { loc ->
                when {
                    loc != null -> viewModel.onMyLocation(loc.latitude, loc.longitude)
                    fallback != null -> viewModel.onMyLocation(fallback.latitude, fallback.longitude)
                    else -> notFound()
                }
            }
        } catch (e: SecurityException) {
            notFound()
        }
    }

    private fun notFound() = toast(
        fa = "موقعیت فعلی پیدا نشد؛ GPS رو روشن کن و دوباره تلاش کن.",
        en = "Couldn't find your location; turn on GPS and try again.",
    )
}

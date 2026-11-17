package com.pishi.iweather.ui

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pishi.iweather.data.City
import com.pishi.iweather.data.FavoritesStore
import com.pishi.iweather.data.POPULAR_IRAN_CITIES
import com.pishi.iweather.data.WeatherApi
import com.pishi.iweather.data.WeatherReport
import com.pishi.iweather.data.WeatherStore
import com.pishi.iweather.notify.AlertScheduler
import com.pishi.iweather.util.AppSettings
import com.pishi.iweather.util.Lang
import com.pishi.iweather.util.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class AppScreen { HOME, SETTINGS }
enum class TestState { IDLE, TESTING, OK, FAIL }

/** Cached snapshot of a saved city, shown on the "Cities" sheet (temperature + condition). */
data class FavInfo(val temperature: Double, val weatherCode: Int, val isDay: Boolean)

/** Single-sourced with the cache/notification identity (WeatherStore.cityKey). */
fun favKey(city: City): String = WeatherStore.cityKey(city)

data class IWeatherUiState(
    val loading: Boolean = true,
    val report: WeatherReport? = null,
    val hasError: Boolean = false,
    /** True when the shown report comes from the on-device cache (offline/stale). */
    val stale: Boolean = false,
    val showSearch: Boolean = false,
    val query: String = "",
    val searchResults: List<City> = emptyList(),
    val searching: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val screen: AppScreen = AppScreen.HOME,
    val testState: TestState = TestState.IDLE,
    val favorites: List<City> = emptyList(),
    val favInfo: Map<String, FavInfo> = emptyMap(),
    /** One-shot transient message key for snackbars: cache_cleared | data_cleared | reset_done | fav_added | fav_removed | fav_limit | fav_exists | upd_uptodate | upd_error | upd_signer. */
    val message: String? = null,
    /** In-app updater state (GitHub Releases chain, Anvex-style). */
    val update: UpdateUi = UpdateUi.Idle,
)

/** Updater UI state machine: silent auto-check on launch, explicit flow once an update exists. */
sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data class Available(val info: com.pishi.iweather.data.UpdateManager.UpdateInfo) : UpdateUi
    data class Downloading(val info: com.pishi.iweather.data.UpdateManager.UpdateInfo, val percent: Int) : UpdateUi
    data class Ready(val info: com.pishi.iweather.data.UpdateManager.UpdateInfo, val apkPath: String) : UpdateUi
}

class IWeatherViewModel(app: Application) : AndroidViewModel(app) {
    private val api = WeatherApi()
    private val settingsStore = SettingsStore(app)
    private val _state = MutableStateFlow(
        IWeatherUiState(settings = settingsStore.load(), favorites = FavoritesStore.load(app)),
    )
    val state: StateFlow<IWeatherUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        AlertScheduler.reschedule(app, _state.value.settings)
        val saved = WeatherStore.savedCity(app) ?: POPULAR_IRAN_CITIES.first() // Tehran default
        val cached = WeatherStore.cachedReport(app, saved, api)
        if (cached != null) {
            _state.update { it.copy(loading = false, report = cached, stale = true) }
        }
        load(saved, showLoading = cached == null)
        publishFavInfo()
        checkForUpdate(manual = false)
    }

    // ---------- in-app updater (GitHub Releases) ----------

    private fun installedVersion(): String = try {
        val ctx = getApplication<Application>()
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0.0.0"
    } catch (e: Exception) {
        "0.0.0"
    }

    /**
     * Checks the latest GitHub release. Automatic checks (launch) stay silent: they only
     * surface the Available state. Manual checks also report "up to date" / failures.
     */
    fun checkForUpdate(manual: Boolean) {
        val cur = _state.value.update
        if (cur is UpdateUi.Checking || cur is UpdateUi.Downloading) return
        _state.update { it.copy(update = UpdateUi.Checking) }
        viewModelScope.launch {
            runCatching { com.pishi.iweather.data.UpdateManager.check(installedVersion()) }
                .onSuccess { info ->
                    _state.update { st ->
                        when {
                            info != null -> st.copy(update = UpdateUi.Available(info))
                            manual -> st.copy(update = UpdateUi.UpToDate, message = "upd_uptodate")
                            else -> st.copy(update = UpdateUi.Idle)
                        }
                    }
                }
                .onFailure {
                    _state.update { st ->
                        if (manual) st.copy(update = UpdateUi.Idle, message = "upd_error")
                        else st.copy(update = UpdateUi.Idle)
                    }
                }
        }
    }

    /** Downloads the offered update, verifies SHA-256 (inside the manager) + signer, then offers install. */
    fun downloadUpdate() {
        val avail = _state.value.update as? UpdateUi.Available ?: return
        _state.update { it.copy(update = UpdateUi.Downloading(avail.info, 0)) }
        viewModelScope.launch {
            runCatching {
                val file = com.pishi.iweather.data.UpdateManager.download(getApplication(), avail.info) { pct ->
                    _state.update { st ->
                        val d = st.update as? UpdateUi.Downloading
                        if (d != null) st.copy(update = d.copy(percent = pct)) else st
                    }
                }
                if (!com.pishi.iweather.data.UpdateManager.verifySigner(getApplication(), file)) {
                    file.delete()
                    throw com.pishi.iweather.data.UpdateManager.SignerMismatchException()
                }
                file
            }.onSuccess { file ->
                _state.update { it.copy(update = UpdateUi.Ready(avail.info, file.absolutePath)) }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        update = UpdateUi.Idle,
                        message = if (e is com.pishi.iweather.data.UpdateManager.SignerMismatchException) "upd_signer" else "upd_error",
                    )
                }
            }
        }
    }

    fun installUpdate() {
        val ready = _state.value.update as? UpdateUi.Ready ?: return
        val ctx = getApplication<Application>()
        runCatching {
            ctx.startActivity(
                com.pishi.iweather.data.UpdateManager.installIntent(ctx, java.io.File(ready.apkPath)),
            )
        }
    }

    fun dismissUpdate() {
        val cur = _state.value.update
        if (cur is UpdateUi.Available || cur is UpdateUi.Ready || cur is UpdateUi.UpToDate) {
            _state.update { it.copy(update = UpdateUi.Idle) }
        }
    }

    // ---------- settings ----------

    fun chooseLanguage(lang: Lang) {
        updateSettings { it.copy(lang = lang, langChosen = true) }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value.settings)
        settingsStore.save(next)
        _state.update { it.copy(settings = next) }
        AlertScheduler.reschedule(getApplication(), next)
    }

    fun resetSettings() {
        val lang = _state.value.settings.lang
        val fresh = AppSettings(lang = lang, langChosen = true)
        settingsStore.save(fresh)
        _state.update { it.copy(settings = fresh, message = "reset_done") }
        AlertScheduler.reschedule(getApplication(), fresh)
    }

    fun openSettings() = _state.update { it.copy(screen = AppScreen.SETTINGS, testState = TestState.IDLE) }
    fun closeSettings() = _state.update { it.copy(screen = AppScreen.HOME) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun testConnection() {
        _state.update { it.copy(testState = TestState.TESTING) }
        viewModelScope.launch {
            val city = _state.value.report?.city ?: WeatherStore.savedCity(getApplication()) ?: POPULAR_IRAN_CITIES.first()
            val ok = runCatching { api.fetchRaw(city) }.isSuccess
            _state.update { it.copy(testState = if (ok) TestState.OK else TestState.FAIL) }
        }
    }

    fun clearCache() {
        WeatherStore.clearCache(getApplication())
        _state.update { it.copy(message = "cache_cleared", favInfo = emptyMap()) }
    }

    fun clearAllData() {
        val app = getApplication<Application>()
        WeatherStore.clearCache(app)
        app.getSharedPreferences("iweather", android.content.Context.MODE_PRIVATE).edit().clear().apply()
        val fresh = AppSettings() // back to defaults + language gate
        settingsStore.save(fresh)
        _state.update {
            it.copy(
                settings = fresh, screen = AppScreen.HOME, report = null, favorites = emptyList(),
                favInfo = emptyMap(), stale = false, message = "data_cleared",
            )
        }
        AlertScheduler.reschedule(app, fresh)
        load(POPULAR_IRAN_CITIES.first(), showLoading = true)
    }

    fun lastFetchMillis(): Long = WeatherStore.lastFetchMillis(getApplication())

    // ---------- weather ----------

    fun load(city: City, showLoading: Boolean = false) {
        _state.update { it.copy(loading = showLoading || it.report == null, hasError = false) }
        // A newer request always wins: cancel the previous one so a slow response for city A
        // can never overwrite the report the user already switched to city B for.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val raw = api.fetchRaw(city)
                val fetchedAt = System.currentTimeMillis()
                val report = api.parseReport(city, raw, fetchedAt)
                WeatherStore.saveCity(getApplication(), city)
                WeatherStore.saveCache(getApplication(), city, raw, fetchedAt)
                _state.update {
                    it.copy(loading = false, report = report, stale = false, hasError = false, showSearch = false)
                }
                com.pishi.iweather.widget.WidgetUpdater.requestUpdate(getApplication())
                publishFavInfo()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    if (it.report != null) {
                        it.copy(loading = false, stale = true)
                    } else {
                        it.copy(loading = false, hasError = true)
                    }
                }
            }
        }
    }

    fun refresh() {
        val city = _state.value.report?.city ?: WeatherStore.savedCity(getApplication()) ?: POPULAR_IRAN_CITIES.first()
        load(city)
    }

    fun onMyLocation(lat: Double, lon: Double) {
        val lang = _state.value.settings.lang
        val fallback = City(name = if (lang == Lang.FA) "موقعیت من" else "My location", latitude = lat, longitude = lon)
        _state.update { it.copy(loading = it.report == null, hasError = false) }
        viewModelScope.launch {
            // Best-effort reverse geocoding so the city shows a real name; falls back to "My location".
            val named = withContext(Dispatchers.IO) { reverseGeocode(lat, lon, lang) } ?: fallback
            load(named)
        }
    }

    private fun reverseGeocode(lat: Double, lon: Double, lang: Lang): City? = runCatching {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(getApplication<Application>(), Locale(if (lang == Lang.FA) "fa" else "en"))
        @Suppress("DEPRECATION")
        val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull() ?: return null
        val name = address.locality ?: address.subAdminArea ?: address.adminArea ?: return null
        City(
            name = name,
            country = address.countryName ?: "",
            admin1 = address.adminArea?.takeIf { it != name } ?: "",
            latitude = lat,
            longitude = lon,
        )
    }.getOrNull()

    fun setShowSearch(show: Boolean) {
        _state.update { it.copy(showSearch = show, query = if (show) it.query else "", searchResults = emptyList()) }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()
        if (q.trim().length < 2) {
            _state.update { it.copy(searchResults = emptyList(), searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _state.update { it.copy(searching = true) }
            val results = runCatching { api.searchCities(q) }.getOrDefault(emptyList())
            _state.update { it.copy(searchResults = results, searching = false) }
        }
    }

    fun selectCity(city: City) {
        val cached = WeatherStore.cachedReport(getApplication(), city, api)
        _state.update {
            if (cached != null) it.copy(report = cached, stale = true, showSearch = false, query = "", searchResults = emptyList())
            else it.copy(showSearch = false, query = "", searchResults = emptyList())
        }
        load(city, showLoading = cached == null)
    }

    // ---------- saved cities ----------

    fun toggleFavorite() {
        val city = _state.value.report?.city ?: return
        toggleFavorite(city)
    }

    /** Adds [city] to the saved locations, or removes it when already saved. Used by the hero star and the search/city rows. */
    fun toggleFavorite(city: City) {
        val favs = _state.value.favorites
        val existing = favs.firstOrNull { FavoritesStore.same(it, city) }
        val (next, msg) = when {
            existing != null -> favs - existing to "fav_removed"
            favs.size >= FavoritesStore.MAX -> favs to "fav_limit"
            else -> favs + city to "fav_added"
        }
        FavoritesStore.save(getApplication(), next)
        _state.update { it.copy(favorites = next, message = msg) }
        publishFavInfo()
    }

    /** Adds [city] without switching to it (search results, popular list). No-op with a hint when already saved. */
    fun addFavorite(city: City) {
        val favs = _state.value.favorites
        val msg = when {
            favs.any { FavoritesStore.same(it, city) } -> "fav_exists"
            favs.size >= FavoritesStore.MAX -> "fav_limit"
            else -> {
                val next = favs + city
                FavoritesStore.save(getApplication(), next)
                _state.update { it.copy(favorites = next) }
                publishFavInfo()
                "fav_added"
            }
        }
        _state.update { it.copy(message = msg) }
    }

    /** Moves a saved location one step up/down in the list; the order is persisted and drives the Cities sheet. */
    fun moveFavorite(city: City, up: Boolean) {
        val favs = _state.value.favorites
        val idx = favs.indexOfFirst { FavoritesStore.same(it, city) }
        if (idx < 0) return
        val target = if (up) idx - 1 else idx + 1
        if (target !in favs.indices) return
        val next = favs.toMutableList().also { it[idx] = favs[target]; it[target] = favs[idx] }
        FavoritesStore.save(getApplication(), next)
        _state.update { it.copy(favorites = next) }
    }

    fun removeFavorite(city: City) {
        val next = _state.value.favorites.filterNot { FavoritesStore.same(it, city) }
        FavoritesStore.save(getApplication(), next)
        _state.update { it.copy(favorites = next) }
    }

    /** Reads each saved city's cached report into a light snapshot map (off the main thread). */
    private fun publishFavInfo() {
        val favs = _state.value.favorites
        if (favs.isEmpty()) return
        viewModelScope.launch {
            val info = withContext(Dispatchers.Default) {
                val out = HashMap<String, FavInfo>()
                for (c in favs) {
                    WeatherStore.cachedCityReport(getApplication(), c, api)?.let {
                        out[favKey(c)] = FavInfo(it.current.temperature, it.current.weatherCode, it.current.isDay)
                    }
                }
                out
            }
            _state.update { it.copy(favInfo = info) }
        }
    }

    /** Fetches fresh data for every saved city (called when the Cities sheet opens). */
    fun refreshFavorites() {
        val favs = _state.value.favorites
        if (favs.isEmpty()) return
        viewModelScope.launch {
            for (c in favs) {
                val raw = runCatching { api.fetchRaw(c) }.getOrNull() ?: continue
                WeatherStore.saveCityCache(getApplication(), c, raw, System.currentTimeMillis())
                publishFavInfo()
            }
        }
    }
}

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

// ============================ overlays ============================

@Composable
internal fun CitiesOverlay(
    state: IWeatherUiState,
    currentCityName: String,
    onSelectCity: (City) -> Unit,
    onToggleFavoriteCity: (City) -> Unit,
    onMoveFavorite: (City, Boolean) -> Unit,
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
                Text(s.navCities, color = Luna.Content, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(48.dp).semantics { contentDescription = s.sClose }.clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = Luna.Content, fontSize = 16.sp)
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
                    itemsIndexed(state.favorites) { idx, city ->
                        FavoriteRow(
                            city = city,
                            info = state.favInfo[favKey(city)],
                            st = st,
                            isCurrent = state.report?.city?.let { FavoritesStore.same(it, city) } == true,
                            canUp = idx > 0,
                            canDown = idx < state.favorites.lastIndex,
                            onClick = { onSelectCity(city) },
                            onRemove = { onRemoveFavorite(city) },
                            onMoveUp = { onMoveFavorite(city, true) },
                            onMoveDown = { onMoveFavorite(city, false) },
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(10.dp))
                    OverlayHeader(s.popularHeader)
                }
                items(POPULAR_IRAN_CITIES) { city ->
                    CityRow(
                        city = city, st = st,
                        isFav = state.favorites.any { FavoritesStore.same(it, city) },
                        onToggleFav = { onToggleFavoriteCity(city) },
                    ) { onSelectCity(city) }
                }
            }
        }
    }
}

@Composable
internal fun OverlayHeader(text: String) {
    Text(text, color = Luna.TextDim, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
internal fun FavoriteRow(
    city: City,
    info: FavInfo?,
    st: AppSettings,
    isCurrent: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(info?.let { wmoInfo(it.weatherCode, it.isDay).emoji } ?: "⭐", fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(city.name, color = Luna.Content, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(6.dp).background(Luna.Teal, androidx.compose.foundation.shape.CircleShape))
                }
            }
            val subtitle = listOf(city.admin1, city.country).filter { it.isNotBlank() }.joinToString(if (st.isFa) "، " else ", ")
            if (subtitle.isNotBlank()) Text(subtitle, color = Luna.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (info != null) {
            Text(Fmt.temp(info.temperature, st), color = Luna.Content, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
        // Reorder within the saved list (order persists and drives this sheet).
        Column {
            Box(
                Modifier.size(26.dp).clickable(enabled = canUp, onClick = onMoveUp),
                contentAlignment = Alignment.Center,
            ) {
                LIcon(
                    LunaPaths.CHEVRON, if (canUp) Luna.Lavender else Luna.TextMuted.copy(alpha = 0.35f),
                    modifier = Modifier.graphicsLayer { rotationZ = -90f }, iconSize = 15.dp, strokeWidth = 2f,
                )
            }
            Box(
                Modifier.size(26.dp).clickable(enabled = canDown, onClick = onMoveDown),
                contentAlignment = Alignment.Center,
            ) {
                LIcon(
                    LunaPaths.CHEVRON, if (canDown) Luna.Lavender else Luna.TextMuted.copy(alpha = 0.35f),
                    modifier = Modifier.graphicsLayer { rotationZ = 90f }, iconSize = 15.dp, strokeWidth = 2f,
                )
            }
        }
        Box(Modifier.size(40.dp).clickable(onClick = onRemove), contentAlignment = Alignment.Center) {
            LIcon(LunaPaths.STAR, Luna.Gold, iconSize = 20.dp, strokeWidth = 1.6f, fill = Luna.Gold)
        }
    }
}

@Composable
internal fun SearchOverlay(
    state: IWeatherUiState,
    onQueryChange: (String) -> Unit,
    onSelectCity: (City) -> Unit,
    onToggleFavoriteCity: (City) -> Unit,
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
                Text(s.searchTitle, color = Luna.Content, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(48.dp).semantics { contentDescription = s.sClose }.clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = Luna.Content, fontSize = 16.sp)
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
                        CityRow(
                            city = city, st = st,
                            isFav = state.favorites.any { FavoritesStore.same(it, city) },
                            onToggleFav = { onToggleFavoriteCity(city) },
                        ) { onSelectCity(city) }
                    }
                } else {
                    items(state.searchResults) { city ->
                        CityRow(
                            city = city, st = st,
                            isFav = state.favorites.any { FavoritesStore.same(it, city) },
                            onToggleFav = { onToggleFavoriteCity(city) },
                        ) { onSelectCity(city) }
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
internal fun CityRow(
    city: City,
    st: AppSettings,
    isFav: Boolean = false,
    onToggleFav: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LIcon(LunaPaths.PIN, Luna.Lavender, iconSize = 18.dp, strokeWidth = 1.7f)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(city.name, color = Luna.Content, fontSize = 14.5.sp)
            val subtitle = listOf(city.admin1, city.country).filter { it.isNotBlank() }
                .joinToString(if (st.isFa) "، " else ", ")
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = Luna.TextDim, fontSize = 12.sp)
            }
        }
        // Save/remove this city in the locations list without switching to it.
        if (onToggleFav != null) {
            Box(Modifier.size(40.dp).clickable(onClick = onToggleFav), contentAlignment = Alignment.Center) {
                LIcon(
                    LunaPaths.STAR,
                    tint = if (isFav) Luna.Gold else Luna.TextMuted,
                    iconSize = 19.dp, strokeWidth = 1.6f,
                    fill = if (isFav) Luna.Gold else null,
                )
            }
        }
    }
}

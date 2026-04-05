package com.refreshme

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.refreshme.data.Stylist
import com.refreshme.ui.components.StylistShimmerItem

@Composable
fun StylistListRoute(
    styleIds: Array<String>? = null,
    viewModel: StylistListViewModel = viewModel(),
    onStylistClick: (Stylist) -> Unit
) {
    val stylists by viewModel.stylists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentRating by viewModel.ratingFilter.collectAsState()
    val currentPrice by viewModel.priceFilter.collectAsState()
    val isMobileOnly by viewModel.atHomeService.collectAsState()

    LaunchedEffect(styleIds) {
        if (!styleIds.isNullOrEmpty()) {
            viewModel.filterBySpecialties(styleIds.toList())
        }
    }

    StylistListScreen(
        stylists = stylists,
        isLoading = isLoading,
        error = error,
        searchQuery = searchQuery,
        currentRating = currentRating,
        currentPrice = currentPrice,
        isMobileOnly = isMobileOnly,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onStylistClick = onStylistClick,
        onRefresh = { viewModel.loadAllStylists() },
        onApplyFilters = { rating, price, isMobile ->
            viewModel.setRatingFilter(rating)
            viewModel.setPriceFilter(price)
            viewModel.setAtHomeService(isMobile)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistListScreen(
    stylists: List<Stylist>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    currentRating: Float,
    currentPrice: Float,
    isMobileOnly: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onStylistClick: (Stylist) -> Unit,
    onRefresh: () -> Unit,
    onApplyFilters: (Float, Float, Boolean) -> Unit
) {
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var isMapView by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find a Stylist") },
                actions = {
                    IconButton(onClick = { isMapView = !isMapView }) {
                        Icon(
                            imageVector = if (isMapView) Icons.AutoMirrored.Filled.List else Icons.Default.Map,
                            contentDescription = if (isMapView) "List View" else "Map View"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search Bar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search by name or specialty") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { showFilterBottomSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (currentRating > 0 || currentPrice < 1000f || isMobileOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Crossfade(targetState = isMapView, label = "view_toggle") { isMap ->
                if (isMap) {
                    StylistMapView(
                        stylists = stylists,
                        onStylistClick = onStylistClick
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = onRefresh
                    ) {
                        StylistListView(
                            stylists = stylists,
                            isLoading = isLoading,
                            error = error,
                            onRetry = onRefresh,
                            onStylistClick = onStylistClick
                        )
                    }
                }
            }
        }
    }

    if (showFilterBottomSheet) {
        FilterBottomSheet(
            initialRating = currentRating,
            initialPrice = currentPrice,
            initialIsMobile = isMobileOnly,
            onDismiss = { showFilterBottomSheet = false },
            onApplyFilters = { rating, price, isMobile ->
                onApplyFilters(rating, price, isMobile)
                showFilterBottomSheet = false
            }
        )
    }
}

@Composable
fun StylistListView(
    stylists: List<Stylist>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onStylistClick: (Stylist) -> Unit
) {
    if (isLoading && stylists.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(10) {
                StylistShimmerItem()
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
        }
    } else if (error != null && stylists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    } else {
        if (stylists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No stylists found matching your criteria.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(stylists) { stylist ->
                    StylistListItem(
                        stylist = stylist,
                        onClick = { onStylistClick(stylist) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun StylistMapView(
    stylists: List<Stylist>,
    onStylistClick: (Stylist) -> Unit
) {
    val defaultLocation = LatLng(34.0522, -118.2437) // LA
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = false)
    ) {
        stylists.forEach { stylist ->
            stylist.location?.let { loc ->
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = stylist.name,
                    snippet = stylist.specialty,
                    onInfoWindowClick = { onStylistClick(stylist) }
                )
            }
        }
    }
}

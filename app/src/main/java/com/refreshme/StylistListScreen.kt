package com.refreshme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.refreshme.data.Stylist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistListRoute(
    viewModel: StylistListViewModel = viewModel(),
    onStylistClick: (Stylist) -> Unit
) {
    val stylists by viewModel.stylists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    StylistListScreen(
        stylists = stylists,
        isLoading = isLoading,
        error = error,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onStylistClick = onStylistClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistListScreen(
    stylists: List<Stylist>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onStylistClick: (Stylist) -> Unit
) {
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var ratingFilter by remember { mutableStateOf(0f) }
    var priceFilter by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Stylists") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search by name or location") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showFilterBottomSheet = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading && stylists.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (error != null) {
                    Text(
                        text = "Error: $error",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val filteredStylists = stylists.filter {
                        (it.rating >= ratingFilter) &&
                                (it.services?.any { service -> service.price <= priceFilter } ?: true)
                    }.sortedByDescending { it.isFeatured }

                    if (filteredStylists.isEmpty()) {
                        Text(
                            text = "No stylists found.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn {
                            items(filteredStylists) { stylist ->
                                StylistListItem(stylist = stylist, onClick = { onStylistClick(stylist) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterBottomSheet) {
        FilterBottomSheet(
            onDismiss = { showFilterBottomSheet = false },
            onApplyFilters = { rating, price ->
                ratingFilter = rating
                priceFilter = price
                showFilterBottomSheet = false
            }
        )
    }
}
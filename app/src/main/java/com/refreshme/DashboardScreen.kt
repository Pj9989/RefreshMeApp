package com.refreshme

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.refreshme.data.Booking
import com.refreshme.data.BookingStatus
import com.refreshme.data.Stylist
import com.refreshme.data.StylistCategories
import com.refreshme.ui.components.rememberFirebaseImageModel
import java.util.Locale

private val DashboardBackground = Color(0xFF0D0D17)
private val DashboardSurface = Color(0xFF171721)
private val DashboardSurfaceSoft = Color(0xFF20202C)
private val DashboardStroke = Color(0xFF2B2A38)
private val DashboardPurple = Color(0xFFA13DFF)
private val DashboardGold = Color(0xFFFFC857)
private val DashboardGreen = Color(0xFF39D070)
private val DashboardMuted = Color(0xFF9692A6)

@Composable
fun DashboardScreen(
    viewModel: CustomerDashboardViewModel = viewModel(),
    onFindStylist: () -> Unit,
    onMyBookings: () -> Unit,
    onStylistClick: (Stylist) -> Unit,
    onVirtualTryOn: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf("all") }

    val nearbyStylists = uiState.nearbyStylists
    val liveStylists = remember(nearbyStylists, selectedCategory) {
        nearbyStylists.filterByCategory(selectedCategory)
    }
    val topPicks = remember(liveStylists, uiState.savedStylists) {
        (uiState.savedStylists + liveStylists).distinctBy { it.id.ifBlank { it.name } }
    }
    val density = LocalDensity.current
    val dashboardDensity = remember(density) {
        Density(density = density.density, fontScale = density.fontScale.coerceAtMost(1.08f))
    }

    CompositionLocalProvider(LocalDensity provides dashboardDensity) {
        Scaffold(containerColor = DashboardBackground) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(DashboardBackground),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    CategoryTabs(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }

                item {
                    MapPreviewCard(onClick = onFindStylist)
                }

                item {
                    AiStyleFinderCard(onClick = onVirtualTryOn)
                }

                uiState.upcomingBooking?.let { booking ->
                    item {
                        ActiveAppointmentCard(
                            booking = booking,
                            onClick = onMyBookings
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = "Live Stylists Near You",
                        trailing = "${liveStylists.count { it.isOnline == true }.coerceAtLeast(liveStylists.size)} online"
                    )
                    Spacer(Modifier.height(12.dp))
                    when {
                        uiState.isLoading -> LoadingStylistsRow()
                        liveStylists.isEmpty() -> EmptyStylistsCard(onClick = onFindStylist)
                        else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(liveStylists.take(8), key = { it.id.ifBlank { it.name } }) { stylist ->
                                LiveStylistCard(stylist = stylist, onClick = { onStylistClick(stylist) })
                            }
                        }
                    }
                }

                if (topPicks.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Top Picks For You", action = "See all", onActionClick = onFindStylist)
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            topPicks.take(4).forEach { stylist ->
                                TopPickCard(stylist = stylist, onClick = { onStylistClick(stylist) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingStylistsRow() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DashboardSurface,
        border = BorderStroke(1.dp, DashboardStroke)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DashboardPurple, strokeWidth = 2.dp)
            Text("Finding stylists near you...", color = DashboardMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyStylistsCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = DashboardSurface,
        border = BorderStroke(1.dp, DashboardStroke)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("No live stylists yet", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text("Browse the map or check back shortly.", color = DashboardMuted, fontSize = 13.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DashboardPurple)
        }
    }
}

@Composable
private fun CategoryTabs(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val tabs = listOf(
        CategoryTab("all", "All", Icons.Default.AutoAwesome),
        CategoryTab(StylistCategories.HAIR, "Hair", Icons.Default.ContentCut),
        CategoryTab(StylistCategories.MAKEUP, "Makeup", Icons.Default.Brush),
        CategoryTab(StylistCategories.NAILS, "Nails", Icons.Default.KeyboardArrowDown)
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tabs) { tab ->
            val selected = selectedCategory == tab.id
            Surface(
                modifier = Modifier.clickable { onCategorySelected(tab.id) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) DashboardPurple.copy(alpha = 0.22f) else DashboardSurface,
                border = BorderStroke(1.dp, if (selected) DashboardPurple else DashboardStroke)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        tint = if (selected) DashboardPurple else DashboardMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        tab.label,
                        color = if (selected) Color.White else DashboardMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MapPreviewCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.15f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF202226))
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val road = Color(0xFF3A3B40)
            val minor = Color(0xFF2C2D32)
            drawLine(road, Offset(size.width * 0.04f, size.height * 0.72f), Offset(size.width, size.height * 0.42f), 4.dp.toPx(), StrokeCap.Round)
            drawLine(road, Offset(size.width * 0.47f, 0f), Offset(size.width * 0.39f, size.height), 3.dp.toPx(), StrokeCap.Round)
            drawLine(minor, Offset(size.width * 0.18f, 0f), Offset(size.width * 0.28f, size.height * 0.42f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(minor, Offset(0f, size.height * 0.32f), Offset(size.width * 0.34f, size.height * 0.55f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(minor, Offset(size.width * 0.66f, 0f), Offset(size.width * 0.56f, size.height * 0.62f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(minor, Offset(size.width * 0.74f, size.height * 0.16f), Offset(size.width, size.height * 0.22f), 2.dp.toPx(), StrokeCap.Round)
        }

        Text(
            "Walnut\nCreek Park",
            color = Color.White.copy(alpha = 0.22f),
            fontSize = 12.sp,
            lineHeight = 13.sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 28.dp)
        )
        Text(
            "NORTH CAROLINA",
            color = Color.White.copy(alpha = 0.32f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
        )
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color(0xFFFF304D),
            modifier = Modifier.align(Alignment.Center).size(42.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 34.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFF3B8CFF))
        )
        Text(
            "Google",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
    }
}

@Composable
private fun AiStyleFinderCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = DashboardSurface,
        border = BorderStroke(1.dp, Color(0xFFC7A887))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFC7A887), modifier = Modifier.size(14.dp))
                    Text("AI STYLE FINDER", color = Color(0xFFC7A887), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                Text("Keep it Bold & Trendy?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(
                    "View your recommendations or try something new.",
                    color = DashboardMuted,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4A3827)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFFC7A887), modifier = Modifier.size(27.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailing: String? = null,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (trailing != null) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(DashboardGreen))
            }
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        when {
            trailing != null -> OnlineCountPill(trailing)
            action != null -> Text(
                action,
                color = DashboardPurple,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = onActionClick != null) { onActionClick?.invoke() }
            )
        }
    }
}

@Composable
private fun OnlineCountPill(text: String) {
    Surface(color = Color(0xFF14351F), shape = RoundedCornerShape(12.dp)) {
        Text(
            text,
            color = DashboardGreen,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun LiveStylistCard(stylist: Stylist, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(136.dp)
            .height(192.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardSurface)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(92.dp).background(DashboardSurfaceSoft)) {
            StylistImage(stylist = stylist, modifier = Modifier.fillMaxSize(), corner = 0)
            OnlineBadge(modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
        }
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stylist.name.ifBlank { "RefreshMe Pro" },
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = DashboardGold, modifier = Modifier.size(12.dp))
                    Text(stylist.ratingText(), color = DashboardMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("From $${stylist.startingPrice()}", color = DashboardGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun TopPickCard(stylist: Stylist, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = DashboardSurface,
        border = BorderStroke(1.dp, DashboardStroke.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(76.dp).clip(RoundedCornerShape(8.dp)).background(DashboardSurfaceSoft)) {
                StylistImage(stylist = stylist, modifier = Modifier.fillMaxSize(), corner = 8)
                OnlineBadge(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DashboardGreen))
                    Text(
                        stylist.name.ifBlank { "RefreshMe Pro" },
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    stylist.specialty?.takeIf { it.isNotBlank() } ?: "Haircut & Style",
                    color = DashboardMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ServiceTag(text = serviceTag(stylist))
                    if (stylist.offersMobileService) ServiceTag(text = "At-Home")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RatingStars(rating = stylist.rating)
                    Text("${stylist.ratingText()} · From $${stylist.startingPrice()}", color = DashboardGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DashboardPurple, modifier = Modifier.size(16.dp))
                Spacer(Modifier.height(34.dp))
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = DashboardMuted, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun StylistImage(stylist: Stylist, modifier: Modifier, corner: Int) {
    val fallbackPainter = rememberVectorPainter(Icons.Default.Person)
    AsyncImage(
        model = rememberFirebaseImageModel(stylist.displayImageUrl),
        contentDescription = null,
        placeholder = fallbackPainter,
        error = fallbackPainter,
        modifier = modifier.clip(RoundedCornerShape(corner.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun OnlineBadge(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = DashboardGreen, shape = RoundedCornerShape(10.dp)) {
        Text(
            "ONLINE",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 8.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ServiceTag(text: String) {
    Surface(color = DashboardPurple.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp)) {
        Text(
            text,
            color = DashboardPurple,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun RatingStars(rating: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(5) { index ->
            Icon(
                imageVector = if (rating >= index + 1) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = DashboardGold,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun ActiveAppointmentCard(booking: Booking, onClick: () -> Unit) {
    val isMobile = booking.isMobile
    val bgColors = if (isMobile) {
        listOf(Color(0xFF1E88E5), Color(0xFF1565C0))
    } else {
        listOf(DashboardPurple, Color(0xFF5B2B98))
    }

    val timestamp = booking.scheduledStart?.seconds?.times(1000)
        ?: booking.requestedAt?.seconds?.times(1000)
        ?: System.currentTimeMillis()
    val dateStr = DateUtils.getRelativeTimeSpanString(timestamp).toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.horizontalGradient(bgColors))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape).padding(8.dp)) {
                    Icon(if (isMobile) Icons.Default.DirectionsCar else Icons.Default.Event, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Next Appointment", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(dateStr, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val fallbackPainter = rememberVectorPainter(Icons.Default.Person)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = rememberFirebaseImageModel(booking.stylistPhotoUrl),
                        contentDescription = null,
                        placeholder = fallbackPainter,
                        error = fallbackPainter,
                        modifier = Modifier.size(42.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(booking.stylistName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(booking.serviceName, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp)
                    }
                }

                val status = when (booking.bookingStatus) {
                    BookingStatus.ON_THE_WAY -> "En Route"
                    BookingStatus.ACCEPTED -> "Confirmed"
                    else -> null
                }
                status?.let {
                    Surface(color = Color.White.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp)) {
                        Text(it, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

private data class CategoryTab(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private fun List<Stylist>.filterByCategory(category: String): List<Stylist> {
    if (category == "all") return this
    return filter { stylist ->
        stylist.categories.orEmpty().any { it.equals(category, ignoreCase = true) }
    }
}

private fun Stylist.ratingText(): String {
    return if (rating > 0.0) String.format(Locale.US, "%.1f", rating) else "New"
}

private fun Stylist.startingPrice(): Int {
    val servicePrice = services.orEmpty()
        .mapNotNull { service -> service.price.takeIf { it > 0.0 } }
        .minOrNull()
    return (servicePrice ?: effectiveAtHomeServiceFee.takeIf { it > 0.0 } ?: 25.0).toInt()
}

private fun serviceTag(stylist: Stylist): String {
    val categories = stylist.categories.orEmpty()
    return when {
        categories.any { it.equals(StylistCategories.MAKEUP, ignoreCase = true) } -> "Makeup"
        categories.any { it.equals(StylistCategories.NAILS, ignoreCase = true) } -> "Nails"
        stylist.specialty?.contains("beard", ignoreCase = true) == true -> "Beard Sculpting"
        else -> "Haircut & Style"
    }
}

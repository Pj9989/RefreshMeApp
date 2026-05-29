package com.refreshme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.refreshme.data.ServiceType
import com.refreshme.data.Stylist
import com.refreshme.data.StylistCategories
import com.refreshme.ui.components.rememberFirebaseImageModel
import java.util.Locale

@Composable
fun StylistListItem(
    stylist: Stylist,
    userLocation: com.google.android.gms.maps.model.LatLng? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(rememberFirebaseImageModel(stylist.displayImageUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Stylist profile picture",
                    placeholder = painterResource(R.drawable.ic_profile),
                    error = painterResource(R.drawable.ic_profile),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stylist.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stylist.specialty ?: "Specialty not available",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Category chips (Hair / Makeup / Nails). Shown only for
                    // multi-discipline or non-hair pros so customers can tell
                    // at a glance what kind of work they do. Single-hair pros
                    // keep the existing barber-style presentation.
                    val cats = stylist.categories.orEmpty()
                    val showCategoryChips = cats.size > 1 ||
                        (cats.size == 1 && cats.first() != StylistCategories.HAIR)
                    if (showCategoryChips) {
                        Spacer(modifier = Modifier.size(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            cats.take(2).forEach { cat ->
                                CategoryChip(label = StylistCategories.label(cat))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", stylist.rating),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val stylistLoc = stylist.location
                        // Skip null locations AND (0,0) defaults that come from legacy docs
                        // created with `GeoPoint(0, 0)` placeholders on the Flutter side.
                        val stylistLocValid = stylistLoc != null &&
                            !(stylistLoc.latitude == 0.0 && stylistLoc.longitude == 0.0)
                        if (userLocation != null && stylistLocValid) {
                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(
                                userLocation.latitude, userLocation.longitude,
                                stylistLoc!!.latitude, stylistLoc.longitude,
                                results
                            )
                            val distanceMiles = (results[0] / 1609.34f).toDouble()
                            // Hide implausibly-large distances (>500 mi). These almost
                            // always indicate bad data — e.g. a stylist whose GeoPoint
                            // wasn't geocoded from their saved salon address.
                            if (distanceMiles <= 500.0) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Distance",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f mi", distanceMiles),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        } else if (stylist.offersAtHomeService == true) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Mobile Service",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mobile",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (stylist.matchScore > 0) {
                        Spacer(modifier = Modifier.size(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Match",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${stylist.matchScore}% Match",
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            val serviceIcon = when (stylist.serviceType) {
                ServiceType.AT_HOME -> R.drawable.ic_house
                ServiceType.IN_SALON -> R.drawable.ic_store
                ServiceType.ALL_HOURS -> R.drawable.ic_24_7
                ServiceType.AFTER_HOURS -> R.drawable.ic_24_7
            }

            Icon(
                painter = painterResource(id = serviceIcon),
                contentDescription = "Service Type",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
            )
        }
    }
}

/**
 * Compact pill chip used on stylist cards to indicate a professional
 * category (Hair / Makeup / Nails). Mirrors the Flutter `_Chip` widget
 * used by the shared stylist card.
 */
@Composable
private fun CategoryChip(label: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

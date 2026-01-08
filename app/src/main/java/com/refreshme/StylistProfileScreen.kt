package com.refreshme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.refreshme.data.Review
import com.refreshme.data.Service
import com.refreshme.data.Stylist

@Composable
fun StylistProfileScreen(stylist: Stylist, onBookAppointment: () -> Unit) {
    Scaffold(
        bottomBar = {
            Button(
                onClick = onBookAppointment,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Book Appointment")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = rememberImagePainter(stylist.profileImageUrl),
                        contentDescription = "Stylist Profile Image",
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stylist.name, style = MaterialTheme.typography.headlineMedium)
                    stylist.specialty?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }
                    stylist.bio?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                stylist.galleryImageUrls?.let {
                    SectionTitle("Gallery")
                    LazyRow(modifier = Modifier.padding(top = 8.dp)) {
                        items(it) { imageUrl ->
                            GalleryImage(imageUrl)
                        }
                    }
                }
            }

            item {
                stylist.services?.let {
                    SectionTitle("Services")
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        it.forEach { service ->
                            ServiceItem(service)
                        }
                    }
                }
            }

            item {
                stylist.reviews?.let {
                    SectionTitle("Reviews")
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        it.forEach { review ->
                            ReviewItem(review)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp)
    )
}

@Composable
fun GalleryImage(imageUrl: String) {
    Image(
        painter = rememberImagePainter(imageUrl),
        contentDescription = "Gallery Image",
        modifier = Modifier
            .size(120.dp)
            .padding(4.dp),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ServiceItem(service: Service) {
    ListItem(
        headlineContent = { Text(service.name) },
        supportingContent = { Text("${"$"}${service.price} - ${service.duration} min") }
    )
}

@Composable
fun ReviewItem(review: Review) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = review.userId, style = MaterialTheme.typography.bodyMedium) // T O D O: get username from id
        // You'll need a way to display stars here. For simplicity, I'm just showing the rating.
        Text(text = "Rating: ${review.rating}/5", style = MaterialTheme.typography.bodySmall)
        Text(text = review.text, style = MaterialTheme.typography.bodyMedium)
    }
}
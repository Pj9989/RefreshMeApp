package com.refreshme.stylist

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ManagePortfolioActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val stylistUid: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in.")

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var portfolioImages by remember { mutableStateOf<List<String>>(emptyList()) }
                var portfolioVideos by remember { mutableStateOf<List<String>>(emptyList()) }
                var isLoading by remember { mutableStateOf(true) }
                var isUploading by remember { mutableStateOf(false) }
                var itemToDelete by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Pair(url, isVideo)

                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current

                fun loadPortfolio() {
                    isLoading = true
                    firestore.collection("stylists").document(stylistUid)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            @Suppress("UNCHECKED_CAST")
                            portfolioImages = snapshot.get("portfolioImages") as? List<String> ?: emptyList()
                            @Suppress("UNCHECKED_CAST")
                            portfolioVideos = snapshot.get("portfolioVideos") as? List<String> ?: emptyList()
                            isLoading = false
                        }
                        .addOnFailureListener {
                            isLoading = false
                            Toast.makeText(context, "Failed to load portfolio.", Toast.LENGTH_SHORT).show()
                        }
                }

                LaunchedEffect(Unit) {
                    loadPortfolio()
                }

                // Modern Image Picker
                val mediaPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        isUploading = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val isVideo = context.contentResolver.getType(uri)?.contains("video") == true
                                val extension = if (isVideo) "mp4" else "jpg"
                                val folder = if (isVideo) "portfolio_videos" else "portfolio_images"
                                val field = if (isVideo) "portfolioVideos" else "portfolioImages"
                                
                                val filename = "${System.currentTimeMillis()}.$extension"
                                val storageRef = storage.reference.child("$folder/$stylistUid/$filename")
                                
                                storageRef.putFile(uri).await()
                                val downloadUrl = storageRef.downloadUrl.await().toString()

                                firestore.collection("stylists").document(stylistUid)
                                    .update(field, FieldValue.arrayUnion(downloadUrl))
                                    .await()

                                launch(Dispatchers.Main) {
                                    isUploading = false
                                    Toast.makeText(context, "Media uploaded successfully!", Toast.LENGTH_SHORT).show()
                                    loadPortfolio()
                                }
                            } catch (e: Exception) {
                                launch(Dispatchers.Main) {
                                    isUploading = false
                                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Manage Portfolio") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { 
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                ) 
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Media")
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
                        if (isLoading && portfolioImages.isEmpty() && portfolioVideos.isEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else if (portfolioImages.isEmpty() && portfolioVideos.isEmpty()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No portfolio media yet.\nTap + to add photos and videos!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Videos first
                                items(portfolioVideos) { videoUrl ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { itemToDelete = Pair(videoUrl, true) }
                                    ) {
                                        // Simple placeholder for video
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                            Text("REEL", color = Color.White, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
                                        }
                                        
                                        // Delete Icon Overlay
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                // Images
                                items(portfolioImages) { imageUrl ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { itemToDelete = Pair(imageUrl, false) }
                                    ) {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = "Portfolio Image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        
                                        // Delete Icon Overlay
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Uploading Overlay
                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(shape = RoundedCornerShape(16.dp)) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Uploading media...", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Delete Confirmation Dialog
                if (itemToDelete != null) {
                    val (urlToRemove, isVideo) = itemToDelete!!
                    AlertDialog(
                        onDismissRequest = { itemToDelete = null },
                        title = { Text(if (isVideo) "Delete Video" else "Delete Photo") },
                        text = { Text("Are you sure you want to delete this ${if (isVideo) "video" else "photo"} from your portfolio?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    itemToDelete = null
                                    isLoading = true
                                    val field = if (isVideo) "portfolioVideos" else "portfolioImages"
                                    
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            firestore.collection("stylists").document(stylistUid)
                                                .update(field, FieldValue.arrayRemove(urlToRemove))
                                                .await()
                                                
                                            try {
                                                storage.getReferenceFromUrl(urlToRemove).delete().await()
                                            } catch (e: Exception) {
                                                // Ignore if file is already deleted in storage
                                            }
                                            
                                            launch(Dispatchers.Main) {
                                                Toast.makeText(context, "Media deleted successfully.", Toast.LENGTH_SHORT).show()
                                                loadPortfolio()
                                            }
                                        } catch (e: Exception) {
                                            launch(Dispatchers.Main) {
                                                Toast.makeText(context, "Failed to delete media.", Toast.LENGTH_SHORT).show()
                                                loadPortfolio()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { itemToDelete = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

package com.refreshme.stylist

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.refreshme.data.BeforeAfter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class ManageBeforeAfterActivity : ComponentActivity() {

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
                var beforeAfterList by remember { mutableStateOf<List<BeforeAfter>>(emptyList()) }
                var isLoading by remember { mutableStateOf(true) }
                var showAddDialog by remember { mutableStateOf(false) }

                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current

                fun loadData() {
                    isLoading = true
                    firestore.collection("stylists").document(stylistUid)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val list = snapshot.get("beforeAfterImages") as? List<Map<String, Any>> ?: emptyList()
                            val parsedList = list.mapNotNull { map ->
                                try {
                                    BeforeAfter(
                                        id = map["id"] as? String ?: UUID.randomUUID().toString(),
                                        beforeImageUrl = map["beforeImageUrl"] as? String ?: "",
                                        afterImageUrl = map["afterImageUrl"] as? String ?: "",
                                        description = map["description"] as? String ?: "",
                                        technicalNotes = map["technicalNotes"] as? String ?: "",
                                        tags = map["tags"] as? List<String> ?: emptyList(),
                                        timestamp = (map["timestamp"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            beforeAfterList = parsedList
                            isLoading = false
                        }
                        .addOnFailureListener {
                            isLoading = false
                            Toast.makeText(context, "Failed to load", Toast.LENGTH_SHORT).show()
                        }
                }

                LaunchedEffect(Unit) {
                    loadData()
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Manage Before & After") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, "Back")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, "Add")
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else if (beforeAfterList.isEmpty()) {
                            Text(
                                "No transformations added yet.\nTap + to add one.",
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(beforeAfterList) { item ->
                                    BeforeAfterItemCard(
                                        item = item,
                                        onDelete = {
                                            coroutineScope.launch {
                                                try {
                                                    val snapshot = firestore.collection("stylists").document(stylistUid).get().await()
                                                    val currentList = snapshot.get("beforeAfterImages") as? List<Map<String, Any>> ?: emptyList()
                                                    val newList = currentList.filter { it["id"] != item.id }
                                                    
                                                    firestore.collection("stylists").document(stylistUid)
                                                        .update("beforeAfterImages", newList)
                                                        .await()
                                                    
                                                    // Best effort delete from storage
                                                    try {
                                                        storage.getReferenceFromUrl(item.beforeImageUrl).delete().await()
                                                        storage.getReferenceFromUrl(item.afterImageUrl).delete().await()
                                                    } catch (e: Exception) { /* Ignore storage delete errors */ }
                                                    
                                                    loadData()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showAddDialog) {
                    AddBeforeAfterDialog(
                        onDismiss = { showAddDialog = false },
                        onUpload = { beforeUri, afterUri, desc, notes ->
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val id = UUID.randomUUID().toString()
                                    val beforeRef = storage.reference.child("before_after/$stylistUid/${id}_before.jpg")
                                    val afterRef = storage.reference.child("before_after/$stylistUid/${id}_after.jpg")
                                    
                                    beforeRef.putFile(beforeUri).await()
                                    val beforeUrl = beforeRef.downloadUrl.await().toString()
                                    
                                    afterRef.putFile(afterUri).await()
                                    val afterUrl = afterRef.downloadUrl.await().toString()
                                    
                                    val newItem = mapOf(
                                        "id" to id,
                                        "beforeImageUrl" to beforeUrl,
                                        "afterImageUrl" to afterUrl,
                                        "description" to desc,
                                        "technicalNotes" to notes,
                                        "timestamp" to com.google.firebase.Timestamp(Date())
                                    )
                                    
                                    val snapshot = firestore.collection("stylists").document(stylistUid).get().await()
                                    val currentList = snapshot.get("beforeAfterImages") as? List<Map<String, Any>> ?: emptyList()
                                    val newList = currentList + newItem

                                    firestore.collection("stylists").document(stylistUid)
                                        .update("beforeAfterImages", newList)
                                        .await()
                                    
                                    launch(Dispatchers.Main) {
                                        showAddDialog = false
                                        loadData()
                                        Toast.makeText(context, "Uploaded successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BeforeAfterItemCard(item: BeforeAfter, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Before", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    AsyncImage(
                        model = item.beforeImageUrl,
                        contentDescription = "Before",
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("After", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    AsyncImage(
                        model = item.afterImageUrl,
                        contentDescription = "After",
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(item.description, fontWeight = FontWeight.Medium)
            if (item.technicalNotes.isNotEmpty()) {
                Text(item.technicalNotes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Delete")
            }
        }
    }
}

@Composable
fun AddBeforeAfterDialog(
    onDismiss: () -> Unit,
    onUpload: (beforeUri: Uri, afterUri: Uri, desc: String, notes: String) -> Unit
) {
    var beforeUri by remember { mutableStateOf<Uri?>(null) }
    var afterUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    var technicalNotes by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val beforeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) beforeUri = uri
    }
    val afterLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) afterUri = uri
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Add Transformation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { beforeLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (beforeUri != null) {
                            AsyncImage(model = beforeUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PhotoCamera, null)
                                Text("Before", fontSize = 12.sp)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { afterLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (afterUri != null) {
                            AsyncImage(model = afterUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PhotoCamera, null)
                                Text("After", fontSize = 12.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (e.g. Balayage & Cut)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = technicalNotes,
                    onValueChange = { technicalNotes = it },
                    label = { Text("Technical Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (beforeUri != null && afterUri != null && description.isNotBlank()) {
                        isUploading = true
                        onUpload(beforeUri!!, afterUri!!, description, technicalNotes)
                    }
                },
                enabled = !isUploading && beforeUri != null && afterUri != null && description.isNotBlank()
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Upload")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploading) {
                Text("Cancel")
            }
        }
    )
}

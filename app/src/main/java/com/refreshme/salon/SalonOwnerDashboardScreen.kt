package com.refreshme.salon

import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ShopProfile(
    val id: String = "",
    val name: String = "",
    val bio: String = "",
    val address: String = "",
    val phone: String = "",
    val website: String = "",
    val isPublic: Boolean = false,
    val stylistIds: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalonOwnerDashboardScreen(
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var shopProfile by remember { mutableStateOf<ShopProfile?>(null) }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid }

    DisposableEffect(uid) {
        var shopListener: ListenerRegistration? = null
        val userListener = uid?.let { userId ->
            db.collection("users").document(userId)
                .addSnapshotListener { userSnapshot, _ ->
                val shopId = userSnapshot?.getString("shopId")
                if (shopId.isNullOrBlank()) {
                    val newShopId = db.collection("shops").document().id
                    val fallbackName = FirebaseAuth.getInstance().currentUser?.displayName
                        ?.takeIf { it.isNotBlank() }
                        ?: "My Shop"
                    db.collection("shops").document(newShopId).set(
                        mapOf(
                            "ownerId" to userId,
                            "name" to fallbackName,
                            "stylistIds" to listOf(userId),
                            "isPublic" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    db.collection("users").document(userId).set(
                        mapOf("shopId" to newShopId, "businessName" to fallbackName),
                        SetOptions.merge()
                    )
                    return@addSnapshotListener
                }
                shopListener?.remove()
                shopListener = db.collection("shops").document(shopId)
                    .addSnapshotListener { shopSnapshot, _ ->
                        val data = shopSnapshot?.data.orEmpty()
                        @Suppress("UNCHECKED_CAST")
                        val stylistIds = (data["stylistIds"] as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?: emptyList()
                        shopProfile = ShopProfile(
                            id = shopSnapshot?.id ?: shopId,
                            name = data["name"] as? String ?: "",
                            bio = data["bio"] as? String ?: "",
                            address = data["address"] as? String ?: "",
                            phone = data["phone"] as? String ?: "",
                            website = data["website"] as? String ?: "",
                            isPublic = data["isPublic"] as? Boolean ?: false,
                            stylistIds = stylistIds
                        )
                    }
            }
            }

        onDispose {
            userListener?.remove()
            shopListener?.remove()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Salon Dashboard", 
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Overview") },
                    label = { Text("Overview") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Staff") },
                    label = { Text("Staff") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = "Settings") },
                    label = { Text("Salon") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> OverviewTab(shopProfile)
                1 -> StaffTab(shopProfile)
                2 -> SalonSettingsTab(shopProfile)
            }
        }
    }
}

@Composable
fun OverviewTab(shopProfile: ShopProfile?) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                shopProfile?.name?.takeIf { it.isNotBlank() } ?: "Your Shop",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                if (shopProfile?.isPublic == true) "Public listing is live" else "Draft listing",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
        }
        
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(title = "Team Members", value = "${shopProfile?.stylistIds?.size ?: 0}", icon = Icons.Default.Groups, modifier = Modifier.weight(1f))
                StatCard(title = "Listing Status", value = if (shopProfile?.isPublic == true) "Live" else "Draft", icon = Icons.Default.Storefront, modifier = Modifier.weight(1f))
            }
        }
        
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Shop Details", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    DetailLine(Icons.Default.Place, shopProfile?.address?.takeIf { it.isNotBlank() } ?: "No address set")
                    DetailLine(Icons.Default.Phone, shopProfile?.phone?.takeIf { it.isNotBlank() } ?: "No phone set")
                    DetailLine(Icons.Default.Language, shopProfile?.website?.takeIf { it.isNotBlank() } ?: "No website set")
                }
            }
        }
    }
}

@Composable
fun StaffTab(shopProfile: ShopProfile?) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manage Staff", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Button(onClick = { Toast.makeText(context, "Invite Staff Feature Coming Soon!", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Invite")
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("${shopProfile?.stylistIds?.size ?: 0} connected team member(s)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Staff invites and permission levels are ready for the next pass; this now reflects your real shop record instead of sample staff.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SalonSettingsTab(shopProfile: ShopProfile?) {
    val context = LocalContext.current
    var showShopDialog by remember { mutableStateOf(false) }

    if (showShopDialog) {
        ShopProfileDialog(
            shopProfile = shopProfile,
            onDismiss = { showShopDialog = false },
            onSave = { updated ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null || updated.id.isBlank()) {
                    Toast.makeText(context, "Shop profile is still loading.", Toast.LENGTH_SHORT).show()
                } else {
                    val updates = mapOf(
                        "name" to updated.name,
                        "bio" to updated.bio,
                        "address" to updated.address,
                        "phone" to updated.phone,
                        "website" to updated.website,
                        "isPublic" to updated.isPublic,
                        "stylistIds" to updated.stylistIds.ifEmpty { listOf(uid) },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    val db = FirebaseFirestore.getInstance()
                    db.collection("shops").document(updated.id)
                        .set(updates, SetOptions.merge())
                        .addOnSuccessListener {
                            db.collection("users").document(uid).set(
                                mapOf(
                                    "shopId" to updated.id,
                                    "businessName" to updated.name,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                ),
                                SetOptions.merge()
                            )
                            Toast.makeText(context, "Shop profile updated.", Toast.LENGTH_SHORT).show()
                            showShopDialog = false
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Could not update shop profile.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Salon Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        
        SettingRow(Icons.Default.Store, "Shop Profile", "Edit name, address, and visibility") { showShopDialog = true }
        SettingRow(Icons.Default.Schedule, "Business Hours", "Set master operating hours") { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() }
        SettingRow(Icons.Default.AccountBalanceWallet, "Payouts & Taxes", "Manage salon bank account") { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() }
        SettingRow(Icons.Default.Shield, "Permissions", "Manager vs Staff access") { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() }
    }
}

@Composable
fun DetailLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
fun ShopProfileDialog(
    shopProfile: ShopProfile?,
    onDismiss: () -> Unit,
    onSave: (ShopProfile) -> Unit
) {
    var name by remember(shopProfile?.id) { mutableStateOf(shopProfile?.name.orEmpty()) }
    var bio by remember(shopProfile?.id) { mutableStateOf(shopProfile?.bio.orEmpty()) }
    var address by remember(shopProfile?.id) { mutableStateOf(shopProfile?.address.orEmpty()) }
    var phone by remember(shopProfile?.id) { mutableStateOf(shopProfile?.phone.orEmpty()) }
    var website by remember(shopProfile?.id) { mutableStateOf(shopProfile?.website.orEmpty()) }
    var isPublic by remember(shopProfile?.id) { mutableStateOf(shopProfile?.isPublic == true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shop Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Shop name") })
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Short description") }, minLines = 2)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
                OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Public listing", fontWeight = FontWeight.Bold)
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && shopProfile?.id?.isNotBlank() == true,
                onClick = {
                    onSave(
                        (shopProfile ?: ShopProfile()).copy(
                            name = name.trim(),
                            bio = bio.trim(),
                            address = address.trim(),
                            phone = phone.trim(),
                            website = website.trim(),
                            isPublic = isPublic
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ActivityItem(text: String, time: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(top = 6.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

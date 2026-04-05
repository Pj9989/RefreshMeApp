package com.refreshme.salon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalonOwnerDashboardScreen(
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    
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
                        Icon(Icons.Default.Logout, contentDescription = "Log out")
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
                0 -> OverviewTab()
                1 -> StaffTab()
                2 -> SalonSettingsTab()
            }
        }
    }
}

@Composable
fun OverviewTab() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Weekly Performance", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(title = "Gross Revenue", value = "$4,250", icon = Icons.Default.AttachMoney, modifier = Modifier.weight(1f))
                StatCard(title = "Total Bookings", value = "84", icon = Icons.Default.Event, modifier = Modifier.weight(1f))
            }
        }
        
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(title = "New Clients", value = "12", icon = Icons.Default.PersonAdd, modifier = Modifier.weight(1f))
                StatCard(title = "Staff Active", value = "5/6", icon = Icons.Default.Groups, modifier = Modifier.weight(1f))
            }
        }
        
        item {
            Spacer(Modifier.height(16.dp))
            Text("Recent Shop Activity", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            // Mock Activity Feed
            ActivityItem("Sarah booked a balayage with Michael", "10 mins ago")
            ActivityItem("John cancelled his 2:00 PM cut", "1 hr ago")
            ActivityItem("New client review: 5 stars for Emily!", "2 hrs ago")
        }
    }
}

@Composable
fun StaffTab() {
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
                Button(onClick = { /* TODO: Invite Staff */ }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Invite")
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        items(5) { index ->
            val names = listOf("Michael B.", "Emily R.", "David S.", "Jessica P.", "Chris T.")
            val roles = listOf("Master Barber", "Color Specialist", "Stylist", "Stylist", "Junior Stylist")
            val isOnline = index < 3
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(names[index].take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(names[index], fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(roles[index], color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                    Surface(
                        color = if (isOnline) Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isOnline) "Clocked In" else "Offline",
                            color = if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SalonSettingsTab() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Salon Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        
        SettingRow(Icons.Default.Store, "Shop Profile", "Edit name, address, and photos")
        SettingRow(Icons.Default.Schedule, "Business Hours", "Set master operating hours")
        SettingRow(Icons.Default.AccountBalanceWallet, "Payouts & Taxes", "Manage salon bank account")
        SettingRow(Icons.Default.Shield, "Permissions", "Manager vs Staff access")
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
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

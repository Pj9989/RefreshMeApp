package com.refreshme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.auth.SignInActivity
import com.refreshme.booking.BookingsScreen
import com.refreshme.chat.ChatScreen
import com.refreshme.profile.UserProfileScreen
import com.refreshme.stylist.SubscriptionActivity
import com.refreshme.ui.theme.RefreshMeTheme

class MainActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DatabaseSeeder.seedData()
        setContent {
            RefreshMeTheme {
                MainScreen()
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

    @Composable
    fun MainScreen() {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val auth = FirebaseAuth.getInstance()

        // Determine if we should show the bottom bar
        val showBottomBar = currentRoute in listOf("dashboard", "stylist_list", "bookings", "profile")

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    BottomNavigationBar(navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (auth.currentUser != null) "dashboard" else "login",
                modifier = Modifier.padding(innerPadding)
            ) {
                // --- AUTH ROUTES ---
                composable("login") {
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        val intent = Intent(context, SignInActivity::class.java)
                        context.startActivity(intent)
                    }
                }

                // --- MAIN ROUTES ---
                composable("dashboard") {
                    DashboardScreen(
                        onFindStylist = { navController.navigate("stylist_list") },
                        onMyBookings = { navController.navigate("bookings") },
                        onStylistClick = { stylist -> navController.navigate("stylist_details/${stylist.id}") }
                    )
                }

                composable("stylist_list") {
                    StylistListRoute(
                        onStylistClick = { stylist ->
                            navController.navigate("stylist_details/${stylist.id}")
                        }
                    )
                }

                composable("bookings") {
                    BookingsScreen(onBack = { navController.popBackStack() })
                }

                composable("profile") {
                    UserProfileScreen(
                        onEditProfile = { /* Navigate to edit profile */ },
                        onManageSubscription = {
                            val intent = Intent(this@MainActivityCompose, SubscriptionActivity::class.java)
                            startActivity(intent)
                        },
                        onSignOut = {
                            auth.signOut()
                            navController.navigate("login") { popUpTo("profile") { inclusive = true } }
                        },
                        onViewBookings = { navController.navigate("bookings") }
                    )
                }

                // --- FEATURE ROUTES ---
                composable(
                    route = "stylist_details/{stylistId}",
                    arguments = listOf(navArgument("stylistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val stylistId = backStackEntry.arguments?.getString("stylistId") ?: return@composable
                    EnhancedStylistProfileScreen(
                        stylistId = stylistId,
                        onBack = { navController.popBackStack() },
                        onBookClick = { id -> navController.navigate("booking/$id") },
                        onChatClick = { id -> navController.navigate("chat/$id") }
                    )
                }

                composable(
                    route = "booking/{stylistId}",
                    arguments = listOf(navArgument("stylistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val stylistId = backStackEntry.arguments?.getString("stylistId") ?: return@composable
                    BookingScreen(
                        stylistId = stylistId,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "chat/{otherUserId}",
                    arguments = listOf(navArgument("otherUserId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: return@composable
                    val currentUserId = auth.currentUser?.uid ?: ""
                    ChatScreen(
                        otherUserId = otherUserId,
                        currentUserId = currentUserId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: androidx.navigation.NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val items = listOf(
            NavigationItem("Dashboard", "dashboard", Icons.Default.Home),
            NavigationItem("Explore", "stylist_list", Icons.Default.Search),
            NavigationItem("Bookings", "bookings", Icons.Default.DateRange),
            NavigationItem("Profile", "profile", Icons.Default.Person)
        )

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentDestination?.route == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class NavigationItem(val title: String, val route: String, val icon: ImageVector)

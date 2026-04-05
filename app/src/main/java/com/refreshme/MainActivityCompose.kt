package com.refreshme

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.auth.SignInActivity
import com.refreshme.auth.RoleSelectActivity
import com.refreshme.profile.EditProfileActivity
import com.refreshme.stylist.MyStylistProfileScreen
import com.refreshme.ui.theme.RefreshMeTheme
import com.refreshme.util.RoleBasedNavigationManager
import com.refreshme.util.RoleBasedNavigationManager.UserRole
import com.refreshme.util.UserManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// Note: This file appears to be an unused experiment with Navigation Compose.
// Currently, the app uses Navigation Component with XML.

class MainActivityComposeViewModel(application: Application) : AndroidViewModel(application) {
    var currentUser by mutableStateOf<User?>(null)
        private set
    var userRole by mutableStateOf<String?>(null)
        private set

    init {
        refreshUser()
    }

    fun refreshUser() {
        viewModelScope.launch {
            currentUser = UserManager.getCurrentUser(forceRefresh = true)
            userRole = UserManager.getUserRole(forceRefresh = true)
        }
    }
}

@AndroidEntryPoint
class MainActivityCompose : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val viewModel = ViewModelProvider(this)[MainActivityComposeViewModel::class.java]

        setContent {
            RefreshMeTheme {
                MainAppScreen(viewModel = viewModel, context = this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val viewModel = ViewModelProvider(this)[MainActivityComposeViewModel::class.java]
        viewModel.refreshUser()
    }
}

@Composable
fun MainAppScreen(viewModel: MainActivityComposeViewModel, context: Context) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()

    val currentUserData = viewModel.currentUser
    val currentUserRole = viewModel.userRole

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define which routes show the bottom bar
    val showBottomBar = currentRoute in listOf("home", "search", "schedule", "profile")

    // Determine start destination based on auth state
    var startDestination by remember { mutableStateOf("splash") }

    LaunchedEffect(auth.currentUser) {
        if (auth.currentUser == null) {
            startDestination = "login"
        } else {
            val role = UserManager.getUserRole()
            startDestination = if (role == null) {
                "role_select"
            } else {
                "home"
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") { launchSingleTop = true; restoreState = true } }
                    )
                    
                    if (currentUserRole == Role.CUSTOMER.name) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search") },
                            selected = currentRoute == "search",
                            onClick = { navController.navigate("search") { launchSingleTop = true; restoreState = true } }
                        )
                    }
                    
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Schedule, contentDescription = "Schedule") },
                        label = { Text("Schedule") },
                        selected = currentRoute == "schedule",
                        onClick = { navController.navigate("schedule") { launchSingleTop = true; restoreState = true } }
                    )
                    
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = currentRoute == "profile",
                        onClick = { navController.navigate("profile") { launchSingleTop = true; restoreState = true } }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = navController, startDestination = startDestination) {
                
                composable("splash") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                composable("login") {
                    // Start traditional SignInActivity
                    LaunchedEffect(Unit) {
                        val intent = Intent(context, SignInActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }
                }

                composable("role_select") {
                    // Start traditional RoleSelectActivity
                    LaunchedEffect(Unit) {
                        val intent = Intent(context, RoleSelectActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }
                }

                composable("home") {
                    if (currentUserData == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        when (currentUserRole) {
                            Role.STYLIST.name -> {
                                // For stylist, launch StylistDashboardActivity
                                LaunchedEffect(Unit) {
                                    RoleBasedNavigationManager.navigateToDashboard(context, UserRole.STYLIST)
                                }
                            }
                            Role.CUSTOMER.name -> {
                                // For customer, launch traditional MainActivity
                                LaunchedEffect(Unit) {
                                    RoleBasedNavigationManager.navigateToDashboard(context, UserRole.CUSTOMER)
                                }
                            }
                            else -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Unknown role")
                                }
                            }
                        }
                    }
                }

                composable("search") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Text("Search Placeholder")
                    }
                }

                composable("schedule") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Text("Schedule Screen Placeholder")
                    }
                }

                composable("profile") {
                    if (currentUserData == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        when (currentUserRole) {
                            Role.STYLIST.name -> {
                                MyStylistProfileScreen(
                                    user = currentUserData,
                                    onEditProfile = { 
                                        val intent = Intent(context, EditProfileActivity::class.java)
                                        intent.putExtra("IS_STYLIST", true)
                                        context.startActivity(intent)
                                    },
                                    onSignOut = {
                                        coroutineScope.launch {
                                            auth.signOut()
                                            UserManager.clear()
                                            navController.navigate("login") { popUpTo("profile") { inclusive = true } }
                                        }
                                    },
                                    onViewSchedule = { navController.navigate("schedule") },
                                    onDeleteAccount = {
                                        val user = auth.currentUser
                                        user?.delete()?.addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                                coroutineScope.launch {
                                                    auth.signOut()
                                                    UserManager.clear()
                                                    navController.navigate("login") { popUpTo("profile") { inclusive = true } }
                                                }
                                            } else {
                                                Toast.makeText(context, "Failed to delete account. You may need to sign in again first.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                )
                            }
                            Role.CUSTOMER.name -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Customer Profile Placeholder")
                                }
                            }
                            else -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Unknown role")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
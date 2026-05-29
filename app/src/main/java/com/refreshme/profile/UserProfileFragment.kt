package com.refreshme.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.CustomerDashboardActivity
import com.refreshme.R
import com.refreshme.auth.SignInActivity
import com.refreshme.stylist.StylistDashboardActivity
import com.refreshme.ui.theme.RefreshMeTheme
import com.refreshme.util.RoleBasedNavigationManager
import com.refreshme.util.RoleBasedNavigationManager.UserRole
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    @Inject
    lateinit var auth: FirebaseAuth
    
    private val viewModel: UserProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RefreshMeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        UserProfileScreen(
                            viewModel = viewModel,
                            onEditProfile = {
                                val intent = Intent(activity, EditProfileActivity::class.java)
                                intent.putExtra("IS_STYLIST", false)
                                startActivity(intent)
                            },
                            onSignOut = {
                                auth.signOut()
                                val intent = Intent(activity, SignInActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            },
                            onViewSavedStylists = {
                                findNavController().navigate(R.id.favoritesFragment)
                            },
                            onSwitchToStylistDashboard = {
                                val intent = Intent(requireContext(), StylistDashboardActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                startActivity(intent)
                                activity?.finish()
                            },
                            onViewBookings = {
                                findNavController().navigate(R.id.bookingsFragment)
                            },
                            isStylistBrowseMode = requireActivity()
                                .intent
                                .getBooleanExtra(CustomerDashboardActivity.EXTRA_ALLOW_STYLIST_BROWSE, false),
                            resolveRole = { callback ->
                                RoleBasedNavigationManager.getUserRole { role ->
                                    callback(role == UserRole.STYLIST)
                                }
                            },
                            onDeleteAccount = {
                                val user = auth.currentUser
                                user?.delete()?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(activity, SignInActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to delete account. You may need to sign out and sign in again before deleting.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

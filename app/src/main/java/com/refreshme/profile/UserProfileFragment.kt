package com.refreshme.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.R
import com.refreshme.auth.SignInActivity
import com.refreshme.stylist.SubscriptionActivity
import com.refreshme.ui.theme.RefreshMeTheme

class UserProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    UserProfileScreen(
                        onEditProfile = {
                            startActivity(Intent(activity, EditProfileActivity::class.java))
                        },
                        onManageSubscription = {
                            startActivity(Intent(activity, SubscriptionActivity::class.java))
                        },
                        onSignOut = {
                            auth.signOut()
                            val intent = Intent(activity, SignInActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        },
                        onViewBookings = {
                            findNavController().navigate(R.id.bookingsFragment)
                        }
                    )
                }
            }
        }
    }
}
package com.refreshme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.auth.SignInActivity
import com.refreshme.stylist.StylistDashboardActivity

class MainActivity : AppCompatActivity() {

    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            launchActivity(SignInActivity::class.java)
            return
        }

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val roleRaw = document.getString("role")
                val role = roleRaw?.uppercase()
                Log.d(
                    "ROLE_DEBUG",
                    "uid=${currentUser.uid} docExists=${document.exists()} roleRaw=$roleRaw"
                )

                if (role.isNullOrBlank()) {
                    Log.e("ROLE_DEBUG", "ROLE MISSING. Check Firestore users/${currentUser.uid}")
                    launchActivity(CustomerDashboardActivity::class.java)
                    return@addOnSuccessListener
                }

                if (role == "STYLIST") {
                    launchActivity(StylistDashboardActivity::class.java)
                } else {
                    launchActivity(CustomerDashboardActivity::class.java)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to fetch user role, defaulting to Customer.", e)
                launchActivity(CustomerDashboardActivity::class.java)
            }
    }

    private fun <T : AppCompatActivity> launchActivity(activityClass: Class<T>) {
        startActivity(Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
    }
}
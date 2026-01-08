package com.refreshme.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.MainActivity
import com.refreshme.databinding.ActivitySignUpBinding
import com.refreshme.util.AnalyticsHelper
import java.util.Calendar

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.signUpButton.setOnClickListener {
            signUpUser()
        }

        binding.signInTextView.setOnClickListener {
            finish() // Go back to SignInActivity
        }
    }

    private fun signUpUser() {
        val name = binding.nameEditText.text.toString()
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val selectedRoleId = binding.roleRadioGroup.checkedRadioButtonId

        if (selectedRoleId == -1) {
            Toast.makeText(this, "Please select a role.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRole = when (selectedRoleId) {
            binding.stylistRadioButton.id -> "STYLIST"
            binding.customerRadioButton.id -> "CUSTOMER"
            else -> "CUSTOMER"
        }
        Log.d("SignUpActivity", "Selected role: $selectedRole")


        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Invalid email format"
            return
        } else {
            binding.emailLayout.error = null
        }

        if (password.length < 6) {
            binding.passwordLayout.error = "Password must be at least 6 characters"
            return
        } else {
            binding.passwordLayout.error = null
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.signUpButton.isEnabled = false

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileUpdateTask ->
                        if (profileUpdateTask.isSuccessful) {
                            val userId = user.uid
                            val userMap = hashMapOf<String, Any>(
                                "name" to name,
                                "email" to email,
                                "role" to selectedRole,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "verified" to false
                            )

                            if (selectedRole == "STYLIST") {
                                val calendar = Calendar.getInstance()
                                calendar.add(Calendar.MONTH, 1)
                                userMap["subscriptionActive"] = true // Free trial is active
                                userMap["subscriptionSource"] = "google_play"
                                userMap["lastPurchaseTime"] = System.currentTimeMillis()
                            }

                            firestore.collection("users").document(userId)
                                .set(userMap)
                                .addOnSuccessListener {
                                    firestore.collection("users").document(userId).get()
                                        .addOnSuccessListener { doc ->
                                            val savedRole = doc.getString("role")
                                            Log.d("SignUpActivity", "ROLE SAVED IN FIRESTORE = $savedRole for uid: $userId")
                                            Toast.makeText(this, "Signed up as: $savedRole", Toast.LENGTH_LONG).show()

                                            binding.progressBar.visibility = View.GONE
                                            binding.signUpButton.isEnabled = true

                                            // IMPORTANT: analytics key should not be hardcoded to "customer"
                                            AnalyticsHelper.setUserProperties("role", selectedRole)

                                            startActivity(MainActivity.newIntent(this))
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("SignUpActivity", "Failed to re-read user doc: ${e.message}")
                                            binding.progressBar.visibility = View.GONE
                                            binding.signUpButton.isEnabled = true
                                            startActivity(MainActivity.newIntent(this))
                                            finish()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    binding.progressBar.visibility = View.GONE
                                    binding.signUpButton.isEnabled = true
                                    Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            binding.progressBar.visibility = View.GONE
                            binding.signUpButton.isEnabled = true
                            Toast.makeText(this, "Failed to update profile: ${profileUpdateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.signUpButton.isEnabled = true
                    Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SignUpActivity::class.java)
        }
    }
}
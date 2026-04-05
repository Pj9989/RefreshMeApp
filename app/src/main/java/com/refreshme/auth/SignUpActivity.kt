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
import com.refreshme.R
import com.refreshme.databinding.ActivitySignUpBinding
import com.refreshme.stylist.StylistDashboardActivity
import com.refreshme.util.AnalyticsHelper
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            Toast.makeText(this, getString(R.string.error_select_role), Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRole = when (selectedRoleId) {
            binding.stylistRadioButton.id -> "STYLIST"
            binding.customerRadioButton.id -> "CUSTOMER"
            else -> "CUSTOMER"
        }

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.error_invalid_email_format)
            return
        } else {
            binding.emailLayout.error = null
        }

        if (password.length < 6) {
            binding.passwordLayout.error = getString(R.string.error_password_min_length)
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
                    val userId = user?.uid ?: return@addOnCompleteListener
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            // 1. Update user profile name
                            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                            user.updateProfile(profileUpdates).await()

                            // 2. Send verification email
                            try {
                                user.sendEmailVerification().await()
                                Toast.makeText(this@SignUpActivity, "Verification email sent to $email", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("SignUpActivity", "Failed to send verification email: ${e.message}")
                            }

                            // 3. Create /users/{uid} document
                            val userMap = hashMapOf<String, Any>(
                                "name" to name,
                                "email" to email,
                                "role" to selectedRole,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "verified" to false
                            )
                            firestore.collection("users").document(userId).set(userMap).await()

                            // 4. If STYLIST, grant trial in /stylists/{uid} document
                            if (selectedRole == "STYLIST") {
                                val stylistTrialMap = hashMapOf<String, Any>(
                                    "trialStartTime" to FieldValue.serverTimestamp(),
                                    "subscriptionActive" to false,
                                    "name" to name
                                )
                                firestore.collection("stylists").document(userId).set(stylistTrialMap).await()
                            }

                            Toast.makeText(this@SignUpActivity, getString(R.string.signup_success_format, selectedRole), Toast.LENGTH_LONG).show()

                            // 5. Analytics
                            AnalyticsHelper.setUserProperties("role", selectedRole)

                            // 6. Navigation: Route to Onboarding so that users can configure their newly created accounts
                            val onboardingIntent = if (selectedRole == "STYLIST") {
                                Intent(this@SignUpActivity, StylistOnboardingActivity::class.java)
                            } else {
                                Intent(this@SignUpActivity, CustomerOnboardingActivity::class.java)
                            }
                            
                            onboardingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(onboardingIntent)
                            finish()

                        } catch (e: Exception) {
                            Log.e("SignUpActivity", "Failed to initialize user data: ${e.message}", e)
                            Toast.makeText(this@SignUpActivity, getString(R.string.error_save_user_data), Toast.LENGTH_SHORT).show()
                            firebaseAuth.signOut()
                        } finally {
                            binding.progressBar.visibility = View.GONE
                            binding.signUpButton.isEnabled = true
                        }
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.signUpButton.isEnabled = true
                    Log.e("Auth", "Sign up failed", task.exception)
                    Toast.makeText(this, getString(R.string.error_signup_failed) + ": ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SignUpActivity::class.java)
        }
    }
}
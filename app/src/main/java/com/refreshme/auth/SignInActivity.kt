package com.refreshme.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.refreshme.R
import com.refreshme.databinding.ActivitySignInBinding
import com.refreshme.databinding.DialogForgotPasswordBinding
import com.refreshme.util.RoleBasedNavigationManager
import com.refreshme.util.RoleBasedNavigationManager.UserRole
import java.util.concurrent.Executor

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // Demo account credentials for Google Play reviewers
    private val DEMO_EMAIL = "tester@refreshme.com"
    private val DEMO_PASSWORD = "testpassword123"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        firebaseAuth = FirebaseAuth.getInstance()
        executor = ContextCompat.getMainExecutor(this)

        if (firebaseAuth.currentUser != null) {
            binding = ActivitySignInBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            binding.emailLayout.visibility = View.GONE
            binding.passwordLayout.visibility = View.GONE
            binding.signInButton.visibility = View.GONE
            binding.demoLoginButton.visibility = View.GONE
            binding.signUpTextView.visibility = View.GONE
            binding.forgotPasswordTextView.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            
            updateFcmToken()
            
            // Check if Biometrics are available
            val biometricManager = BiometricManager.from(this)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                setupBiometricPrompt()
                biometricPrompt.authenticate(promptInfo)
            } else {
                checkUserRoleAndNavigate()
            }
            return
        }

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signInButton.setOnClickListener {
            signInUser()
        }

        binding.demoLoginButton.setOnClickListener {
            signInAsDemo()
        }

        binding.signUpTextView.setOnClickListener {
            startActivity(SignUpActivity.newIntent(this))
        }

        binding.forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
        }

        // Demo Login button: automatically fills and signs in with the reviewer test account
        binding.demoLoginButton.setOnClickListener {
            signInWithDemoAccount()
        }
    }

    private fun signInWithDemoAccount() {
        binding.progressBar.visibility = View.VISIBLE
        binding.demoLoginButton.isEnabled = false
        binding.signInButton.isEnabled = false

        firebaseAuth.signInWithEmailAndPassword(DEMO_EMAIL, DEMO_PASSWORD)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, com.refreshme.MainActivity::class.java))
                    finish()
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.demoLoginButton.isEnabled = true
                    binding.signInButton.isEnabled = true
                    Log.e("SignInActivity", "Demo login failed", task.exception)
                    Toast.makeText(
                        this,
                        "Demo login failed. Please ensure the test account is set up in Firebase.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setupBiometricPrompt() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    // If they cancel, they can't get in unless they sign out, or we can just let them in fallback.
                    // For security, if it fails, maybe log them out or just finish the app.
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        finish()
                    } else {
                        checkUserRoleAndNavigate()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                    checkUserRoleAndNavigate()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for RefreshMe")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
    }

    private fun updateFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val uid = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(uid).update("fcmToken", token)
                db.collection("stylists").document(uid).update("fcmToken", token)
            }
        }
    }

    private fun checkUserRoleAndNavigate() {
        RoleBasedNavigationManager.getUserRole { role ->
            if (!isFinishing) {
                if (role == UserRole.UNKNOWN) {
                    startActivity(Intent(this, RoleSelectActivity::class.java))
                } else {
                    RoleBasedNavigationManager.navigateToDashboard(this, role)
                }
                finish()
            }
        }
    }

    private fun signInUser() {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.error_invalid_email_format)
            return
        } else {
            binding.emailLayout.error = null
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.signInButton.isEnabled = false
        binding.demoLoginButton.isEnabled = false

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateFcmToken()
                // After manual sign in, we just navigate.
                checkUserRoleAndNavigate()
            } else {
                binding.progressBar.visibility = View.GONE
                binding.signInButton.isEnabled = true
                binding.demoLoginButton.isEnabled = true
                val exception = task.exception
                val errorMessage = when (exception) {
                    is FirebaseAuthInvalidUserException -> getString(R.string.error_no_account_found)
                    is FirebaseAuthInvalidCredentialsException -> getString(R.string.error_invalid_password)
                    else -> getString(R.string.error_auth_failed)
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInAsDemo() {
        // NOTE: Create this user in your Firebase Console first
        val demoEmail = "tester@refreshme.com"
        val demoPassword = "testpassword123"

        binding.progressBar.visibility = View.VISIBLE
        binding.signInButton.isEnabled = false
        binding.demoLoginButton.isEnabled = false

        firebaseAuth.signInWithEmailAndPassword(demoEmail, demoPassword).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateFcmToken()
                checkUserRoleAndNavigate()
            } else {
                binding.progressBar.visibility = View.GONE
                binding.signInButton.isEnabled = true
                binding.demoLoginButton.isEnabled = true
                Toast.makeText(this, "Demo Login Failed. Ensure tester account exists in Firebase.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogBinding = DialogForgotPasswordBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.forgot_password_title))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.send_button), null)
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val email = dialogBinding.emailEditText.text.toString()
                if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    firebaseAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, getString(R.string.password_reset_sent), Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(this, getString(R.string.error_send_reset_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, getString(R.string.error_enter_valid_email), Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SignInActivity::class.java)
        }
    }
}
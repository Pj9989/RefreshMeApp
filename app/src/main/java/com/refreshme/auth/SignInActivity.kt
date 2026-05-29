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
            binding.signUpTextView.visibility = View.GONE
            binding.forgotPasswordTextView.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            
            // Gate: require email verification (with reviewer bypass) before allowing app entry.
            verifyEmailAndProceed {
                updateFcmToken()

                // Check if Biometrics are available
                val biometricManager = BiometricManager.from(this)
                if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                    setupBiometricPrompt()
                    biometricPrompt.authenticate(promptInfo)
                } else {
                    checkUserRoleAndNavigate()
                }
            }
            return
        }

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signInButton.setOnClickListener {
            signInUser()
        }

        binding.signUpTextView.setOnClickListener {
            startActivity(SignUpActivity.newIntent(this))
        }

        binding.forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
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

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Gate: require email verification (with reviewer bypass) before allowing app entry.
                verifyEmailAndProceed {
                    updateFcmToken()
                    checkUserRoleAndNavigate()
                }
            } else {
                binding.progressBar.visibility = View.GONE
                binding.signInButton.isEnabled = true
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

    /**
     * Refreshes the current Firebase user and only invokes [onVerified] if the user's email
     * is verified, or the email is on the reviewer allowlist. Otherwise signs the user out
     * and shows the "verify your email" dialog.
     *
     * Mirrors the iOS gate in lib/providers/auth_provider.dart so both platforms refuse to
     * let unverified accounts into the app.
     */
    private fun verifyEmailAndProceed(onVerified: () -> Unit) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            onVerified()
            return
        }

        // Reviewer accounts bypass verification (for Apple / Google App Review).
        if (isReviewerEmail(user.email)) {
            onVerified()
            return
        }

        user.reload().addOnCompleteListener {
            val refreshedUser = firebaseAuth.currentUser
            if (refreshedUser != null && refreshedUser.isEmailVerified) {
                onVerified()
            } else {
                firebaseAuth.signOut()
                showEmailNotVerifiedDialog()
            }
        }
    }

    private fun isReviewerEmail(email: String?): Boolean {
        return email?.lowercase() in REVIEWER_EMAILS
    }

    private fun showEmailNotVerifiedDialog() {
        if (isFinishing) return

        // Reveal the sign-in form again (in case we just came from a persisted-session boot
        // where the form fields were hidden behind the progress spinner).
        binding.emailLayout.visibility = View.VISIBLE
        binding.passwordLayout.visibility = View.VISIBLE
        binding.signInButton.visibility = View.VISIBLE
        binding.signUpTextView.visibility = View.VISIBLE
        binding.forgotPasswordTextView.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.signInButton.isEnabled = true

        AlertDialog.Builder(this)
            .setTitle("Verify your email")
            .setMessage(
                "Please verify your email address before logging in. " +
                    "Check your inbox for the verification link."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SignInActivity::class.java)
        }

        // Demo / reviewer accounts that bypass email verification.
        // ONLY accounts created specifically for App Review (Apple / Google) belong here.
        // Real human accounts must verify like everyone else.
        private val REVIEWER_EMAILS = setOf(
            "reviewer@refreshmeapp.com",
            "tester@refreshmeapp.com",
            "appreviewer@refreshmeapp.com",
            "reviewer.customer@refreshmeapp.com",
            "reviewer.stylist@refreshmeapp.com"
        )
    }
}
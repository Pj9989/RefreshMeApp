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

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth

    // Demo account credentials for Google Play reviewers
    private val DEMO_EMAIL = "tester@refreshme.com"
    private val DEMO_PASSWORD = "testpassword123"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser != null) {
            binding = ActivitySignInBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            binding.emailLayout.visibility = View.GONE
            binding.passwordLayout.visibility = View.GONE
            binding.signInButton.visibility = View.GONE
            binding.roleRadioGroup.visibility = View.GONE
            binding.signUpTextView.visibility = View.GONE
            binding.forgotPasswordTextView.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            
            updateFcmToken()
            checkUserRoleAndNavigate()
            return
        }

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.customerRadioButton.setText(R.string.customer)
        binding.stylistRadioButton.setText(R.string.stylist)

        binding.signInButton.setOnClickListener {
            signInUser()
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
                    startActivity(MainActivity.newIntent(this))
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
                updateFcmToken()
                checkUserRoleAndNavigate()
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

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SignInActivity::class.java)
        }
    }
}

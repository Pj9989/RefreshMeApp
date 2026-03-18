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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.MainActivity
import com.refreshme.databinding.ActivitySignInBinding
import com.refreshme.databinding.DialogForgotPasswordBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    // Demo account credentials for Google Play reviewers
    private val DEMO_EMAIL = "tester@refreshme.com"
    private val DEMO_PASSWORD = "testpassword123"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

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

    private fun signInUser() {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Invalid email format"
            return
        } else {
            binding.emailLayout.error = null
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.signInButton.isEnabled = false

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // **FIXED**: ALWAYS go to MainActivity. It will handle the role routing.
                startActivity(MainActivity.newIntent(this))
                finish()
            } else {
                binding.progressBar.visibility = View.GONE
                binding.signInButton.isEnabled = true
                val exception = task.exception
                val errorMessage = when (exception) {
                    is FirebaseAuthInvalidUserException -> "No account found with this email."
                    is FirebaseAuthInvalidCredentialsException -> "Invalid password. Please try again."
                    else -> "Authentication failed. Please try again later."
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogBinding = DialogForgotPasswordBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Forgot Password")
            .setView(dialogBinding.root)
            .setPositiveButton("Send", null)
            .setNegativeButton("Cancel") { dialog, _ ->
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
                                Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(this, "Failed to send reset email.", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Please enter a valid email.", Toast.LENGTH_SHORT).show()
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

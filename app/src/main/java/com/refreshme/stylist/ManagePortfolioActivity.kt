package com.refreshme.stylist

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.refreshme.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ManagePortfolioActivity : AppCompatActivity() {

    private lateinit var portfolioRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var uploadPhotoButton: Button
    private lateinit var portfolioAdapter: PortfolioAdapter

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val stylistUid: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in.")

    // Activity Result Launcher for picking images from gallery
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let { uploadPortfolioImage(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_portfolio)

        setupToolbar()
        setupViews()
        setupRecyclerView()
        loadPortfolio()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.manage_portfolio)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupViews() {
        portfolioRecyclerView = findViewById(R.id.portfolio_recycler_view)
        emptyStateText = findViewById(R.id.empty_state_text)
        uploadPhotoButton = findViewById(R.id.upload_photo_button)

        uploadPhotoButton.setOnClickListener {
            openImagePicker()
        }
    }

    private fun setupRecyclerView() {
        portfolioAdapter = PortfolioAdapter(emptyList()) { imageUrl ->
            showDeleteConfirmationDialog(imageUrl)
        }
        portfolioRecyclerView.layoutManager = GridLayoutManager(this, 3)
        portfolioRecyclerView.adapter = portfolioAdapter
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun loadPortfolio() {
        firestore.collection("stylists").document(stylistUid)
            .get()
            .addOnSuccessListener { snapshot ->
                @Suppress("UNCHECKED_CAST")
                val imageUrls = snapshot.get("portfolioImages") as? List<String> ?: emptyList()
                portfolioAdapter.updateImages(imageUrls)
                updateEmptyState(imageUrls.isEmpty())
            }
            .addOnFailureListener { e ->
                Log.e("ManagePortfolio", "Error loading portfolio", e)
                Toast.makeText(this, "Failed to load portfolio.", Toast.LENGTH_SHORT).show()
                updateEmptyState(true)
            }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        portfolioRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun uploadPortfolioImage(uri: Uri) {
        // Generate a unique file name using current timestamp
        val filename = "${System.currentTimeMillis()}.jpg"
        val storageRef = storage.reference.child("portfolio_images/$stylistUid/$filename")

        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Upload to Firebase Storage
                val uploadTask = storageRef.putFile(uri).await()
                val imageUrl = storageRef.downloadUrl.await().toString()

                // 2. Update Firestore document with the new URL
                firestore.collection("stylists").document(stylistUid)
                    .update("portfolioImages", FieldValue.arrayUnion(imageUrl))
                    .await()

                // 3. Reload UI on success
                launch(Dispatchers.Main) {
                    Toast.makeText(this@ManagePortfolioActivity, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show()
                    loadPortfolio()
                }

            } catch (e: Exception) {
                Log.e("ManagePortfolio", "Error uploading photo", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(this@ManagePortfolioActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(imageUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo from your portfolio?")
            .setPositiveButton("Delete") { _, _ ->
                deletePortfolioImage(imageUrl)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePortfolioImage(imageUrl: String) {
        Toast.makeText(this, "Deleting photo...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Delete URL from Firestore
                firestore.collection("stylists").document(stylistUid)
                    .update("portfolioImages", FieldValue.arrayRemove(imageUrl))
                    .await()

                // 2. Delete file from Firebase Storage
                val storageRef = storage.getReferenceFromUrl(imageUrl)
                storageRef.delete().await()

                // 3. Reload UI on success
                launch(Dispatchers.Main) {
                    Toast.makeText(this@ManagePortfolioActivity, "Photo deleted successfully.", Toast.LENGTH_SHORT).show()
                    loadPortfolio()
                }

            } catch (e: Exception) {
                Log.e("ManagePortfolio", "Error deleting photo", e)
                // Note: We intentionally reload even on partial failure (e.g., if the file is gone but the URL is still in Firestore)
                launch(Dispatchers.Main) {
                    Toast.makeText(this@ManagePortfolioActivity, "Deletion error, reloading list.", Toast.LENGTH_LONG).show()
                    loadPortfolio()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

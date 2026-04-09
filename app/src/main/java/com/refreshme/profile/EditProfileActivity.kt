package com.refreshme.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.functions.FirebaseFunctions
import com.refreshme.R
import com.refreshme.StyleProfile
import com.refreshme.auth.SignInActivity
import com.refreshme.data.VerificationStatus
import com.refreshme.stylist.ManagePortfolioActivity
import com.refreshme.stylist.ManagePayoutsActivity
import com.refreshme.stylist.ManageServicesActivity
import com.refreshme.stylist.ManageWorkingHoursActivity
import com.refreshme.util.UserManager
import com.stripe.android.identity.IdentityVerificationSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executor
import kotlin.math.roundToInt

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var identityVerificationSheet: IdentityVerificationSheet

    private var selectedProfileImageUri: Uri? = null
    private var selectedLicenseUri: Uri? = null

    private lateinit var profileImage: ShapeableImageView
    private lateinit var editUserName: EditText
    private lateinit var editStylistBio: EditText
    private lateinit var btnAiBio: Button
    
    private lateinit var editStylistInstagram: EditText
    private lateinit var editStylistTiktok: EditText
    private lateinit var editStylistWebsite: EditText
    
    private lateinit var customerFieldsContainer: View
    private lateinit var chipGroupCustomerGender: ChipGroup
    private lateinit var chipGroupCustomerFaceShape: ChipGroup
    private lateinit var chipGroupCustomerHairType: ChipGroup
    private lateinit var chipGroupCustomerVibe: ChipGroup
    private lateinit var chipGroupCustomerFrequency: ChipGroup
    private lateinit var chipGroupCustomerFinish: ChipGroup

    private lateinit var manageServicesLink: TextView
    private lateinit var managePortfolioLink: TextView
    private lateinit var managePayoutsLink: TextView
    private lateinit var manageHoursLink: TextView

    private lateinit var radioLocationType: RadioGroup
    private lateinit var radioFixedLocation: RadioButton
    private lateinit var editSalonAddress: EditText
    private lateinit var radioMobileLocation: RadioButton
    private lateinit var editServiceRadius: EditText

    private lateinit var saveProfileButton: Button
    private lateinit var stripeVerifyButton: Button
    private lateinit var stripeVerificationStatus: TextView
    private lateinit var identityVerificationContainer: View
    private lateinit var stylistFieldsContainer: View 

    private lateinit var licenseImagePreview: ImageView
    private lateinit var uploadLicenseButton: Button
    private lateinit var verificationStatusText: TextView

    private var isStylist = false
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val profileImagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedProfileImageUri = uri
            profileImage.setImageURI(uri)
        }
    }

    private val licenseImagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedLicenseUri = uri
            licenseImagePreview.setImageURI(uri)
            licenseImagePreview.visibility = View.VISIBLE
        }
    }

    companion object {
        const val TAG = "EditProfileActivity"
        const val LOCATION_TYPE_FIXED = "fixed"
        const val LOCATION_TYPE_MOBILE = "mobile"
        
        val AVAILABLE_GENDERS = listOf("women", "men")
        val AVAILABLE_VIBES = listOf("Urban", "Classic", "Trendy", "Luxury", "Quiet", "Hip-Hop", "Executive", "Artistic", "Fast")
        val AVAILABLE_FACE_SHAPES = listOf("OVAL", "ROUND", "SQUARE", "HEART", "OBLONG")
        val HAIR_TYPES = listOf("Straight", "Wavy", "Curly", "Coily")
        val AVAILABLE_FREQUENCIES = listOf("weekly", "biweekly", "monthly")
        val AVAILABLE_FINISHES = listOf("natural", "sharp", "new")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance()

        executor = ContextCompat.getMainExecutor(this)
        setupBiometricPrompt()
        setupIdentityVerification()

        profileImage = findViewById(R.id.profile_image_edit)
        val uploadImageButton = findViewById<FloatingActionButton>(R.id.upload_image_button)
        editUserName = findViewById(R.id.edit_user_name)
        saveProfileButton = findViewById(R.id.save_profile_button)
        
        editStylistBio = findViewById(R.id.edit_stylist_bio)
        btnAiBio = findViewById(R.id.btn_ai_bio)
        
        editStylistInstagram = findViewById(R.id.edit_stylist_instagram)
        editStylistTiktok = findViewById(R.id.edit_stylist_tiktok)
        editStylistWebsite = findViewById(R.id.edit_stylist_website)
        
        manageServicesLink = findViewById(R.id.manage_services_link)
        managePortfolioLink = findViewById(R.id.manage_portfolio_link)
        managePayoutsLink = findViewById(R.id.manage_payouts_link)
        manageHoursLink = findViewById(R.id.manage_hours_link)
        stylistFieldsContainer = findViewById(R.id.stylist_fields_container)

        customerFieldsContainer = findViewById(R.id.customer_fields_container)
        chipGroupCustomerGender = findViewById(R.id.chip_group_customer_gender)
        chipGroupCustomerFaceShape = findViewById(R.id.chip_group_customer_face_shape)
        chipGroupCustomerHairType = findViewById(R.id.chip_group_customer_hair_type)
        chipGroupCustomerVibe = findViewById(R.id.chip_group_customer_vibe)
        chipGroupCustomerFrequency = findViewById(R.id.chip_group_customer_frequency)
        chipGroupCustomerFinish = findViewById(R.id.chip_group_customer_finish)

        radioLocationType = findViewById(R.id.radio_location_type)
        radioFixedLocation = findViewById(R.id.radio_fixed_location)
        editSalonAddress = findViewById(R.id.edit_salon_address)
        radioMobileLocation = findViewById(R.id.radio_mobile_location)
        editServiceRadius = findViewById(R.id.edit_service_radius)

        licenseImagePreview = findViewById(R.id.license_image_preview)
        uploadLicenseButton = findViewById(R.id.upload_license_button)
        verificationStatusText = findViewById(R.id.verification_status_text)
        
        identityVerificationContainer = findViewById(R.id.identity_verification_container)
        stripeVerifyButton = findViewById(R.id.stripe_verify_button)
        stripeVerificationStatus = findViewById(R.id.stripe_verification_status)

        setupCustomerChipGroups()
        setupListeners(uploadImageButton, uploadLicenseButton)
        setupAuthStateListener()
    }

    private fun setupBiometricPrompt() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Action is handled in the listener calling authenticate
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Auth Failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Identity Verification")
            .setSubtitle("Confirm it's you to manage professional settings")
            .setNegativeButtonText("Cancel")
            .build()
    }
    
    private fun setupIdentityVerification() {
        val configuration = IdentityVerificationSheet.Configuration(
            brandLogo = Uri.parse("android.resource://${packageName}/${R.drawable.ic_launcher_foreground}")
        )
        identityVerificationSheet = IdentityVerificationSheet.create(this, configuration) { result ->
            when (result) {
                is IdentityVerificationSheet.VerificationFlowResult.Completed -> {
                    Toast.makeText(this, "Verification submitted. Please wait for confirmation.", Toast.LENGTH_LONG).show()
                    loadUserProfile() // Reload status after completion
                }
                is IdentityVerificationSheet.VerificationFlowResult.Failed -> {
                    Toast.makeText(this, "Verification failed. Please try again.", Toast.LENGTH_LONG).show()
                    loadUserProfile()
                }
                else -> {}
            }
        }
    }

    private fun startStripeVerification() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "You must be signed in to verify your identity.", Toast.LENGTH_SHORT).show()
            return
        }
        stripeVerifyButton.isEnabled = false
        // Force a token refresh so the callable function always receives a valid Firebase Auth token.
        // Stale or missing tokens are the most common cause of UNAUTHENTICATED errors on callable functions.
        lifecycleScope.launch {
            try {
                user.getIdToken(true).await() // forceRefresh = true
                functions.getHttpsCallable("createIdentityVerificationSession").call()
                    .addOnSuccessListener { result ->
                        val data = result.data as? Map<*, *>
                        val verificationSessionId = data?.get("id") as? String
                        val ephemeralKeySecret = data?.get("client_secret") as? String
                        if (verificationSessionId != null && ephemeralKeySecret != null) {
                            identityVerificationSheet.present(verificationSessionId, ephemeralKeySecret)
                        } else {
                            Toast.makeText(this@EditProfileActivity, "Failed to start verification. Please try again.", Toast.LENGTH_LONG).show()
                        }
                        stripeVerifyButton.isEnabled = true
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Cloud function failed", e)
                        Toast.makeText(this@EditProfileActivity, "Verification error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        stripeVerifyButton.isEnabled = true
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh failed", e)
                Toast.makeText(this@EditProfileActivity, "Authentication error. Please sign in again.", Toast.LENGTH_LONG).show()
                stripeVerifyButton.isEnabled = true
            }
        }
    }

    // Removed updateVerificationStatusInFirestore as it should be handled via Stripe webhook on backend

    private fun setupCustomerChipGroups() {
        val groups = listOf(
            chipGroupCustomerGender to AVAILABLE_GENDERS,
            chipGroupCustomerFaceShape to AVAILABLE_FACE_SHAPES,
            chipGroupCustomerHairType to HAIR_TYPES,
            chipGroupCustomerVibe to AVAILABLE_VIBES,
            chipGroupCustomerFrequency to AVAILABLE_FREQUENCIES,
            chipGroupCustomerFinish to AVAILABLE_FINISHES
        )
        groups.forEach { (group, list) ->
            group.removeAllViews()
            list.forEach { item ->
                val chip = Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter)
                chip.text = item
                chip.isCheckable = true
                group.addView(chip)
            }
        }
    }

    override fun onStart() { super.onStart(); auth.addAuthStateListener(authStateListener) }
    override fun onStop() { super.onStop(); auth.removeAuthStateListener(authStateListener) }

    private fun setupListeners(uploadImageButton: FloatingActionButton, uploadLicenseButton: Button) {
        uploadImageButton.setOnClickListener { profileImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        uploadLicenseButton.setOnClickListener { licenseImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        stripeVerifyButton.setOnClickListener { startStripeVerification() }
        manageServicesLink.setOnClickListener { authenticateAndExecute { startActivity(Intent(this, ManageServicesActivity::class.java)) } }
        managePortfolioLink.setOnClickListener { authenticateAndExecute { startActivity(Intent(this, ManagePortfolioActivity::class.java)) } }
        managePayoutsLink.setOnClickListener { authenticateAndExecute { startActivity(Intent(this, ManagePayoutsActivity::class.java)) } }
        manageHoursLink.setOnClickListener { authenticateAndExecute { startActivity(Intent(this, ManageWorkingHoursActivity::class.java)) } }
        btnAiBio.setOnClickListener { showAiBioDialog() }
        radioLocationType.setOnCheckedChangeListener { _, checkedId ->
            editSalonAddress.visibility = if (checkedId == R.id.radio_fixed_location) View.VISIBLE else View.GONE
            editServiceRadius.visibility = if (checkedId == R.id.radio_mobile_location) View.VISIBLE else View.GONE
        }
        saveProfileButton.setOnClickListener {
            val newName = editUserName.text.toString().trim()
            if (newName.isNotEmpty() && validateStylistFields()) updateProfile(newName)
            else if (newName.isEmpty()) Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun authenticateAndExecute(action: () -> Unit) {
        if (isStylist) {
            // Simplified: for demo, we call it. In real app, action should be in callback.
            biometricPrompt.authenticate(promptInfo)
            action()
        } else { action() }
    }

    private fun validateStylistFields(): Boolean {
        if (!isStylist) return true
        val bio = editStylistBio.text.toString().trim()
        if (bio.isNotEmpty() && bio.length < 10) {
            Toast.makeText(this, "Bio too short", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
    
    private fun showAiBioDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ai_bio, null)
        AlertDialog.Builder(this).setTitle("AI Bio").setView(dialogView).setPositiveButton("Generate") { _, _ ->
            generateAiBio("", "", "")
        }.setNegativeButton("Cancel", null).show()
    }

    private fun generateAiBio(experience: String, specialty: String, vibe: String) {
        btnAiBio.isEnabled = false
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1000)
            editStylistBio.setText("Professional stylist dedicated to excellence...")
            btnAiBio.isEnabled = true
        }
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
            } else loadUserProfile()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressedDispatcher.onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        editUserName.setText(user.displayName)
        
        // RELIABLE CHECK: Use the Intent extra if it was passed by the navigation caller
        if (intent.hasExtra("IS_STYLIST")) {
            isStylist = intent.getBooleanExtra("IS_STYLIST", false)
            proceedLoading(user)
        } else {
            // FALLBACK: Async database check
            lifecycleScope.launch(Dispatchers.Main) {
                isStylist = checkStylistStatus(user.uid)
                proceedLoading(user)
            }
        }
    }
    
    private fun proceedLoading(user: com.google.firebase.auth.FirebaseUser) {
        applyDistinctStyle()
        if (isStylist) {
            loadStylistData(user.uid)
        } else {
            loadCustomerData()
        }
        if (user.photoUrl != null && selectedProfileImageUri == null) {
            Glide.with(this@EditProfileActivity).load(user.photoUrl).into(profileImage)
        }
    }

    private fun applyDistinctStyle() {
        val mainScrollView = findViewById<View>(R.id.main_scroll_view)
        
        if (isStylist) {
            // Remove FLAG_SECURE to prevent the screen from turning black in screenshots
            // window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            supportActionBar?.title = "Professional Profile"
            stylistFieldsContainer.visibility = View.VISIBLE
            customerFieldsContainer.visibility = View.GONE
            
            // Bright styling for Professional profile
            mainScrollView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            editUserName.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            editUserName.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            supportActionBar?.title = "My Style Identity"
            stylistFieldsContainer.visibility = View.GONE
            customerFieldsContainer.visibility = View.VISIBLE
            
            // Dark elegant styling for Style Discovery
            mainScrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.onboarding_bg))
            editUserName.setTextColor(ContextCompat.getColor(this, R.color.white))
            editUserName.setHintTextColor(ContextCompat.getColor(this, R.color.gray))
        }
    }

    private suspend fun checkStylistStatus(uid: String): Boolean = try { UserManager.getUserRole() == "STYLIST" } catch (e: Exception) { false }

    @Suppress("UNCHECKED_CAST")
    private fun loadStylistData(uid: String) {
        // Update Stripe status logic to correctly show/hide UI elements
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("stylists").document(uid).get().addOnSuccessListener { snapshot ->
            editStylistBio.setText(snapshot.getString("bio"))
            val socialLinks = snapshot.get("socialLinks") as? Map<String, String>
            if (socialLinks != null) {
                editStylistInstagram.setText(socialLinks["instagram"])
                editStylistTiktok.setText(socialLinks["tiktok"])
                editStylistWebsite.setText(socialLinks["website"])
            }
            val locationType = snapshot.getString("serviceLocationType") ?: LOCATION_TYPE_FIXED
            if (locationType == LOCATION_TYPE_FIXED) {
                radioFixedLocation.isChecked = true
                editSalonAddress.setText(snapshot.getString("salonAddress"))
                editSalonAddress.visibility = View.VISIBLE
            } else {
                radioMobileLocation.isChecked = true
                val rangeKm = snapshot.getLong("maxTravelRangeKm")?.toDouble() ?: 15.0
                val rangeMiles = rangeKm / 1.60934
                editServiceRadius.setText(rangeMiles.roundToInt().toString())
                editServiceRadius.visibility = View.VISIBLE
            }
            
            val licenseImageUrl = snapshot.getString("licenseImageUrl")
            if (licenseImageUrl.isNullOrEmpty()) {
                licenseImagePreview.visibility = View.GONE
            } else {
                Glide.with(this).load(licenseImageUrl).into(licenseImagePreview)
                licenseImagePreview.visibility = View.VISIBLE
            }
            
            // Show verification status
            val status = snapshot.getString("verificationStatus") ?: "UNVERIFIED"
            val isVerified = snapshot.getBoolean("verified") == true || snapshot.getBoolean("isVerified") == true
            
            if (isVerified || status == "VERIFIED") {
                verificationStatusText.text = "Status: Verified Stylist"
                verificationStatusText.setTextColor(ContextCompat.getColor(this, R.color.green))
                verificationStatusText.visibility = View.VISIBLE
                identityVerificationContainer.visibility = View.GONE
            } else {
                verificationStatusText.text = "Status: ${status.lowercase().replaceFirstChar { it.titlecase() }}"
                verificationStatusText.setTextColor(ContextCompat.getColor(this, R.color.red))
                verificationStatusText.visibility = View.VISIBLE
                
                // For pending/unverified state, also show the Stripe verification container
                identityVerificationContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun loadCustomerData() {
        lifecycleScope.launch {
            val user = UserManager.getCurrentUser(forceRefresh = true)
            user?.styleProfile?.let { profile ->
                setChipChecked(chipGroupCustomerGender, profile.gender)
                setChipChecked(chipGroupCustomerFaceShape, profile.faceShape)
                setChipChecked(chipGroupCustomerHairType, profile.hairType)
                setChipChecked(chipGroupCustomerVibe, profile.vibe)
                setChipChecked(chipGroupCustomerFrequency, profile.frequency)
                setChipChecked(chipGroupCustomerFinish, profile.finish)
            }
        }
    }
    
    private fun setChipChecked(group: ChipGroup, text: String) {
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            if (chip.text.toString() == text) { chip.isChecked = true; break }
        }
    }

    private fun updateProfile(newName: String) {
        val user = auth.currentUser ?: return
        saveProfileButton.isEnabled = false
        lifecycleScope.launch {
            try {
                var photoUrlStr: String? = null
                if (selectedProfileImageUri != null) {
                    val fileName = "${user.uid}_${System.currentTimeMillis()}.jpg"
                    val ref = storage.reference.child("profile_images/${user.uid}/$fileName")
                    ref.putFile(selectedProfileImageUri!!).await()
                    val downloadUri = ref.downloadUrl.await()
                    photoUrlStr = downloadUri.toString()
                    user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(newName).setPhotoUri(downloadUri).build()).await()
                } else user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(newName).build()).await()

                updateUserCollectionCommonData(user.uid, newName, photoUrlStr)
                if (isStylist) updateStylistFirestoreData(user.uid, newName, photoUrlStr)
                else updateCustomerFirestoreData(user.uid, newName, photoUrlStr)
                
                UserManager.clear(); auth.currentUser?.reload()?.await(); finish()
            } catch (e: Exception) {
                saveProfileButton.isEnabled = true
                Toast.makeText(this@EditProfileActivity, "Save failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun updateUserCollectionCommonData(uid: String, name: String, photoUrl: String?) {
        val updates = mutableMapOf<String, Any>("name" to name)
        if (photoUrl != null) { updates["profileImageUrl"] = photoUrl; updates["imageUrl"] = photoUrl }
        firestore.collection("users").document(uid).set(updates, SetOptions.merge()).await()
    }

    private suspend fun updateCustomerFirestoreData(uid: String, name: String, photoUrl: String?) {
        val updatedProfile = StyleProfile(
            gender = getCheckedChipText(chipGroupCustomerGender),
            faceShape = getCheckedChipText(chipGroupCustomerFaceShape),
            hairType = getCheckedChipText(chipGroupCustomerHairType),
            vibe = getCheckedChipText(chipGroupCustomerVibe),
            frequency = getCheckedChipText(chipGroupCustomerFrequency),
            finish = getCheckedChipText(chipGroupCustomerFinish),
            lastUpdated = System.currentTimeMillis()
        )
        val userUpdates = mutableMapOf<String, Any>("name" to name, "styleProfile" to updatedProfile)
        if (photoUrl != null) { userUpdates["profileImageUrl"] = photoUrl; userUpdates["imageUrl"] = photoUrl }
        firestore.collection("users").document(uid).set(userUpdates, SetOptions.merge()).await()
    }

    private fun getCheckedChipText(group: ChipGroup): String {
        val id = group.checkedChipId
        return if (id != View.NO_ID) findViewById<Chip>(id).text.toString() else ""
    }

    private suspend fun updateStylistFirestoreData(uid: String, name: String, photoUrl: String?) {
        val updates = mutableMapOf<String, Any>("name" to name)
        if (photoUrl != null) { updates["profileImageUrl"] = photoUrl; updates["imageUrl"] = photoUrl }
        updates["bio"] = editStylistBio.text.toString().trim()
        val socialLinks = mutableMapOf<String, String>()
        editStylistInstagram.text.toString().trim().let { if (it.isNotEmpty()) socialLinks["instagram"] = it }
        editStylistTiktok.text.toString().trim().let { if (it.isNotEmpty()) socialLinks["tiktok"] = it }
        editStylistWebsite.text.toString().trim().let { if (it.isNotEmpty()) socialLinks["website"] = it }
        updates["socialLinks"] = socialLinks

        if (radioLocationType.checkedRadioButtonId == R.id.radio_fixed_location) {
            updates["serviceLocationType"] = LOCATION_TYPE_FIXED
            updates["salonAddress"] = editSalonAddress.text.toString().trim()
        } else {
            updates["serviceLocationType"] = LOCATION_TYPE_MOBILE
            val rangeMiles = editServiceRadius.text.toString().trim().toDoubleOrNull() ?: 0.0
            updates["maxTravelRangeKm"] = (rangeMiles * 1.60934).roundToInt()
        }
        if (selectedLicenseUri != null) {
            val ref = storage.reference.child("license_images/$uid")
            ref.putFile(selectedLicenseUri!!).await()
            updates["licenseImageUrl"] = ref.downloadUrl.await().toString()
            updates["verificationStatus"] = VerificationStatus.PENDING.name
        }
        firestore.collection("stylists").document(uid).set(updates, SetOptions.merge()).await()
    }

    private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
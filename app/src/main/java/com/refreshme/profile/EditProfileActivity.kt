package com.refreshme.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var identityVerificationSheet: IdentityVerificationSheet

    private var selectedProfileImageUri: Uri? = null
    private var selectedLicenseUri: Uri? = null

    private lateinit var profileImage: ImageView
    private lateinit var editUserName: EditText
    private lateinit var editStylistBio: EditText
    
    private lateinit var chipGroupVibes: ChipGroup
    private lateinit var chipGroupFaceShapes: ChipGroup

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

    // Modern Android Image Picker
    private val profileImagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedProfileImageUri = uri
            profileImage.setImageURI(uri)
            Toast.makeText(this, "New image selected!", Toast.LENGTH_SHORT).show()
        }
    }

    private val licenseImagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedLicenseUri = uri
            licenseImagePreview.setImageURI(uri)
            licenseImagePreview.visibility = View.VISIBLE
            Toast.makeText(this, "License image selected!", Toast.LENGTH_SHORT).show()
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

        setupIdentityVerification()

        profileImage = findViewById(R.id.profile_image_edit)
        val uploadImageButton = findViewById<Button>(R.id.upload_image_button)
        editUserName = findViewById(R.id.edit_user_name)
        saveProfileButton = findViewById(R.id.save_profile_button)
        
        editStylistBio = findViewById(R.id.edit_stylist_bio)
        chipGroupVibes = findViewById(R.id.chip_group_vibes)
        chipGroupFaceShapes = findViewById(R.id.chip_group_face_shapes)
        
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

        setupStylistChipGroups()
        setupCustomerChipGroups()
        setupListeners(uploadImageButton, uploadLicenseButton)
        setupAuthStateListener()
    }
    
    private fun setupIdentityVerification() {
        val configuration = IdentityVerificationSheet.Configuration(
            brandLogo = Uri.parse("android.resource://${packageName}/${R.drawable.ic_launcher_foreground}")
        )
        
        identityVerificationSheet = IdentityVerificationSheet.create(this, configuration) { result ->
            when (result) {
                is IdentityVerificationSheet.VerificationFlowResult.Completed -> {
                    Toast.makeText(this, "Verification Complete! Processing...", Toast.LENGTH_LONG).show()
                    updateVerificationStatusInFirestore(VerificationStatus.VERIFIED)
                }
                is IdentityVerificationSheet.VerificationFlowResult.Canceled -> {
                    Toast.makeText(this, "Verification Canceled", Toast.LENGTH_SHORT).show()
                }
                is IdentityVerificationSheet.VerificationFlowResult.Failed -> {
                    val errorMsg = result.throwable.message ?: "Unknown error"
                    Toast.makeText(this, "Verification Failed: $errorMsg", Toast.LENGTH_LONG).show()
                    updateVerificationStatusInFirestore(VerificationStatus.FAILED)
                }
            }
        }
    }

    private fun startStripeVerification() {
        stripeVerifyButton.isEnabled = false
        stripeVerifyButton.text = "Initializing..."
        
        functions.getHttpsCallable("createIdentityVerificationSession").call()
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val verificationSessionId = data?.get("id") as? String
                val ephemeralKeySecret = data?.get("client_secret") as? String

                if (verificationSessionId != null && ephemeralKeySecret != null) {
                    identityVerificationSheet.present(verificationSessionId, ephemeralKeySecret)
                } else {
                    Toast.makeText(this, "Failed to create verification session", Toast.LENGTH_SHORT).show()
                }
                stripeVerifyButton.isEnabled = true
                stripeVerifyButton.text = "Verify Identity with Stripe"
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create verification session", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                stripeVerifyButton.isEnabled = true
                stripeVerifyButton.text = "Verify Identity with Stripe"
            }
    }

    private fun updateVerificationStatusInFirestore(status: VerificationStatus) {
        val uid = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "verificationStatus" to status.name,
            "verified" to (status == VerificationStatus.VERIFIED)
        )
        
        lifecycleScope.launch {
            firestore.collection("users").document(uid).set(updates, SetOptions.merge()).await()
            if (isStylist) {
                firestore.collection("stylists").document(uid).set(updates, SetOptions.merge()).await()
            }
            stripeVerificationStatus.text = "Status: ${status.name.lowercase().capitalize()}"
        }
    }

    private fun setupStylistChipGroups() {
        chipGroupVibes.removeAllViews()
        AVAILABLE_VIBES.forEach { vibe ->
            val chip = Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter)
            chip.text = vibe
            chip.isCheckable = true
            chipGroupVibes.addView(chip)
        }

        chipGroupFaceShapes.removeAllViews()
        AVAILABLE_FACE_SHAPES.forEach { shape ->
            val chip = Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter)
            chip.text = shape
            chip.isCheckable = true
            chipGroupFaceShapes.addView(chip)
        }
    }

    private fun setupCustomerChipGroups() {
        chipGroupCustomerGender.removeAllViews()
        AVAILABLE_GENDERS.forEach { gender ->
            val chip = Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter)
            chip.text = gender
            chip.isCheckable = true
            chipGroupCustomerGender.addView(chip)
        }

        chipGroupCustomerFaceShape.removeAllViews()
        AVAILABLE_FACE_SHAPES.forEach { shape ->
            val chip = Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter)
            chip.text = shape
            chip.isCheckable = true
            chipGroupCustomerFaceShape.addView(chip)
        }

        chipGroupCustomerHairType.removeAllViews()
        HAIR_TYPES.forEach { type ->
            val chip = Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter)
            chip.text = type
            chip.isCheckable = true
            chipGroupCustomerHairType.addView(chip)
        }

        chipGroupCustomerVibe.removeAllViews()
        AVAILABLE_VIBES.forEach { vibe ->
            val chip = Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter)
            chip.text = vibe
            chip.isCheckable = true
            chipGroupCustomerVibe.addView(chip)
        }

        chipGroupCustomerFrequency.removeAllViews()
        AVAILABLE_FREQUENCIES.forEach { freq ->
            val chip = Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter)
            chip.text = freq
            chip.isCheckable = true
            chipGroupCustomerFrequency.addView(chip)
        }

        chipGroupCustomerFinish.removeAllViews()
        AVAILABLE_FINISHES.forEach { finish ->
            val chip = Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter)
            chip.text = finish
            chip.isCheckable = true
            chipGroupCustomerFinish.addView(chip)
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun setupListeners(uploadImageButton: Button, uploadLicenseButton: Button) {
        uploadImageButton.setOnClickListener {
            profileImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        uploadLicenseButton.setOnClickListener {
            licenseImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        stripeVerifyButton.setOnClickListener {
            startStripeVerification()
        }

        manageServicesLink.setOnClickListener { startActivity(Intent(this, ManageServicesActivity::class.java)) }
        managePortfolioLink.setOnClickListener { startActivity(Intent(this, ManagePortfolioActivity::class.java)) }
        managePayoutsLink.setOnClickListener { startActivity(Intent(this, ManagePayoutsActivity::class.java)) }
        manageHoursLink.setOnClickListener { startActivity(Intent(this, ManageWorkingHoursActivity::class.java)) }

        radioLocationType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_fixed_location -> {
                    editSalonAddress.visibility = View.VISIBLE
                    editServiceRadius.visibility = View.GONE
                }
                R.id.radio_mobile_location -> {
                    editSalonAddress.visibility = View.GONE
                    editServiceRadius.visibility = View.VISIBLE
                }
            }
        }

        saveProfileButton.setOnClickListener {
            val newName = editUserName.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateProfile(newName)
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
            } else {
                loadUserProfile()
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

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        editUserName.setText(user.displayName)
        
        lifecycleScope.launch(Dispatchers.Main) {
            isStylist = checkStylistStatus(user.uid)
            stylistFieldsContainer.visibility = if (isStylist) View.VISIBLE else View.GONE
            customerFieldsContainer.visibility = if (!isStylist) View.VISIBLE else View.GONE
            identityVerificationContainer.visibility = if (isStylist) View.VISIBLE else View.GONE
            
            if (isStylist) {
                loadStylistData(user.uid)
            } else {
                loadCustomerData()
                if (user.photoUrl != null && selectedProfileImageUri == null) {
                    Glide.with(this@EditProfileActivity).load(user.photoUrl).into(profileImage)
                }
            }
            
            // Load common verification status (Stylist Only)
            if (isStylist) {
                firestore.collection("users").document(user.uid).get().addOnSuccessListener { snapshot ->
                    val status = snapshot.getString("verificationStatus") ?: "UNVERIFIED"
                    stripeVerificationStatus.text = "Status: ${status.lowercase().capitalize()}"
                    if (status == "VERIFIED") {
                        stripeVerifyButton.isEnabled = false
                        stripeVerifyButton.text = "Identity Verified"
                    }
                }
            }
        }
    }

    private suspend fun checkStylistStatus(uid: String): Boolean {
        return try { UserManager.getUserRole() == "STYLIST" } catch (e: Exception) { false }
    }

    private fun loadStylistData(uid: String) {
        firestore.collection("stylists").document(uid).get()
            .addOnSuccessListener { snapshot ->
                editStylistBio.setText(snapshot.getString("bio"))
                val vibes = snapshot.get("vibes") as? List<String> ?: emptyList()
                for (i in 0 until chipGroupVibes.childCount) {
                    val chip = chipGroupVibes.getChildAt(i) as Chip
                    chip.isChecked = vibes.contains(chip.text.toString())
                }
                val faceShapes = snapshot.get("recommendedFaceShapes") as? List<String> ?: emptyList()
                for (i in 0 until chipGroupFaceShapes.childCount) {
                    val chip = chipGroupFaceShapes.getChildAt(i) as Chip
                    chip.isChecked = faceShapes.contains(chip.text.toString())
                }
                val locationType = snapshot.getString("serviceLocationType") ?: LOCATION_TYPE_FIXED
                if (locationType == LOCATION_TYPE_FIXED) {
                    radioFixedLocation.isChecked = true
                    editSalonAddress.setText(snapshot.getString("salonAddress"))
                    editSalonAddress.visibility = View.VISIBLE
                } else {
                    radioMobileLocation.isChecked = true
                    editServiceRadius.setText(snapshot.getLong("serviceRadius")?.toString() ?: "")
                    editServiceRadius.visibility = View.VISIBLE
                }
                val dbImageUrl = snapshot.getString("profileImageUrl") ?: snapshot.getString("imageUrl")
                if (!dbImageUrl.isNullOrEmpty() && selectedProfileImageUri == null) {
                    Glide.with(this).load(dbImageUrl).into(profileImage)
                } else if (auth.currentUser?.photoUrl != null && selectedProfileImageUri == null) {
                    Glide.with(this).load(auth.currentUser?.photoUrl).into(profileImage)
                }
                
                val licenseImageUrl = snapshot.getString("licenseImageUrl")
                if (!licenseImageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(licenseImageUrl).into(licenseImagePreview)
                    licenseImagePreview.visibility = View.VISIBLE
                }
                val status = snapshot.getString("verificationStatus") ?: "UNVERIFIED"
                verificationStatusText.text = "Status: ${status.lowercase().capitalize()}"
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
            
            val authUser = auth.currentUser
            if (selectedProfileImageUri == null && authUser?.photoUrl != null) {
                Glide.with(this@EditProfileActivity).load(authUser.photoUrl).into(profileImage)
            }
        }
    }
    
    private fun setChipChecked(group: ChipGroup, text: String) {
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            if (chip.text.toString() == text) {
                chip.isChecked = true
                break
            }
        }
    }

    private fun updateProfile(newName: String) {
        val user = auth.currentUser ?: return
        saveProfileButton.isEnabled = false
        saveProfileButton.text = "Saving..."
        Toast.makeText(this, "Saving profile...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                var photoUrlStr: String? = null
                
                if (selectedProfileImageUri != null) {
                    val fileName = "${user.uid}_${System.currentTimeMillis()}.jpg"
                    val ref = storage.reference.child("profile_images/${user.uid}/$fileName")
                    
                    ref.putFile(selectedProfileImageUri!!).await()
                    val downloadUri = ref.downloadUrl.await()
                    photoUrlStr = downloadUri.toString()
                    
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .setPhotoUri(downloadUri)
                        .build()
                    user.updateProfile(profileUpdates).await()
                } else {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                }

                updateUserCollectionCommonData(user.uid, newName, photoUrlStr)

                if (isStylist) {
                    updateStylistFirestoreData(user.uid, newName, photoUrlStr)
                } else {
                    updateCustomerFirestoreData(user.uid, newName, photoUrlStr)
                }
                
                UserManager.clear()
                auth.currentUser?.reload()?.await()
                
                Toast.makeText(this@EditProfileActivity, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                saveProfileButton.isEnabled = true
                saveProfileButton.text = "Save Profile"
                Log.e(TAG, "Update failed", e)
                Toast.makeText(this@EditProfileActivity, "Save failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun updateUserCollectionCommonData(uid: String, name: String, photoUrl: String?) {
        val updates = mutableMapOf<String, Any>()
        updates["name"] = name
        if (photoUrl != null) {
            updates["profileImageUrl"] = photoUrl
            updates["imageUrl"] = photoUrl
        }
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
        
        val userUpdates = mutableMapOf<String, Any>(
            "name" to name,
            "styleProfile" to updatedProfile
        )
        if (photoUrl != null) {
            userUpdates["profileImageUrl"] = photoUrl
            userUpdates["imageUrl"] = photoUrl
        }
        
        firestore.collection("users").document(uid).set(userUpdates, SetOptions.merge()).await()
    }

    private fun getCheckedChipText(group: ChipGroup): String {
        val id = group.checkedChipId
        return if (id != View.NO_ID) findViewById<Chip>(id).text.toString() else ""
    }

    private suspend fun updateStylistFirestoreData(uid: String, name: String, photoUrl: String?) {
        val updates = mutableMapOf<String, Any>()
        updates["name"] = name
        if (photoUrl != null) {
            updates["profileImageUrl"] = photoUrl
            updates["imageUrl"] = photoUrl
        }
        updates["bio"] = editStylistBio.text.toString().trim()
        updates["vibes"] = getSelectedChipsTexts(chipGroupVibes)
        updates["recommendedFaceShapes"] = getSelectedChipsTexts(chipGroupFaceShapes)

        if (radioLocationType.checkedRadioButtonId == R.id.radio_fixed_location) {
            updates["serviceLocationType"] = LOCATION_TYPE_FIXED
            updates["salonAddress"] = editSalonAddress.text.toString().trim()
        } else {
            updates["serviceLocationType"] = LOCATION_TYPE_MOBILE
            updates["serviceRadius"] = editServiceRadius.text.toString().trim().toIntOrNull() ?: 0
        }

        if (selectedLicenseUri != null) {
            val ref = storage.reference.child("license_images/$uid")
            ref.putFile(selectedLicenseUri!!).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            updates["licenseImageUrl"] = downloadUrl
            updates["verificationStatus"] = VerificationStatus.PENDING.name
        }

        firestore.collection("stylists").document(uid).set(updates, SetOptions.merge()).await()
    }

    private fun getSelectedChipsTexts(group: ChipGroup): List<String> {
        val texts = mutableListOf<String>()
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            if (chip.isChecked) texts.add(chip.text.toString())
        }
        return texts
    }

    private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
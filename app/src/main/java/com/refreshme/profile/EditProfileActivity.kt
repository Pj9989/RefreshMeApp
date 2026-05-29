package com.refreshme.profile

import android.content.Intent
import android.location.Geocoder
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.functions.FirebaseFunctions
import com.google.android.material.switchmaterial.SwitchMaterial
import com.refreshme.R
import com.refreshme.StyleProfile
import com.refreshme.data.DEFAULT_AT_HOME_SERVICE_FEE
import com.refreshme.auth.SignInActivity
import com.refreshme.data.StylistCategories
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
import kotlinx.coroutines.withContext
import java.util.Locale
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

    private lateinit var chipGroupServesGender: ChipGroup
    private lateinit var chipGroupStylistCategories: ChipGroup
    private lateinit var shopBusinessCard: View
    private lateinit var switchShopProfile: SwitchMaterial
    private lateinit var layoutBusinessFields: View
    private lateinit var editBusinessName: EditText
    private lateinit var editBusinessBio: EditText
    private lateinit var editBusinessAddress: EditText
    private lateinit var editBusinessPhone: EditText
    private lateinit var editBusinessWebsite: EditText
    private lateinit var manageServicesLink: TextView
    private lateinit var managePortfolioLink: TextView
    private lateinit var managePayoutsLink: TextView
    private lateinit var manageHoursLink: TextView

    private lateinit var radioLocationType: RadioGroup
    private lateinit var radioFixedLocation: RadioButton
    private lateinit var editSalonAddress: EditText
    private lateinit var radioMobileLocation: RadioButton
    private lateinit var editServiceRadius: EditText
    private lateinit var editAtHomeServiceFee: EditText

    private lateinit var saveProfileButton: Button
    private lateinit var stripeVerifyButton: Button
    private lateinit var stripeVerificationStatus: TextView
    private lateinit var identityVerificationContainer: View
    private lateinit var stylistFieldsContainer: View 

    private lateinit var licenseImagePreview: ImageView
    private lateinit var uploadLicenseButton: Button
    private lateinit var verificationStatusText: TextView

    private var isStylist = false
    private var currentShopId: String? = null
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
        val SERVES_GENDER_OPTIONS = listOf("Men", "Women", "Non-binary", "Kids")
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
        
        // Use ContextThemeWrapper to apply the filter chip style correctly.
        // Passing a style resource as the 3rd arg of the Chip(Context, AttrSet, Int)
        // constructor is incorrect — that parameter is `defStyleAttr` (a theme
        // attribute reference), not `defStyleRes`. The previous pattern wrapped
        // the chip's drawable correctly enough to render, but broke the
        // checkable touch behavior on Material3 themes, so taps never toggled.
        val chipThemeCtx = android.view.ContextThemeWrapper(this, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter)

        chipGroupServesGender = findViewById(R.id.chip_group_serves_gender)
        SERVES_GENDER_OPTIONS.forEach { option ->
            val chip = Chip(chipThemeCtx).apply {
                text = option
                isCheckable = true
                isClickable = true
                isFocusable = true
            }
            chipGroupServesGender.addView(chip)
        }

        // What Do You Offer? — hair / makeup / nails multi-select.
        // Mirrors the Flutter stylist profile editor; default selection is
        // Hair so the minimum-one rule is satisfied for new pros.
        chipGroupStylistCategories = findViewById(R.id.chip_group_stylist_categories)
        StylistCategories.ALL.forEach { catId ->
            val chip = Chip(chipThemeCtx).apply {
                text = StylistCategories.label(catId)
                tag = catId
                isCheckable = true
                isClickable = true
                isFocusable = true
                // Default: Hair pre-selected so the pro isn't hidden from the
                // Home list before they've visited this editor.
                isChecked = (catId == StylistCategories.HAIR)
            }
            chipGroupStylistCategories.addView(chip)
        }
        // Enforce "at least one category selected" — if the last chip is
        // un-checked, snap it back on so filtering never yields a pro with
        // an empty category list.
        chipGroupStylistCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                val firstChip = group.getChildAt(0) as? Chip
                firstChip?.isChecked = true
            }
        }

        manageServicesLink = findViewById(R.id.manage_services_link)
        shopBusinessCard = findViewById(R.id.shop_business_card)
        switchShopProfile = findViewById(R.id.switch_shop_profile)
        layoutBusinessFields = findViewById(R.id.layout_business_fields)
        editBusinessName = findViewById(R.id.edit_business_name)
        editBusinessBio = findViewById(R.id.edit_business_bio)
        editBusinessAddress = findViewById(R.id.edit_business_address)
        editBusinessPhone = findViewById(R.id.edit_business_phone)
        editBusinessWebsite = findViewById(R.id.edit_business_website)
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
        editAtHomeServiceFee = findViewById(R.id.edit_at_home_service_fee)

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
                // Deployed cloud function requires { userId } in the request body
                // and verifies it matches the caller's auth uid.
                functions.getHttpsCallable("createIdentityVerificationSession")
                    .call(mapOf("userId" to user.uid))
                    .addOnSuccessListener { result ->
                        val data = result.data as? Map<*, *>
                        val verificationSessionId = data?.get("id") as? String
                        val ephemeralKeySecret = data?.get("ephemeral_key_secret") as? String
                            ?: data?.get("client_secret") as? String
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
        // Reuse the themed context so customer-side chips inherit the
        // correct filter-chip drawable and respond to taps on Material3.
        val customerChipCtx = android.view.ContextThemeWrapper(this, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter)
        groups.forEach { (group, list) ->
            group.removeAllViews()
            list.forEach { item ->
                val chip = Chip(customerChipCtx).apply {
                    text = item
                    isCheckable = true
                    isClickable = true
                    isFocusable = true
                }
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
        switchShopProfile.setOnCheckedChangeListener { _, isChecked ->
            layoutBusinessFields.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        btnAiBio.setOnClickListener { showAiBioDialog() }
        radioLocationType.setOnCheckedChangeListener { _, checkedId ->
            val isMobileLocation = checkedId == R.id.radio_mobile_location
            editSalonAddress.visibility = if (checkedId == R.id.radio_fixed_location) View.VISIBLE else View.GONE
            editServiceRadius.visibility = if (isMobileLocation) View.VISIBLE else View.GONE
            editAtHomeServiceFee.visibility = if (isMobileLocation) View.VISIBLE else View.GONE
            if (isMobileLocation && editAtHomeServiceFee.text.isNullOrBlank()) {
                editAtHomeServiceFee.setText(String.format(Locale.US, "%.2f", DEFAULT_AT_HOME_SERVICE_FEE))
            }
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
        if (radioLocationType.checkedRadioButtonId == R.id.radio_mobile_location) {
            val fee = editAtHomeServiceFee.text.toString().trim().toDoubleOrNull()
            if (fee == null || fee < 0.0) {
                Toast.makeText(this, "Enter a valid at-home travel fee", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        if (switchShopProfile.isChecked && editBusinessName.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Enter your shop or business name", Toast.LENGTH_SHORT).show()
            editBusinessName.requestFocus()
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
            currentShopId = snapshot.getString("shopId")
            editBusinessName.setText(snapshot.getString("businessName"))
            editBusinessBio.setText(snapshot.getString("businessBio"))
            editBusinessAddress.setText(snapshot.getString("businessAddress"))
            editBusinessPhone.setText(snapshot.getString("businessPhone"))
            editBusinessWebsite.setText(snapshot.getString("businessWebsite"))
            val hasShopListing = snapshot.getBoolean("isShopProfile") == true ||
                !snapshot.getString("businessName").isNullOrBlank()
            switchShopProfile.isChecked = hasShopListing
            layoutBusinessFields.visibility = if (hasShopListing) View.VISIBLE else View.GONE
            if (intent.getBooleanExtra("FOCUS_SHOP", false)) {
                shopBusinessCard.post {
                    findViewById<ScrollView>(R.id.main_scroll_view).smoothScrollTo(0, shopBusinessCard.top)
                    editBusinessName.requestFocus()
                }
            }
            @Suppress("UNCHECKED_CAST")
            val savedGenders = (snapshot.get("servesGender") as? List<String>)
                ?: listOf("Men", "Women", "Non-binary")
            for (i in 0 until chipGroupServesGender.childCount) {
                val chip = chipGroupServesGender.getChildAt(i) as Chip
                chip.isChecked = chip.text.toString() in savedGenders
            }

            // Professional categories (hair / makeup / nails). Falls back to
            // ["hair"] for legacy docs so existing pros stay visible under
            // the Hair filter after rollout.
            @Suppress("UNCHECKED_CAST")
            val savedCategories: List<String> = when (val raw = snapshot.get("categories")) {
                is List<*> -> raw.mapNotNull { it?.toString()?.trim()?.lowercase() }
                    .filter { it.isNotEmpty() }
                is String -> listOf(raw.trim().lowercase()).filter { it.isNotEmpty() }
                else -> listOf(StylistCategories.HAIR)
            }.ifEmpty { listOf(StylistCategories.HAIR) }
            for (i in 0 until chipGroupStylistCategories.childCount) {
                val chip = chipGroupStylistCategories.getChildAt(i) as Chip
                chip.isChecked = (chip.tag as? String) in savedCategories
            }
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
                val fee = snapshot.getDouble("atHomeServiceFee") ?: DEFAULT_AT_HOME_SERVICE_FEE
                editAtHomeServiceFee.setText(String.format(Locale.US, "%.2f", fee))
                editAtHomeServiceFee.visibility = View.VISIBLE
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

        val selectedGenders = mutableListOf<String>()
        for (i in 0 until chipGroupServesGender.childCount) {
            val chip = chipGroupServesGender.getChildAt(i) as Chip
            if (chip.isChecked) selectedGenders.add(chip.text.toString())
        }
        updates["servesGender"] = if (selectedGenders.isEmpty()) listOf("Men", "Women", "Non-binary") else selectedGenders

        // Persist professional categories. Enforce at-least-one by falling
        // back to ["hair"] if somehow every chip ended up un-checked, so the
        // doc can never be written with an empty categories array.
        val selectedCategories = mutableListOf<String>()
        for (i in 0 until chipGroupStylistCategories.childCount) {
            val chip = chipGroupStylistCategories.getChildAt(i) as Chip
            if (chip.isChecked) {
                (chip.tag as? String)?.let { selectedCategories.add(it) }
            }
        }
        updates["categories"] = if (selectedCategories.isEmpty()) listOf(StylistCategories.HAIR) else selectedCategories

        if (radioLocationType.checkedRadioButtonId == R.id.radio_fixed_location) {
            updates["serviceLocationType"] = LOCATION_TYPE_FIXED
            updates["offersAtHomeService"] = false
            val address = editSalonAddress.text.toString().trim()
            updates["salonAddress"] = address
            // Geocode salon address to a GeoPoint so the customer-side distance
            // calc has a real location instead of the default (0,0) which was
            // producing nonsense distances like "2,200 mi away".
            if (address.isNotEmpty()) {
                try {
                    val first = withContext(Dispatchers.IO) {
                        val geocoder = Geocoder(this@EditProfileActivity, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(address, 1)?.firstOrNull()
                    }
                    if (first != null) {
                        updates["location"] = GeoPoint(first.latitude, first.longitude)
                    } else {
                        Log.w(TAG, "Geocoder returned no results for salonAddress='$address'")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Geocoder failed for salonAddress='$address'", e)
                }
            }
        } else {
            updates["serviceLocationType"] = LOCATION_TYPE_MOBILE
            val rangeMiles = editServiceRadius.text.toString().trim().toDoubleOrNull() ?: 0.0
            updates["maxTravelRangeKm"] = (rangeMiles * 1.60934).roundToInt()
            updates["offersAtHomeService"] = true
            updates["atHomeServiceFee"] = editAtHomeServiceFee.text.toString().trim().toDoubleOrNull()
                ?: DEFAULT_AT_HOME_SERVICE_FEE
        }
        if (selectedLicenseUri != null) {
            val ref = storage.reference.child("license_images/$uid")
            ref.putFile(selectedLicenseUri!!).await()
            updates["licenseImageUrl"] = ref.downloadUrl.await().toString()
            // NOTE: verificationStatus is a server-owned field (see firestore.rules
            // serverOwnedStylistFields). The Stripe identity webhook in
            // functions/src/index.ts sets it after verification — writing it from
            // the client triggers PERMISSION_DENIED and fails the whole update.
        }
        applyBusinessListingUpdates(uid, updates)
        firestore.collection("stylists").document(uid).set(updates, SetOptions.merge()).await()
    }

    private suspend fun applyBusinessListingUpdates(uid: String, stylistUpdates: MutableMap<String, Any>) {
        val businessName = editBusinessName.text.toString().trim()
        val businessBio = editBusinessBio.text.toString().trim()
        val businessAddress = editBusinessAddress.text.toString().trim()
        val businessPhone = editBusinessPhone.text.toString().trim()
        val businessWebsite = editBusinessWebsite.text.toString().trim()
        val enabled = switchShopProfile.isChecked && businessName.isNotEmpty()

        stylistUpdates["businessName"] = businessName
        stylistUpdates["businessBio"] = businessBio
        stylistUpdates["businessAddress"] = businessAddress
        stylistUpdates["businessPhone"] = businessPhone
        stylistUpdates["businessWebsite"] = businessWebsite
        stylistUpdates["isShopProfile"] = enabled

        if (enabled) {
            val existingShopId = currentShopId?.takeIf { it.isNotBlank() }
            val shopId = existingShopId ?: firestore.collection("shops").document().id
            currentShopId = shopId
            stylistUpdates["shopId"] = shopId

            val shopUpdates = mutableMapOf<String, Any>(
                "ownerId" to uid,
                "name" to businessName,
                "bio" to businessBio,
                "address" to businessAddress,
                "phone" to businessPhone,
                "website" to businessWebsite,
                "stylistIds" to FieldValue.arrayUnion(uid),
                "isPublic" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingShopId == null) {
                shopUpdates["createdAt"] = FieldValue.serverTimestamp()
            }
            firestore.collection("shops").document(shopId).set(shopUpdates, SetOptions.merge()).await()
        } else {
            currentShopId?.takeIf { it.isNotBlank() }?.let { shopId ->
                firestore.collection("shops").document(shopId).set(
                    mapOf(
                        "isPublic" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
                stylistUpdates["shopId"] = shopId
            }
        }
    }

    private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

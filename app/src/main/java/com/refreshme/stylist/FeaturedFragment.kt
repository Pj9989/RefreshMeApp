package com.refreshme.stylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.refreshme.BuildConfig
import com.refreshme.R
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FeaturedFragment : Fragment() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val functions by lazy { FirebaseFunctions.getInstance() }
    private lateinit var paymentSheet: PaymentSheet

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_featured, container, false)

        PaymentConfiguration.init(
            requireContext(),
            BuildConfig.STRIPE_PUBLISHABLE_KEY
        )
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        val purchaseButton = view.findViewById<Button>(R.id.purchase_button)
        purchaseButton.setOnClickListener {
            createFeaturedPaymentIntent()
        }

        return view
    }

    private fun createFeaturedPaymentIntent() {
        lifecycleScope.launch {
            try {
                val data = hashMapOf(
                    "stylistId" to auth.currentUser?.uid,
                    "amount" to 1000 // $10 in cents
                )

                val result = functions
                    .getHttpsCallable("createFeaturedPaymentIntent")
                    .call(data)
                    .await()

                val response = result.data as? HashMap<String, Any>
                val clientSecret = response?.get("clientSecret") as? String

                if (clientSecret != null) {
                    paymentSheet.presentWithPaymentIntent(
                        clientSecret,
                        PaymentSheet.Configuration(
                            merchantDisplayName = "RefreshMe"
                        )
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error creating payment intent: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(context, "Payment canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(context, "Payment failed: ${paymentSheetResult.error.message}", Toast.LENGTH_LONG).show()
            }
            is PaymentSheetResult.Completed -> {
                setStylistAsFeatured()
            }
        }
    }

    private fun setStylistAsFeatured() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("stylists").document(userId)
            .update("isFeatured", true)
            .addOnSuccessListener {
                Toast.makeText(context, "You are now a featured stylist!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error setting you as a featured stylist", Toast.LENGTH_SHORT).show()
            }
    }
}
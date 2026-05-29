package com.refreshme.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {

    /**
     * Fetch bookings for a customer (using both userId and customerId fields for compatibility)
     */
    fun getCustomerBookings(userId: String): Flow<List<Booking>> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null && currentUid != userId) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var bookingsByUserId = emptyList<Booking>()
        var bookingsByCustomerId = emptyList<Booking>()

        fun emitMerged() {
            val merged = (bookingsByUserId + bookingsByCustomerId).distinctBy { it.id }
                .sortedByDescending { it.requestedAt?.seconds ?: 0 }
            trySend(merged)
        }

        val sub1 = firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                bookingsByUserId = snapshot?.documents?.mapNotNull { parseBooking(it) } ?: emptyList()
                emitMerged()
            }

        val sub2 = firestore.collection("bookings")
            .whereEqualTo("customerId", userId)
            .addSnapshotListener { snapshot, _ ->
                bookingsByCustomerId = snapshot?.documents?.mapNotNull { parseBooking(it) } ?: emptyList()
                emitMerged()
            }

        awaitClose { 
            sub1.remove() 
            sub2.remove()
        }
    }

    /**
     * Fetch bookings for a stylist
     */
    fun getStylistBookings(stylistId: String): Flow<List<Booking>> = callbackFlow {
        Log.d("BookingRepo", "Listening for stylist bookings for ID: $stylistId")
        
        val subscription = firestore.collection("bookings")
            .whereEqualTo("stylistId", stylistId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BookingRepo", "Stylist bookings error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    Log.d("BookingRepo", "Received ${snapshot.size()} docs for stylist $stylistId")
                    val bookings = snapshot.documents.mapNotNull { doc ->
                        parseBooking(doc)
                    }.sortedByDescending { it.requestedAt?.seconds ?: 0 }
                    trySend(bookings)
                }
            }
        awaitClose { subscription.remove() }
    }

    private fun parseBooking(doc: com.google.firebase.firestore.DocumentSnapshot): Booking? {
        return try {
            val statusStr = doc.getString("status")?.uppercase() ?: "REQUESTED"
            val status = try {
                BookingStatus.valueOf(statusStr).name
            } catch (e: Exception) {
                when(statusStr) {
                    "CONFIRMED", "ACCEPTED" -> BookingStatus.ACCEPTED.name
                    "PENDING_PAYMENT", "PENDING" -> BookingStatus.PENDING.name
                    "DEPOSIT_PAID", "PAID" -> BookingStatus.DEPOSIT_PAID.name
                    "AWAITING_CUSTOMER_CONFIRMATION", "PENDING_CUSTOMER_CONFIRMATION", "COMPLETION_PENDING" -> BookingStatus.AWAITING_CUSTOMER_CONFIRMATION.name
                    "COMPLETION_DISPUTED", "DISPUTED" -> BookingStatus.COMPLETION_DISPUTED.name
                    "PAID_IN_FULL", "COMPLETED" -> BookingStatus.COMPLETED.name
                    "REFUNDED", "CANCELLED", "PAYMENT_CANCELLED", "PAYMENT_FAILED" -> BookingStatus.CANCELLED.name
                    "DECLINED" -> BookingStatus.DECLINED.name
                    else -> BookingStatus.REQUESTED.name
                }
            }
            
            // Handle various date formats (Timestamp, Long, etc.)
            val bookingDate = doc.getTimestamp("date")
                ?: doc.getTimestamp("bookingDate")
                ?: doc.getTimestamp("requestedAt")
                ?: doc.getTimestamp("startTime")
                ?: doc.getLong("date")?.let { Timestamp(Date(it)) }
                ?: doc.getLong("bookingDate")?.let { Timestamp(Date(it)) }
                ?: doc.getLong("requestedAt")?.let { Timestamp(Date(it)) }

            val priceCents = doc.getLong("priceCents") 
                ?: (doc.get("servicePrice") as? Number)?.toDouble()?.times(100)?.toLong()
                ?: (doc.get("price") as? Number)?.toDouble()?.times(100)?.toLong()
                ?: 0L

            Booking(
                id = doc.id,
                stylistId = doc.getString("stylistId") ?: "",
                stylistName = doc.getString("stylistName") ?: "Stylist",
                stylistPhotoUrl = doc.getString("stylistPhotoUrl"),
                customerId = doc.getString("customerId") ?: doc.getString("userId") ?: "",
                customerName = doc.getString("customerName") ?: "Client",
                customerPhotoUrl = doc.getString("customerPhotoUrl") ?: doc.getString("customerPhoto"),
                serviceName = doc.getString("serviceName") ?: doc.getString("service") ?: "Service",
                notes = doc.getString("notes") ?: "",
                status = status,
                requestedAt = bookingDate,
                priceCents = priceCents,
                isRated = doc.getBoolean("isRated") ?: false,
                isCustomerRated = doc.getBoolean("isCustomerRated") ?: false,
                scheduledStart = doc.getTimestamp("startTime") ?: bookingDate,
                isMobile = doc.getBoolean("isMobile") ?: false,
                travelFeeApplied = (doc.get("travelFeeApplied") as? Number)?.toDouble(),
                emergencyFeeApplied = (doc.get("emergencyFeeApplied") as? Number)?.toDouble(),
                customerAddress = doc.getString("customerAddress"),
                customerLat = doc.getDouble("customerLat"),
                customerLng = doc.getDouble("customerLng"),
                stylistLat = doc.getDouble("stylistLat"),
                stylistLng = doc.getDouble("stylistLng")
            )
        } catch (e: Exception) {
            Log.e("BookingRepo", "Error parsing booking ${doc.id}", e)
            null
        }
    }

    suspend fun createBooking(req: Booking): Result<String> {
        return Result.failure(
            UnsupportedOperationException(
                "Direct client booking creation is disabled. Use the payment-backed booking flow."
            )
        )
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Result<Unit> {
        return try {
            val bookingRef = firestore.collection("bookings").document(bookingId)
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            // When the stylist declines/cancels a booking from Android, mark
            // cancelledBy + cancelledAt so the Cloud Function
            // `notifyCustomerOnBookingUpdate` issues a Stripe refund of the
            // deposit (per policy: deposits are refundable when the stylist
            // cancels, but NOT when the customer cancels).
            if (status == BookingStatus.DECLINED || status == BookingStatus.CANCELLED) {
                updates["cancelledBy"] = "stylist"
                updates["cancelledAt"] = FieldValue.serverTimestamp()
            }
            bookingRef.update(updates).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun rescheduleBooking(bookingId: String, newStartTime: Timestamp): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to BookingStatus.REQUESTED.name, // Reset to requested so stylist confirms
                "startTime" to newStartTime,
                "date" to newStartTime,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("bookings").document(bookingId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitReview(booking: Booking, rating: Double, comment: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("User not logged in")
            
            val review = Review(
                userId = user.uid,
                userName = user.displayName ?: "Verified Client",
                stylistId = booking.stylistId,
                rating = rating,
                comment = comment,
                timestampMillis = System.currentTimeMillis()
            )

            firestore.runTransaction { transaction ->
                val stylistRef = firestore.collection("stylists").document(booking.stylistId)
                val stylistDoc = transaction.get(stylistRef)
                
                val currentRating = stylistDoc.getDouble("rating") ?: 0.0
                val currentReviewCount = stylistDoc.getLong("reviewCount") ?: 0L
                
                val newReviewCount = currentReviewCount + 1
                val newRating = ((currentRating * currentReviewCount) + rating) / newReviewCount
                
                transaction.update(stylistRef, mapOf(
                    "rating" to newRating,
                    "reviewCount" to newReviewCount,
                    "reviews" to FieldValue.arrayUnion(review)
                ))
                
                val bookingRef = firestore.collection("bookings").document(booking.id)
                transaction.update(bookingRef, "isRated", true)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitCustomerReview(booking: Booking, rating: Double, comment: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("User not logged in")

            val review = Review(
                userId = user.uid,
                userName = user.displayName ?: "Verified Stylist",
                stylistId = booking.customerId, // Using stylistId to store customerId in the Review model to reuse it
                rating = rating,
                comment = comment,
                timestampMillis = System.currentTimeMillis()
            )

            firestore.runTransaction { transaction ->
                val customerRef = firestore.collection("users").document(booking.customerId)
                val customerDoc = transaction.get(customerRef)

                val currentRating = customerDoc.getDouble("rating") ?: 0.0
                val currentReviewCount = customerDoc.getLong("reviewCount") ?: 0L

                val newReviewCount = currentReviewCount + 1
                val newRating = ((currentRating * currentReviewCount) + rating) / newReviewCount

                transaction.update(customerRef, mapOf(
                    "rating" to newRating,
                    "reviewCount" to newReviewCount,
                    "reviews" to FieldValue.arrayUnion(review)
                ))

                val bookingRef = firestore.collection("bookings").document(booking.id)
                transaction.update(bookingRef, "isCustomerRated", true)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitDirectReview(stylistId: String, rating: Double, comment: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("User not logged in")
            
            val review = Review(
                userId = user.uid,
                userName = user.displayName ?: "Verified Client",
                stylistId = stylistId,
                rating = rating,
                comment = comment,
                timestampMillis = System.currentTimeMillis()
            )

            firestore.runTransaction { transaction ->
                val stylistRef = firestore.collection("stylists").document(stylistId)
                val stylistDoc = transaction.get(stylistRef)
                
                val currentRating = stylistDoc.getDouble("rating") ?: 0.0
                val currentReviewCount = stylistDoc.getLong("reviewCount") ?: 0L
                
                val newReviewCount = currentReviewCount + 1
                val newRating = ((currentRating * currentReviewCount) + rating) / newReviewCount
                
                transaction.update(stylistRef, mapOf(
                    "rating" to newRating,
                    "reviewCount" to newReviewCount,
                    "reviews" to FieldValue.arrayUnion(review)
                ))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptBookingRequest(bookingId: String) = updateBookingStatus(bookingId, BookingStatus.ACCEPTED)
    suspend fun declineBookingRequest(bookingId: String) = updateBookingStatus(bookingId, BookingStatus.DECLINED)

    suspend fun requestBookingCompletion(bookingId: String): Result<Unit> {
        return try {
            functions.getHttpsCallable("requestBookingCompletion")
                .call(hashMapOf("bookingId" to bookingId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmBookingCompletion(bookingId: String): Result<Unit> {
        return try {
            functions.getHttpsCallable("confirmBookingCompletion")
                .call(hashMapOf("bookingId" to bookingId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disputeBookingCompletion(bookingId: String, reason: String): Result<Unit> {
        return try {
            functions.getHttpsCallable("disputeBookingCompletion")
                .call(hashMapOf("bookingId" to bookingId, "reason" to reason))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

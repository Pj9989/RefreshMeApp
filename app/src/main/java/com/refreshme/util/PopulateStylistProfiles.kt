package com.refreshme.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.refreshme.data.Appointment
import com.refreshme.data.Review
import com.refreshme.data.Service
import com.refreshme.data.ServiceType
import com.refreshme.data.Stylist
import com.refreshme.data.VerificationStatus
import com.refreshme.data.WorkingHours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

object PopulateStylistProfiles {

    private const val TAG = "PopulateStylistProfiles"

    fun populateData() {
        // Use an IO CoroutineScope instead of the heavily discouraged GlobalScope
        CoroutineScope(Dispatchers.IO).launch {
            try {
                deleteAllStylists()
                populateNewStylists()
            } catch (e: Exception) {
                Log.e(TAG, "Error populating data", e)
            }
        }
    }

    private suspend fun deleteAllStylists() {
        val firestore = FirebaseFirestore.getInstance()
        val stylistsCollection = firestore.collection("stylists")

        Log.d(TAG, "Deleting all stylists...")

        try {
            val snapshot = stylistsCollection.get().await()
            for (document in snapshot.documents) {
                document.reference.delete().await()
            }
            Log.d(TAG, "All stylists deleted.")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting stylists", e)
            throw e
        }
    }

    private fun populateNewStylists() {
        val firestore = FirebaseFirestore.getInstance()
        val stylistsCollection = firestore.collection("stylists")
        val stylists = listOf(
            Stylist(
                name = "Jenna 'Cuts' Smith",
                specialty = "Fades & Tapers",
                rating = 4.9,
                reviewCount = 120,
                isVerified = true,
                isOnline = true,
                location = GeoPoint(34.0522, -118.2437),
                profileImageUrl = "https://images.pexels.com/photos/3993324/pexels-photo-3993324.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2",
                workingHours = listOf(
                    WorkingHours(2, "09:00", "17:00"), // Monday
                    WorkingHours(3, "09:00", "17:00"), // Tuesday
                    WorkingHours(4, "09:00", "17:00"), // Wednesday
                    WorkingHours(5, "09:00", "17:00"), // Thursday
                    WorkingHours(6, "09:00", "17:00")  // Friday
                ),
                bookedAppointments = listOf(
                    Appointment(getTodayMillis(10, 0), 60),
                    Appointment(getTodayMillis(14, 0), 90)
                ),
                isAvailable = true,
                address = "123 Main St, Los Angeles, CA",
                bio = "Experienced professional specializing in modern cuts and fades.",
                services = listOf(
                    Service(name = "Line-up", price = 25.0, durationMinutes = 30),
                    Service(name = "Fade", price = 40.0, durationMinutes = 60),
                    Service(name = "House Call", price = 60.0, durationMinutes = 90)
                ),
                portfolioImages = listOf(
                    "https://images.pexels.com/photos/1813272/pexels-photo-1813272.jpeg",
                    "https://images.pexels.com/photos/3993324/pexels-photo-3993324.jpeg"
                ),
                reviews = listOf(
                    Review(userId = "user1", userName = "John Doe", stylistId = "stylist1", rating = 5.0, comment = "Great haircut!", timestampMillis = System.currentTimeMillis()),
                    Review(userId = "user2", userName = "Jane Smith", stylistId = "stylist1", rating = 4.0, comment = "Good service.", timestampMillis = System.currentTimeMillis())
                ),
                serviceType = ServiceType.ALL_HOURS,
                offersAtHomeService = true,
                atHomeServiceFee = 15.0,
                emergencyFee = 35.0,
                travelBufferMinutes = 45,
                requiresIdVerificationForMobile = true,
                verificationStatus = VerificationStatus.VERIFIED,
                yearsOfExperience = 5,
                tools = listOf("Clippers", "Trimmers", "Straight Razor"),
                vibes = listOf("Urban", "Trendy", "Fast")
            ),
            Stylist(
                name = "Maya 'Silk Press' Johnson",
                specialty = "Silk Press",
                rating = 4.9,
                reviewCount = 205,
                isVerified = true,
                isOnline = true,
                location = GeoPoint(34.0532, -118.2447),
                profileImageUrl = "https://images.pexels.com/photos/1813272/pexels-photo-1813272.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2",
                workingHours = listOf(
                    WorkingHours(2, "10:00", "18:00"),
                    WorkingHours(3, "10:00", "18:00"),
                    WorkingHours(4, "10:00", "18:00"),
                    WorkingHours(5, "10:00", "18:00"),
                    WorkingHours(6, "10:00", "18:00")
                ),
                isAvailable = true,
                services = listOf(
                    Service(name = "Silk Press", price = 85.0, durationMinutes = 90),
                    Service(name = "Trim", price = 30.0, durationMinutes = 30),
                ),
                yearsOfExperience = 8,
                tools = listOf("Titanium Flat Iron", "Blow Dryer"),
                offersAtHomeService = true,
                atHomeServiceFee = 25.0,
                emergencyFee = 50.0,
                travelBufferMinutes = 30,
                requiresIdVerificationForMobile = true,
                vibes = listOf("Safe Space", "Luxury", "VIP/Private")
            ),
            Stylist(
                name = "Isabella 'Bella' Rossi",
                specialty = "Balayage",
                rating = 4.9,
                reviewCount = 150,
                isVerified = true,
                isOnline = true,
                location = GeoPoint(34.0542, -118.2457),
                profileImageUrl = "https://images.pexels.com/photos/3998419/pexels-photo-3998419.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2",
                workingHours = listOf(
                    WorkingHours(2, "08:00", "16:00"),
                    WorkingHours(3, "08:00", "16:00"),
                    WorkingHours(4, "08:00", "16:00")
                ),
                services = listOf(
                    Service(name = "Women's Cut", price = 60.0, durationMinutes = 75),
                    Service(name = "Coloring / Balayage", price = 150.0, durationMinutes = 180),
                ),
                yearsOfExperience = 10,
                tools = listOf("Professional Scissors", "Dyson Hair Dryer"),
                offersAtHomeService = true,
                atHomeServiceFee = 40.0,
                emergencyFee = 100.0, // Last minute color correction
                travelBufferMinutes = 60,
                requiresIdVerificationForMobile = true,
                vibes = listOf("Artistic", "Safe Space", "LGBTQ+ Friendly")
            ),
            Stylist(
                name = "Sarah 'The Braider' Davis",
                specialty = "Braids & Extensions",
                rating = 4.8,
                reviewCount = 95,
                isVerified = false,
                isOnline = true,
                location = GeoPoint(34.0552, -118.2467),
                profileImageUrl = "https://images.pexels.com/photos/428364/pexels-photo-428364.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2",
                services = listOf(
                    Service(name = "Knotless Braids", price = 180.0, durationMinutes = 240),
                    Service(name = "Wig Install", price = 120.0, durationMinutes = 120),
                ),
                yearsOfExperience = 4,
                tools = listOf("Edge Control", "Braiding Gel"),
                offersAtHomeService = true,
                atHomeServiceFee = 30.0,
                emergencyFee = 75.0,
                travelBufferMinutes = 45,
                requiresIdVerificationForMobile = true,
                serviceType = ServiceType.AFTER_HOURS,
                vibes = listOf("Urban", "Kid-Friendly", "Quiet")
            )
        )
        stylists.forEach { stylist ->
            stylistsCollection.add(stylist)
                .addOnSuccessListener { Log.d(TAG, "Stylist added: ${stylist.name}") }
                .addOnFailureListener { e -> Log.w(TAG, "Error adding stylist", e) }
        }
    }

    private fun getTodayMillis(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
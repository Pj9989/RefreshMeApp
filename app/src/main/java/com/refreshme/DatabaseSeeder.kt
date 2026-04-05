package com.refreshme

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar
import java.util.Date

object DatabaseSeeder {

    fun seedData() {
        val db = FirebaseFirestore.getInstance()
        val stylistsCollection = db.collection("stylists")
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, 4)
        val flashDealExpiry = calendar.time

        // 1. The "Vibe" Master (Urban/Streetwear)
        val stylistAlex = hashMapOf(
            "id" to "stylist_1",
            "name" to "Alex Rivera",
            "specialty" to "Master Barber",
            "rating" to 4.9,
            "reviewCount" to 124,
            "profileImageUrl" to "https://images.unsplash.com/photo-1599316301019-21b3f7f7e2e0?ixlib=rb-4.0.3\u0026auto=format\u0026fit=crop\u0026w=400\u0026q=80",
            "availableNow" to true,
            "isAvailable" to true,
            "isVerified" to true,
            "online" to true,
            "offersAtHomeService" to true,
            "atHomeServiceFee" to 15.0,
            "bio" to "Specializing in precision fades and urban styles. I bring the shop experience to your doorstep.",
            "vibes" to listOf("Urban", "Hip-Hop", "Streetwear", "Fast"),
            "recommendedFaceShapes" to listOf("SQUARE", "OVAL"),
            "yearsOfExperience" to 8,
            "services" to listOf(
                mapOf("id" to "s1", "name" to "Skin Fade", "price" to 35.0, "durationMinutes" to 45),
                mapOf("id" to "s2", "name" to "Beard Sculpting", "price" to 20.0, "durationMinutes" to 25)
            ),
            "reviews" to listOf(
                mapOf(
                    "userId" to "user_1",
                    "userName" to "John D.",
                    "rating" to 5.0,
                    "comment" to "Best fade in the city, hands down. Super convenient that he comes to you.",
                    "timestamp" to Timestamp.now()
                ),
                mapOf(
                    "userId" to "user_2",
                    "userName" to "Mike S.",
                    "rating" to 4.5,
                    "comment" to "Fast and professional. Recommended.",
                    "timestamp" to Timestamp.now()
                )
            ),
            "beforeAfterImages" to listOf(
                mapOf(
                    "beforeImageUrl" to "https://images.unsplash.com/photo-1522337363643-594c9f776c03?auto=format\u0026fit=crop\u0026w=400\u0026q=80",
                    "afterImageUrl" to "https://images.unsplash.com/photo-1621605815971-fbc98d665033?auto=format\u0026fit=crop\u0026w=400\u0026q=80",
                    "description" to "High Skin Fade + Crop",
                    "technicalNotes" to "Zero gap clippers for the fade, texturizing powder on top, forward flow.",
                    "tags" to listOf("Fade", "Crop", "Texture"),
                    "timestamp" to Timestamp.now()
                )
            ),
            "currentFlashDeal" to mapOf(
                "title" to "Rainy Day Special",
                "description" to "20% off all fades today only!",
                "discountPercentage" to 20,
                "expiryTime" to Timestamp(flashDealExpiry)
            )
        )

        // 2. The "Classic" Gentleman (Luxury/Quiet)
        val stylistBella = hashMapOf(
            "id" to "stylist_isabella",
            "name" to "Isabella 'Bella' Rossi",
            "specialty" to "Modern Styles",
            "rating" to 5.0,
            "reviewCount" to 89,
            "profileImageUrl" to "https://images.pexels.com/photos/3998419/pexels-photo-3998419.jpeg",
            "availableNow" to true,
            "isAvailable" to true, 
            "isVerified" to true,
            "online" to true,
            "offersAtHomeService" to false,
            "bio" to "A quiet, luxury experience for the modern gentleman. Espresso served with every cut.",
            "vibes" to listOf("Luxury", "Quiet", "Executive", "Classic"),
            "recommendedFaceShapes" to listOf("OBLONG", "HEART", "OVAL"),
            "yearsOfExperience" to 12,
            "services" to listOf(
                mapOf("id" to "s3", "name" to "Executive Cut", "price" to 60.0, "durationMinutes" to 60),
                mapOf("id" to "s4", "name" to "Hot Towel Shave", "price" to 45.0, "durationMinutes" to 45)
            ),
            "reviews" to listOf(
                mapOf(
                    "userId" to "user_3",
                    "userName" to "Arthur P.",
                    "rating" to 5.0,
                    "comment" to "Bella is a true artist. The environment is so relaxing.",
                    "timestamp" to Timestamp.now()
                )
            ),
            "beforeAfterImages" to listOf(
                mapOf(
                    "beforeImageUrl" to "https://images.unsplash.com/photo-1595152772835-219674b2a8a6?auto=format\u0026fit=crop\u0026w=400\u0026q=80",
                    "afterImageUrl" to "https://images.unsplash.com/photo-1605497788044-5a32c7078486?auto=format\u0026fit=crop\u0026w=400\u0026q=80",
                    "description" to "Classic Side Part",
                    "technicalNotes" to "Scissor over comb, tapered neckline, styled with high-shine pomade.",
                    "tags" to listOf("Scissor Cut", "Classic", "Pomade"),
                    "timestamp" to Timestamp.now()
                )
            )
        )

        // 3. The "Creative" Stylist (Trendy/Artistic)
        val stylistMarcus = hashMapOf(
            "id" to "stylist_marcus",
            "name" to "Marcus Green",
            "specialty" to "Color & Design",
            "rating" to 4.7,
            "reviewCount" to 45,
            "profileImageUrl" to "https://images.unsplash.com/photo-1580894732444-8ecded7900cd?auto=format\u0026fit=crop\u0026w=400\u0026q=80",
            "availableNow" to true,
            "isAvailable" to true,
            "isVerified" to true,
            "online" to true,
            "offersAtHomeService" to true,
            "atHomeServiceFee" to 10.0,
            "bio" to "Hair tattoos, vivid colors, and breaking the rules. Let's create something unique.",
            "vibes" to listOf("Artistic", "Trendy", "Loud", "Creative"),
            "recommendedFaceShapes" to listOf("ROUND", "HEART"),
            "yearsOfExperience" to 4,
            "services" to listOf(
                mapOf("id" to "s5", "name" to "Hair Design / Tattoo", "price" to 50.0, "durationMinutes" to 60),
                mapOf("id" to "s6", "name" to "Bleach & Tone", "price" to 120.0, "durationMinutes" to 120)
            )
        )

        val stylists = listOf(stylistAlex, stylistBella, stylistMarcus)

        for (stylist in stylists) {
            stylistsCollection.document(stylist["id"] as String)
                .set(stylist, SetOptions.merge())
                .addOnSuccessListener { Log.d("Seeder", "Seeded/Updated ${stylist["name"]}") }
                .addOnFailureListener { e -> Log.e("Seeder", "Error seeding ${stylist["name"]}", e) }
        }
    }
}

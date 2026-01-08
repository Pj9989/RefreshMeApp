package com.refreshme

import com.google.firebase.firestore.FirebaseFirestore

object DatabaseSeeder {

    fun seedData() {
        val db = FirebaseFirestore.getInstance()

        val stylists = listOf(
            mapOf(
                "id" to "stylist_1",
                "name" to "Alex Rivera",
                "specialty" to "Master Barber",
                "rating" to 4.9,
                "imageUrl" to "https://images.unsplash.com/photo-1599316301019-21b3f7f7e2e0?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=400&q=80",
                "isAvailable" to true,
                "bio" to "Over 10 years of experience in classic cuts and modern fades. Specializing in beard grooming and hot towel shaves.",
                "portfolioImages" to listOf(
                    "https://images.unsplash.com/photo-1549747594-5463f6e1e2e9?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=400&q=80",
                    "https://images.unsplash.com/photo-1521460391332-2b2e7a4d6e9e?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=400&q=80"
                ),
                "services" to listOf(
                    mapOf("name" to "Classic Haircut", "price" to 30.0, "durationMinutes" to 45),
                    mapOf("name" to "Beard Trim", "price" to 15.0, "durationMinutes" to 20),
                    mapOf("name" to "The Royal Shave", "price" to 50.0, "durationMinutes" to 60)
                )
            ),
            mapOf(
                "id" to "stylist_2",
                "name" to "Jordan Smith",
                "specialty" to "Color Expert",
                "rating" to 4.8,
                "imageUrl" to "https://images.unsplash.com/photo-1595152772295-f12a3d7c2a7a?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=400&q=80",
                "isAvailable" to true,
                "bio" to "Passionate about vibrant colors and creative styling. Certified in advanced balayage techniques.",
                "portfolioImages" to listOf(
                    "https://images.unsplash.com/photo-1540622875319-3a3f8e0d9f4e?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=400&q=80",
                    "https://images.unsplash.com/photo-1542328583-00a29037c68d?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=400&q=80"
                ),
                "services" to listOf(
                    mapOf("name" to "Full Color", "price" to 150.0, "durationMinutes" to 120),
                    mapOf("name" to "Balayage", "price" to 180.0, "durationMinutes" to 180),
                    mapOf("name" to "Blowout", "price" to 45.0, "durationMinutes" to 45)
                )
            )
        )

        for (stylist in stylists) {
            db.collection("stylists").document(stylist["id"] as String).set(stylist)
        }
    }
}
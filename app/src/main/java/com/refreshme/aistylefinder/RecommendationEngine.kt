package com.refreshme.aistylefinder

/**
 * Engine that processes quiz answers and generates hairstyle recommendations
 */
object RecommendationEngine {

    // Sample Unsplash images for styles
    private const val IMG_TAPER = "https://images.unsplash.com/photo-1599351431247-f10f20427cca?w=800"
    private const val IMG_FADE = "https://images.unsplash.com/photo-1621605815844-8da811d2e45d?w=800"
    private const val IMG_CROP = "https://images.unsplash.com/photo-1593702295094-ada74342990e?w=800"
    private const val IMG_BOB = "https://images.unsplash.com/photo-1492106087820-71f1a00d2b11?w=800"
    private const val IMG_BUZZ = "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=800"
    private const val IMG_LONG = "https://images.unsplash.com/photo-1519699047748-de8e457a634e?w=800"

    fun getRecommendations(request: AiStyleRequest): List<AiStyleRecommendation> {
        val key = "${request.vibe}_${request.frequency}_${request.finish}"
        
        val baseRecommendations = when (key) {
            // Clean & Classic combinations
            "clean_classic_weekly_natural" -> listOf(
                AiStyleRecommendation(
                    "Classic Taper",
                    "Based on your preference for a clean, classic look with natural finish and weekly maintenance, the Classic Taper offers timeless style that's easy to maintain.",
                    "classic_cuts",
                    IMG_TAPER
                ),
                AiStyleRecommendation(
                    "Gentleman's Cut",
                    "A refined, professional style that looks natural and requires weekly touch-ups to maintain its polished appearance.",
                    "classic_cuts",
                    IMG_TAPER
                )
            )
            "clean_classic_weekly_sharp" -> listOf(
                AiStyleRecommendation(
                    "Professional Cut",
                    "Your preference for clean lines with sharp finish and weekly maintenance calls for a Professional Cut with precise edges.",
                    "line_ups",
                    IMG_FADE
                ),
                AiStyleRecommendation(
                    "Clean Fade",
                    "A sharp, well-defined fade that maintains crisp lines with weekly visits.",
                    "fades",
                    IMG_FADE
                )
            )
            "bold_trendy_weekly_sharp" -> listOf(
                AiStyleRecommendation(
                    "High Fade",
                    "Bold, sharp fade that makes a statement and requires weekly upkeep for maximum impact.",
                    "fades",
                    IMG_FADE
                ),
                AiStyleRecommendation(
                    "Skin Fade",
                    "Ultra-sharp, trendy fade with crisp lines for a bold look.",
                    "fades",
                    IMG_FADE
                )
            )
            "bold_trendy_biweekly_natural" -> listOf(
                AiStyleRecommendation(
                    "Textured Crop",
                    "Trendy, textured style with natural finish that looks great for two weeks.",
                    "trendy_styles",
                    IMG_CROP
                )
            )
            "low_maintenance_monthly_natural" -> listOf(
                AiStyleRecommendation(
                    "The Classic Bob",
                    "Based on your low maintenance preference, this is a great match. This style requires minimal daily styling and grows out gracefully.",
                    "long_hair",
                    IMG_BOB
                ),
                AiStyleRecommendation(
                    "Long Layers",
                    "Easy-care longer style with natural movement.",
                    "long_hair",
                    IMG_LONG
                )
            )
            "low_maintenance_weekly_natural" -> listOf(
                AiStyleRecommendation(
                    "Buzz Cut",
                    "Ultra-low maintenance with a natural, effortless look and weekly touch-ups.",
                    "buzz_cuts",
                    IMG_BUZZ
                )
            )
            
            else -> listOf(
                AiStyleRecommendation(
                    "Modern Style",
                    "Contemporary cut that balances style and maintenance.",
                    "trendy_styles",
                    IMG_CROP
                )
            )
        }

        // Add Face Shape Advice
        val faceShapeAdvice = getFaceShapeAdvice(request.faceShape)
        return baseRecommendations.map { 
            it.copy(reasoning = "${it.reasoning}\n\n$faceShapeAdvice")
        }
    }

    private fun getFaceShapeAdvice(faceShape: String): String {
        return when (faceShape) {
            "OVAL" -> "Since you have an Oval face shape, you're in luck! This is considered the most balanced shape, and almost any hairstyle works well. We recommend keeping hair off your forehead to show off your features."
            "ROUND" -> "For your Round face shape, we recommend styles that add height and volume on top while keeping the sides short. This helps elongate your face and create a more balanced look."
            "SQUARE" -> "With your Square face shape, we suggest styles that soften your strong jawline. Textured cuts and side-swept bangs work great to add some curves to your overall look."
            "HEART" -> "For a Heart face shape, aim for styles that add width around the jawline. Longer styles or those with more volume on the sides will help balance your wider forehead."
            else -> "We've tailored these recommendations to your selected vibe and maintenance level."
        }
    }
    
    /**
     * Get specialty tags that match the recommended styles
     */
    fun getMatchingSpecialties(recommendations: List<AiStyleRecommendation>): List<String> {
        return recommendations.map { it.specialty }.distinct()
    }

    /**
     * Get a human-readable explanation of why a stylist matches based on their specialty
     */
    fun getMatchExplanation(specialty: String?, request: AiStyleRequest): String {
        if (specialty == null) return "This stylist offers a variety of services."
        
        val vibeText = when (request.vibe) {
            "clean_classic" -> "clean and classic"
            "bold_trendy" -> "bold and trendy"
            "low_maintenance" -> "low maintenance"
            else -> "personalized"
        }

        return when (specialty) {
            "classic_cuts" -> "Expert in the $vibeText styles you prefer, specializing in timeless precision."
            "fades" -> "Top-tier expert for the sharp fades and crisp lines recommended for your $vibeText look."
            "line_ups" -> "Perfect for maintaining the sharp, defined finish you selected in your quiz."
            "trendy_styles" -> "Specializes in the modern, fashion-forward looks that match your trendy vibe."
            "long_hair" -> "Highly skilled with the longer, natural styles that fit your low-maintenance profile."
            "buzz_cuts" -> "Ideal for the high-frequency, clean-cut look you're looking for."
            "texture_work" -> "Expert at adding the specific texture and movement recommended for your hair type."
            else -> "This stylist specializes in looks that complement your $vibeText style preferences."
        }
    }
}
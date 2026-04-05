package com.refreshme.data.stylefinder

data class AiStyleResult(
    val needsMoreInfo: Boolean = false,
    val recommendedStyles: List<RecommendedStyle> = emptyList()
)

data class RecommendedStyle(
    val styleId: String = "",
    val name: String = "",
    val whyItFits: String = "",
    val maintenance: String = "",
    val barberScript: String = "",
    val confidence: Int = 0,
    val tags: List<String> = emptyList()
)

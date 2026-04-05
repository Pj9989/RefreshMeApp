package com.refreshme.data.stylefinder

data class Style(
    val id: String = "",
    val name: String = "",
    val tags: List<String> = emptyList(),
    val worksForFaceShapes: List<String> = emptyList(),
    val worksForHairTypes: List<String> = emptyList(),
    val maintenance: String = "",
    val barberScript: String = "",
    val exampleImages: List<String> = emptyList()
)

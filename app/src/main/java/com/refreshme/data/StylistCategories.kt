package com.refreshme.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Canonical pro-category identifiers persisted in Firestore.
 *
 * Kept in sync 1:1 with the Flutter / iOS `StylistCategories` class so the
 * same documents are interoperable between platforms. When adding a new
 * category both platforms must be updated simultaneously.
 */
object StylistCategories {
    const val HAIR = "hair"
    const val MAKEUP = "makeup"
    const val NAILS = "nails"

    /** Ordered list used for UI pickers / filter chips. */
    val ALL: List<String> = listOf(HAIR, MAKEUP, NAILS)

    /** Human-readable label for a category id. Falls back to capitalizing the id. */
    fun label(id: String): String = when (id) {
        HAIR -> "Hair"
        MAKEUP -> "Makeup"
        NAILS -> "Nails"
        else -> id.ifEmpty { "" }
            .replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
    }

    /** Material icon for a category id. Used for filter chips, profile editor, and cards. */
    fun icon(id: String): ImageVector = when (id) {
        HAIR -> Icons.Default.ContentCut
        MAKEUP -> Icons.Default.Brush
        NAILS -> Icons.Default.Spa
        else -> Icons.Default.AutoAwesome
    }
}

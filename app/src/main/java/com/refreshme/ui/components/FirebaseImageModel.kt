package com.refreshme.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await

@Composable
fun rememberFirebaseImageModel(imageSource: String?): Any? {
    val source = imageSource?.trim()?.takeIf { it.isNotEmpty() }

    val model by produceState<Any?>(initialValue = source?.takeIf(::canLoadDirectly), source) {
        value = resolveFirebaseImageModel(source)
    }

    return model
}

private suspend fun resolveFirebaseImageModel(source: String?): Any? {
    if (source.isNullOrBlank()) return null
    if (canLoadDirectly(source)) return source

    return runCatching {
        val storage = Firebase.storage
        val reference = if (source.startsWith("gs://", ignoreCase = true)) {
            storage.getReferenceFromUrl(source)
        } else {
            storage.reference.child(source.trimStart('/'))
        }
        reference.downloadUrl.await().toString()
    }.getOrNull()
}

private fun canLoadDirectly(source: String): Boolean {
    val scheme = Uri.parse(source).scheme?.lowercase() ?: return false
    return scheme in setOf("http", "https", "content", "file", "android.resource")
}

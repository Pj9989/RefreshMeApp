package com.refreshme.util

import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

object StorageHelper {

    private val storage = Firebase.storage

    suspend fun uploadImage(bitmap: Bitmap, path: String): String {
        val storageRef = storage.reference.child(path)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()

        val uploadTask = storageRef.putBytes(data)
        val downloadUrl = uploadTask.await().storage.downloadUrl.await()
        return downloadUrl.toString()
    }

    suspend fun downloadUrl(path: String): Uri? {
        return try {
            storage.reference.child(path).downloadUrl.await()
        } catch (e: Exception) {
            null
        }
    }
}
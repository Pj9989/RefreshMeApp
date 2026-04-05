package com.refreshme.aistylefinder

import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs

enum class FaceShape {
    OVAL,
    ROUND,
    SQUARE,
    HEART,
    OBLONG,
    UNKNOWN
}

object FaceAnalyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    private val detector = FaceDetection.getClient(options)

    fun analyzeFace(image: InputImage, onResult: (FaceShape) -> Unit) {
        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    onResult(FaceShape.UNKNOWN)
                    return@addOnSuccessListener
                }

                val face = faces[0]
                val shape = determineFaceShape(face)
                onResult(shape)
            }
            .addOnFailureListener { e ->
                Log.e("FaceAnalyzer", "Face detection failed", e)
                onResult(FaceShape.UNKNOWN)
            }
    }

    private fun determineFaceShape(face: Face): FaceShape {
        val bounds = face.boundingBox
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        
        if (width <= 0 || height <= 0) return FaceShape.UNKNOWN

        val ratio = height / width

        // Get landmarks for more precise analysis
        val leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK)?.position
        val rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
        
        // Use landmarks to refine the ratio-based guess
        return when {
            ratio > 1.5 -> FaceShape.OBLONG
            ratio > 1.3 -> FaceShape.OVAL
            ratio < 1.1 -> {
                // Round vs Square: Square usually has a wider lower face
                if (leftCheek != null && rightCheek != null && mouthBottom != null) {
                    val cheekWidth = abs(rightCheek.x - leftCheek.x)
                    if (cheekWidth / width > 0.8) FaceShape.SQUARE else FaceShape.ROUND
                } else {
                    FaceShape.ROUND
                }
            }
            else -> {
                // Heart shape check: Narrower lower face
                if (leftCheek != null && rightCheek != null && mouthBottom != null) {
                    val cheekWidth = abs(rightCheek.x - leftCheek.x)
                    if (cheekWidth / width > 0.9) FaceShape.HEART else FaceShape.OVAL
                } else {
                    FaceShape.OVAL
                }
            }
        }
    }
}
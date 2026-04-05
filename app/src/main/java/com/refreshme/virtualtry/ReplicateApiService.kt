package com.refreshme.virtualtry

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Handles all communication with the Replicate.com API.
 *
 * Replicate uses an asynchronous prediction model:
 * 1. POST /v1/predictions  → creates a prediction, returns an ID
 * 2. GET  /v1/predictions/{id} → poll until status == "succeeded" or "failed"
 *
 * Model used: tencentarc/photomaker-style (IP-Adapter style transfer)
 * which accepts a reference image + prompt and generates a styled portrait.
 *
 * To switch models, update MODEL_VERSION to any Replicate model version hash.
 */
object ReplicateApiService {

    private const val TAG = "ReplicateApiService"
    private const val BASE_URL = "https://api.replicate.com/v1"

    /**
     * Replicate model: tencentarc/photomaker-style
     * This model takes a reference photo of a person and a text prompt,
     * then generates a new portrait matching the described hairstyle/look.
     *
     * Version hash for photomaker-style (stable, widely used):
     * Replace this with any other Replicate model version hash as needed.
     */
    private const val MODEL_VERSION =
        "467d062309da518648ba89d226490e02b8ed09b5abc15026e54e31c5a8cd0769"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Submits a prediction to Replicate and polls until it completes.
     *
     * @param apiKey     The user's Replicate API token (r8_xxxx)
     * @param base64Image The selfie encoded as a Base64 data URI string
     * @param prompt      The AI prompt describing the desired hairstyle/look
     * @return The URL of the generated image, or null on failure
     */
    suspend fun generateHairstyle(
        apiKey: String,
        base64Image: String,
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Create the prediction
            val predictionId = createPrediction(apiKey, base64Image, prompt)
                ?: return@withContext Result.failure(Exception("Failed to create prediction. Check your API key."))

            Log.d(TAG, "Prediction created: $predictionId")

            // Step 2: Poll for the result (max 120 seconds, polling every 3 seconds)
            val imageUrl = pollForResult(apiKey, predictionId)
                ?: return@withContext Result.failure(Exception("Prediction timed out or failed. Please try again."))

            Log.d(TAG, "Prediction succeeded: $imageUrl")
            Result.success(imageUrl)

        } catch (e: Exception) {
            Log.e(TAG, "API call failed", e)
            Result.failure(e)
        }
    }

    private fun createPrediction(
        apiKey: String,
        base64Image: String,
        prompt: String
    ): String? {
        val json = JSONObject().apply {
            put("version", MODEL_VERSION)
            put("input", JSONObject().apply {
                // input_image: the user's selfie as a Base64 data URI
                put("input_image", base64Image)
                // prompt: the AI styling instruction
                put("prompt", prompt)
                // style_strength_ratio: how strongly to apply the style (20-50 is natural)
                put("style_strength_ratio", 20)
                // num_outputs: always 1 for this use case
                put("num_outputs", 1)
                // negative_prompt: things to avoid in the output
                put(
                    "negative_prompt",
                    "nsfw, lowres, bad anatomy, bad hands, text, error, missing fingers, " +
                            "extra digit, fewer digits, cropped, worst quality, low quality, " +
                            "normal quality, jpeg artifacts, signature, watermark, username, blurry"
                )
            })
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/predictions")
            .addHeader("Authorization", "Token $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null

        Log.d(TAG, "Create prediction response (${response.code}): $responseBody")

        if (!response.isSuccessful) {
            Log.e(TAG, "Create prediction failed: $responseBody")
            return null
        }

        return JSONObject(responseBody).optString("id").takeIf { it.isNotEmpty() }
    }

    private suspend fun pollForResult(
        apiKey: String,
        predictionId: String
    ): String? {
        val maxAttempts = 40  // 40 × 3s = 120 seconds max wait
        repeat(maxAttempts) { attempt ->
            delay(3_000L)  // Wait 3 seconds between polls

            val request = Request.Builder()
                .url("$BASE_URL/predictions/$predictionId")
                .addHeader("Authorization", "Token $apiKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return null
            val json = JSONObject(responseBody)

            val status = json.optString("status")
            Log.d(TAG, "Poll attempt ${attempt + 1}: status=$status")

            when (status) {
                "succeeded" -> {
                    val output = json.optJSONArray("output")
                    return output?.optString(0)?.takeIf { it.isNotEmpty() }
                }
                "failed", "canceled" -> {
                    val error = json.optString("error", "Unknown error")
                    Log.e(TAG, "Prediction $status: $error")
                    return null
                }
                // "starting", "processing" → keep polling
            }
        }
        return null  // Timed out
    }
}

package com.ml.shubham0204.clipandroid

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType // Import extension function
import java.io.IOException
import kotlin.coroutines.resume

class Translator {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val apiUrl = "https://api-inference.huggingface.co/models/Helsinki-NLP/opus-mt-vi-en"
    private val apiToken = "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" // Thay bằng token của bạn

    // Định nghĩa data class để parse JSON trả về
    data class TranslationResponse(
        @SerializedName("translation_text") val translationText: String
    )

    suspend fun translateText(inputText: String): String = suspendCancellableCoroutine { continuation ->
        Log.d("Translator", "Bắt đầu dịch: $inputText") // Thêm log đầu vào

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            """{"inputs": "vi: $inputText"}"""
        )

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Translator", "Lỗi khi gọi API: ${e.message}")
                continuation.resume(inputText) // Trả về nguyên bản nếu lỗi
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("Translator", "Phản hồi thô từ API: $responseBody") // Thêm log phản hồi thô
                    try {
                        val translationList = gson.fromJson(responseBody, Array<TranslationResponse>::class.java)
                        val translatedText = translationList.firstOrNull()?.translationText ?: inputText
                        Log.d("Translator", "Văn bản đã dịch: $translatedText") // Thêm log kết quả dịch
                        continuation.resume(translatedText)
                    } catch (e: Exception) {
                        Log.e("Translator", "Lỗi parse JSON: ${e.message}")
                        continuation.resume(inputText)
                    }
                } else {
                    Log.e("Translator", "API trả về lỗi: ${response.code} - ${response.message}")
                    continuation.resume(inputText)
                }
            }
        })
    }
}
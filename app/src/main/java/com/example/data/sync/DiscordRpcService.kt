package com.example.data.sync

import android.util.Log
import com.example.data.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DiscordRpcService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "DiscordRpcService"
    }

    suspend fun updatePresence(
        animeTitle: String,
        episodeNumber: Int,
        currentTimeSec: Long,
        durationSec: Long,
        coverImage: String,
        settings: AppSettings
    ): Boolean = withContext(Dispatchers.IO) {
        if (!settings.discordRpcEnabled || settings.discordWebhookUrl.isBlank()) {
            return@withContext false
        }

        try {
            val remainingSec = (durationSec - currentTimeSec).coerceAtLeast(0L)
            val remMin = remainingSec / 60
            val remSec = remainingSec % 60
            val remText = String.format("%02d:%02d remaining", remMin, remSec)

            val embed = JSONObject().apply {
                put("title", "Watching $animeTitle")
                put("description", "Episode $episodeNumber ($remText)")
                put("color", 5814783) // Theme accent cyan/emerald hex
                put("thumbnail", JSONObject().apply {
                    put("url", coverImage)
                })
                put("footer", JSONObject().apply {
                    put("text", "Zenith Anime • Android Client")
                })
            }

            val payload = JSONObject().apply {
                put("username", "Zenith Anime Status")
                put("embeds", JSONArray().apply { put(embed) })
            }

            val request = Request.Builder()
                .url(settings.discordWebhookUrl.trim())
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Discord RPC dispatch failed: ${e.message}")
            false
        }
    }
}

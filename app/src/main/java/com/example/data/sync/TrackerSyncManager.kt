package com.example.data.sync

import android.util.Log
import com.example.data.model.Anime
import com.example.data.model.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TrackerSyncManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "TrackerSyncManager"
    }

    suspend fun autoScrobble(
        anime: Anime,
        episodeNumber: Int,
        isCompleted: Boolean,
        settings: AppSettings
    ): Boolean = withContext(Dispatchers.IO) {
        if (!settings.trackerSyncEnabled || !settings.autoScrobbleEnabled) {
            return@withContext false
        }

        var anySuccess = false

        // 1. Scrobble to AniList if token present
        if (settings.anilistAuthToken.isNotBlank()) {
            val aniListId = if (!anime.id.startsWith("mal_")) anime.id.toIntOrNull() else null
            if (aniListId != null) {
                val ok = scrobbleAniList(aniListId, episodeNumber, isCompleted, settings.anilistAuthToken)
                if (ok) anySuccess = true
            }
        }

        // 2. Scrobble to MyAnimeList if token present
        if (settings.malAuthToken.isNotBlank()) {
            val malId = anime.malId.toIntOrNull()?.takeIf { it > 0 }
                ?: if (anime.id.startsWith("mal_")) anime.id.removePrefix("mal_").toIntOrNull()?.takeIf { it > 0 } else null
            if (malId != null) {
                val ok = scrobbleMyAnimeList(malId, episodeNumber, isCompleted, settings.malAuthToken)
                if (ok) anySuccess = true
            }
        }

        anySuccess
    }

    suspend fun scrobbleAniList(
        mediaId: Int,
        progress: Int,
        isCompleted: Boolean,
        authToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val mutation = """
                mutation (${'$'}mediaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus) {
                  SaveMediaListEntry (mediaId: ${'$'}mediaId, progress: ${'$'}progress, status: ${'$'}status) {
                    id
                    status
                    progress
                  }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("mediaId", mediaId)
                put("progress", progress)
                put("status", if (isCompleted) "COMPLETED" else "CURRENT")
            }

            val payload = JSONObject().apply {
                put("query", mutation)
                put("variables", variables)
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .header("Authorization", "Bearer ${authToken.trim()}")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                Log.d(TAG, "AniList scrobble mediaId=$mediaId ep=$progress completed=$isCompleted -> code=${response.code}")
                success
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed AniList scrobble: ${e.message}")
            false
        }
    }

    suspend fun scrobbleMyAnimeList(
        malId: Int,
        progress: Int,
        isCompleted: Boolean,
        authToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.myanimelist.net/v2/anime/$malId/my_list_status"
            val formBody = FormBody.Builder()
                .add("num_watched_episodes", progress.toString())
                .add("status", if (isCompleted) "completed" else "watching")
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${authToken.trim()}")
                .patch(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                Log.d(TAG, "MAL scrobble malId=$malId ep=$progress completed=$isCompleted -> code=${response.code}")
                success
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed MAL scrobble: ${e.message}")
            false
        }
    }
}

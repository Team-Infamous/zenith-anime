package com.example.data.network

import com.example.data.model.AnimeTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class AnimeThemesService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun getAnimeThemes(anilistId: String?, title: String): List<AnimeTheme> = withContext(Dispatchers.IO) {
        val results = mutableListOf<AnimeTheme>()

        // 1. Try querying by AniList external ID if available
        if (!anilistId.isNullOrBlank() && anilistId != "0" && !anilistId.startsWith("mal_")) {
            try {
                val url = "https://api.animethemes.moe/anime?filter[has]=resources&filter[site]=AniList&filter[external_id]=$anilistId&include=animethemes.animethemeentries.videos,animethemes.song.artists"
                val themes = fetchThemesFromUrl(url)
                if (themes.isNotEmpty()) return@withContext themes
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }

        // 2. Fallback search by anime title
        try {
            val cleanTitle = title.replace(Regex("(?i)(season|part|cour)\\s*\\d+"), "").trim()
            val encoded = URLEncoder.encode(if (cleanTitle.length > 2) cleanTitle else title, "UTF-8")
            val url = "https://api.animethemes.moe/anime?q=$encoded&include=animethemes.animethemeentries.videos,animethemes.song.artists"
            val themes = fetchThemesFromUrl(url)
            if (themes.isNotEmpty()) return@withContext themes
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        results
    }

    private fun fetchThemesFromUrl(url: String): List<AnimeTheme> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "ZenithAnime/1.3.0 (Android; pair-programming; client)")
            .header("Accept", "application/json")
            .build()

        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            response.body?.string()
        } ?: return emptyList()

        val root = JSONObject(body)
        val animeArray = root.optJSONArray("anime") ?: return emptyList()
        if (animeArray.length() == 0) return emptyList()

        val firstAnime = animeArray.getJSONObject(0)
        val themesArray = firstAnime.optJSONArray("animethemes") ?: return emptyList()
        val result = mutableListOf<AnimeTheme>()

        for (i in 0 until themesArray.length()) {
            val themeObj = themesArray.getJSONObject(i)
            val id = themeObj.optInt("id", i + 1)
            val type = themeObj.optString("type", "OP")
            val sequence = themeObj.optInt("sequence", if (type == "OP") 1 else 1)

            val songObj = themeObj.optJSONObject("song")
            val songTitle = songObj?.optString("title", "")?.takeIf { it.isNotBlank() } ?: "$type $sequence"

            var artistName = ""
            val artistsArray = songObj?.optJSONArray("artists")
            if (artistsArray != null && artistsArray.length() > 0) {
                artistName = artistsArray.getJSONObject(0).optString("name", "")
            }

            val entries = themeObj.optJSONArray("animethemeentries")
            var videoUrl = ""
            var resolution = "1080p"
            var episodes = ""

            if (entries != null && entries.length() > 0) {
                val entryObj = entries.getJSONObject(0)
                episodes = entryObj.optString("episodes", "")
                val videos = entryObj.optJSONArray("videos")
                if (videos != null && videos.length() > 0) {
                    val videoObj = videos.getJSONObject(0)
                    videoUrl = videoObj.optString("link", "")
                    val resInt = videoObj.optInt("resolution", 1080)
                    resolution = "${resInt}p"
                }
            }

            if (videoUrl.isNotBlank()) {
                result.add(
                    AnimeTheme(
                        id = id,
                        type = type,
                        sequence = sequence,
                        songTitle = songTitle,
                        artist = artistName,
                        episodes = episodes,
                        videoUrl = videoUrl,
                        resolution = resolution
                    )
                )
            }
        }

        return result
    }
}

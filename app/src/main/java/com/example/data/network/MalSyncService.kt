package com.example.data.network

import com.example.data.model.Anime
import com.example.data.model.WatchStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MalSyncService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun importFromMyAnimeList(username: String): Result<List<Anime>> = withContext(Dispatchers.IO) {
        try {
            val trimmed = username.trim()
            if (trimmed.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Username cannot be empty"))

            // MAL public list JSON endpoint
            val url = "https://myanimelist.net/animelist/$trimmed/load.json?offset=0&status=7"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string()
            } ?: return@withContext Result.failure(Exception("Empty response from MyAnimeList"))

            val jsonArray = JSONArray(body)
            val list = mutableListOf<Anime>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val malId = item.optInt("anime_id", 0).toString()
                val title = item.optString("anime_title", "Anime")
                val cover = item.optString("anime_image_path", "")
                val mediaType = item.optString("anime_media_type_string", "TV")
                val numEpisodes = item.optInt("anime_num_episodes", 12).coerceAtLeast(1)
                val malStatus = item.optInt("status", 1)
                val score = item.optDouble("score", 8.0)

                val watchStatus = when (malStatus) {
                    1 -> WatchStatus.WATCHING.name
                    2 -> WatchStatus.COMPLETED.name
                    3 -> WatchStatus.ON_HOLD.name
                    4 -> WatchStatus.DROPPED.name
                    6 -> WatchStatus.PLAN_TO_WATCH.name
                    else -> WatchStatus.WATCHING.name
                }

                list.add(
                    Anime(
                        id = "mal_$malId",
                        malId = malId,
                        title = title,
                        englishTitle = title,
                        romajiTitle = title,
                        nativeTitle = title,
                        coverImage = cover,
                        bannerImage = cover,
                        synopsis = "Imported from MyAnimeList profile ($trimmed)",
                        rating = if (score > 0) score else 8.0,
                        ageRating = "PG-13",
                        quality = "HD",
                        year = 2024,
                        season = "MAL Import",
                        status = "RELEASING",
                        type = mediaType,
                        totalEpisodes = numEpisodes,
                        genres = "Anime",
                        studio = "MAL",
                        scheduleDay = "Sun",
                        scheduleTime = "20:00",
                        isFavorite = false,
                        isInWatchlist = true,
                        isCompleted = watchStatus == WatchStatus.COMPLETED.name,
                        watchStatus = watchStatus
                    )
                )
            }

            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromAniList(username: String): Result<List<Anime>> = withContext(Dispatchers.IO) {
        try {
            val trimmed = username.trim()
            if (trimmed.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Username cannot be empty"))

            val graphQL = """
                query (${'$'}userName: String) {
                  MediaListCollection(userName: ${'$'}userName, type: ANIME) {
                    lists {
                      name
                      status
                      entries {
                        status
                        score
                        media {
                          id
                          idMal
                          title {
                            romaji
                            english
                            native
                          }
                          coverImage {
                            large
                            extraLarge
                          }
                          bannerImage
                          description
                          averageScore
                          status
                          episodes
                          seasonYear
                          format
                          genres
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", graphQL)
                put("variables", JSONObject().apply {
                    put("userName", trimmed)
                })
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string()
            } ?: return@withContext Result.failure(Exception("Empty response from AniList"))

            val root = JSONObject(body)
            val data = root.optJSONObject("data") ?: return@withContext Result.failure(Exception("User not found or list is private"))
            val collection = data.optJSONObject("MediaListCollection") ?: return@withContext Result.failure(Exception("Empty collection"))
            val listsArray = collection.optJSONArray("lists") ?: return@withContext Result.success(emptyList())

            val list = mutableListOf<Anime>()

            for (i in 0 until listsArray.length()) {
                val listObj = listsArray.getJSONObject(i)
                val entries = listObj.optJSONArray("entries") ?: continue

                for (j in 0 until entries.length()) {
                    val entry = entries.getJSONObject(j)
                    val aniStatus = entry.optString("status", "CURRENT")
                    val media = entry.optJSONObject("media") ?: continue

                    val id = media.getInt("id").toString()
                    val malId = media.optInt("idMal", 0).toString()
                    val titleObj = media.optJSONObject("title")
                    val english = titleObj?.optString("english")?.takeIf { it.isNotEmpty() && it != "null" }
                    val romaji = titleObj?.optString("romaji")?.takeIf { it.isNotEmpty() && it != "null" }
                    val native = titleObj?.optString("native")?.takeIf { it.isNotEmpty() && it != "null" } ?: ""
                    val mainTitle = english ?: romaji ?: native.ifEmpty { "Anime Title" }

                    val coverObj = media.optJSONObject("coverImage")
                    val cover = coverObj?.optString("extraLarge")?.takeIf { it.isNotEmpty() && it != "null" }
                        ?: coverObj?.optString("large")?.takeIf { it.isNotEmpty() && it != "null" } ?: ""

                    val banner = media.optString("bannerImage").takeIf { it.isNotEmpty() && it != "null" } ?: cover
                    val rawDesc = media.optString("description", "").replace(Regex("<[^>]*>"), "")
                    val score = media.optDouble("averageScore", 80.0) / 10.0
                    val mediaStatus = media.optString("status", "RELEASING")
                    val hasEpisodesField = !media.isNull("episodes") && media.has("episodes")
                    val rawEpCount = if (hasEpisodesField) media.optInt("episodes", 0) else 0
                    val epCount = when {
                        mediaStatus.equals("NOT_YET_RELEASED", ignoreCase = true) -> 0
                        rawEpCount > 0 -> rawEpCount
                        mediaStatus.equals("FINISHED", ignoreCase = true) -> 12
                        else -> 0
                    }
                    val year = media.optInt("seasonYear", 2024)
                    val format = media.optString("format", "TV")

                    val genresArray = media.optJSONArray("genres")
                    val genreList = mutableListOf<String>()
                    if (genresArray != null) {
                        for (g in 0 until genresArray.length()) {
                            val genre = genresArray.optString(g, "").trim()
                            if (genre.isNotEmpty()) genreList.add(genre)
                        }
                    }
                    val genresStr = if (genreList.isNotEmpty()) genreList.joinToString(", ") else "Anime"

                    val watchStatus = when (aniStatus) {
                        "CURRENT" -> WatchStatus.WATCHING.name
                        "COMPLETED" -> WatchStatus.COMPLETED.name
                        "PAUSED" -> WatchStatus.ON_HOLD.name
                        "DROPPED" -> WatchStatus.DROPPED.name
                        "PLANNING" -> WatchStatus.PLAN_TO_WATCH.name
                        else -> WatchStatus.WATCHING.name
                    }

                    list.add(
                        Anime(
                            id = id,
                            malId = malId,
                            title = mainTitle,
                            englishTitle = english ?: "",
                            romajiTitle = romaji ?: "",
                            nativeTitle = native,
                            coverImage = cover,
                            bannerImage = banner,
                            synopsis = rawDesc.ifEmpty { "Imported from AniList profile ($trimmed)" },
                            rating = if (score > 0) score else 8.0,
                            ageRating = "PG-13",
                            quality = "HD",
                            year = year,
                            season = "$year",
                            status = mediaStatus,
                            type = format,
                            totalEpisodes = epCount,
                            genres = genresStr,
                            studio = "AniList",
                            scheduleDay = "Sun",
                            scheduleTime = "20:00",
                            isFavorite = false,
                            isInWatchlist = true,
                            isCompleted = watchStatus == WatchStatus.COMPLETED.name,
                            watchStatus = watchStatus
                        )
                    )
                }
            }

            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime")
data class Anime(
    @PrimaryKey val id: String,
    val malId: String = "",
    val title: String,
    val englishTitle: String = "",
    val romajiTitle: String = "",
    val nativeTitle: String = "",
    val coverImage: String,
    val bannerImage: String,
    val synopsis: String,
    val rating: Double = 8.5,
    val ageRating: String = "PG-13",
    val quality: String = "HD",
    val year: Int = 2024,
    val season: String = "Fall 2024",
    val status: String = "Releasing",
    val type: String = "TV",
    val totalEpisodes: Int = 24,
    val genres: String = "Action, Fantasy",
    val studio: String = "MAPPA",
    val scheduleDay: String = "Sun",
    val scheduleTime: String = "19:30",
    val isFavorite: Boolean = false,
    val isInWatchlist: Boolean = false,
    val isCompleted: Boolean = false,
    val nextAiringEpisode: Int? = null,
    val nextAiringTimeUntilSec: Long? = null,
    val nextAiringAtTimestamp: Long? = null,
    val watchStatus: String = "WATCHING",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun displayTitle(preference: String = "English"): String {
        return if (preference.equals("Japanese", ignoreCase = true) || preference.equals("Romaji", ignoreCase = true)) {
            romajiTitle.ifBlank { nativeTitle }.ifBlank { title }
        } else {
            englishTitle.ifBlank { title }.ifBlank { romajiTitle }
        }
    }

    val genreList: List<String>
        get() = genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val isAiring: Boolean
        get() = status.equals("RELEASING", ignoreCase = true) ||
                status.equals("Currently Airing", ignoreCase = true) ||
                status.equals("Airing", ignoreCase = true)

    val isUpcoming: Boolean
        get() = status.equals("NOT_YET_RELEASED", ignoreCase = true) ||
                status.equals("Upcoming", ignoreCase = true) ||
                status.equals("Not Yet Aired", ignoreCase = true) ||
                (nextAiringEpisode == 1 && (nextAiringTimeUntilSec ?: 0L) > 0L)

    /**
     * The number of episodes that have actually aired and are available to watch right now.
     * Never returns max/planned episodes (e.g. 12, 24) for upcoming or currently airing anime.
     */
    val availableEpisodes: Int
        get() {
            if (isUpcoming) return 0
            if (isAiring) {
                val nextEp = nextAiringEpisode
                if (nextEp != null) {
                    val aired = (nextEp - 1).coerceAtLeast(0)
                    return if (totalEpisodes > 0) minOf(aired, totalEpisodes) else aired
                }
                return if (totalEpisodes > 0) totalEpisodes else 0
            }
            if (type.equals("Movie", ignoreCase = true)) return 1
            return totalEpisodes.coerceAtLeast(0)
        }
}

data class AnimeRelation(
    val id: String,
    val title: String,
    val coverImage: String,
    val relationType: String,
    val format: String,
    val isCurrent: Boolean = false,
    /** Consumer-friendly label such as "Season 2" or "Part 3" resolved from the relation chain. */
    val friendlyLabel: String = "",
    /** Position in the franchise, 1-based. 0 when unknown. */
    val seasonNumber: Int = 0
)

enum class WatchStatus(val displayName: String, val key: String) {
    WATCHING("Watching", "watching"),
    COMPLETED("Completed", "completed"),
    ON_HOLD("On Hold", "on_hold"),
    DROPPED("Dropped", "dropped"),
    PLAN_TO_WATCH("Plan to Watch", "plan_to_watch");

    companion object {
        fun fromRaw(raw: String?): WatchStatus {
            val cleaned = raw?.trim()?.uppercase()?.replace("-", "_")?.replace(" ", "_") ?: return WATCHING
            return values().firstOrNull { it.name == cleaned || it.key.uppercase() == cleaned } ?: WATCHING
        }
    }
}

data class MalReview(
    val id: String,
    val author: String,
    val avatarUrl: String = "",
    val score: Int = 8,
    val summary: String = "",
    val body: String,
    val date: String = "",
    val isSpoiler: Boolean = false,
    val isPreliminary: Boolean = false,
    val episodesWatched: Int = 0,
    val helpfulCount: Int = 0,
    val source: String = "MyAnimeList"
)

data class AiringScheduleItem(
    val id: Int,
    val airingAt: Long,
    val timeUntilAiring: Long,
    val episode: Int,
    val anime: Anime
)

data class CharacterVa(
    val id: String,
    val characterName: String,
    val characterImage: String,
    val role: String = "MAIN",
    val vaName: String = "",
    val vaImage: String = "",
    val vaLanguage: String = "Japanese"
)

data class AnimeTheme(
    val id: Int,
    val type: String, // "OP" or "ED"
    val sequence: Int, // 1, 2, ...
    val songTitle: String,
    val artist: String = "",
    val episodes: String = "",
    val videoUrl: String,
    val resolution: String = "1080p"
)

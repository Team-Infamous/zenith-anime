package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgress(
    @PrimaryKey val id: String,
    val animeId: String,
    val episodeNumber: Int,
    val episodeTitle: String,
    val animeTitle: String,
    val coverImage: String,
    val currentTimeSec: Long,
    val durationSec: Long = 1440L,
    val language: String = "sub",
    val completed: Boolean = false,
    val lastWatchedAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationSec > 0) (currentTimeSec.toFloat() / durationSec.toFloat()).coerceIn(0f, 1f) else 0f

    fun formattedCurrentTime(): String {
        val m = currentTimeSec / 60
        val s = currentTimeSec % 60
        return String.format("%02d:%02d", m, s)
    }

    fun formattedDuration(): String {
        val m = durationSec / 60
        val s = durationSec % 60
        return String.format("%02d:%02d", m, s)
    }
}

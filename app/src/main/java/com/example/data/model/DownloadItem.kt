package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey val id: String,
    val animeId: String,
    val animeTitle: String,
    val episodeNumber: Int,
    val episodeTitle: String,
    val thumbnail: String,
    val quality: String = "1080P",
    val fileSize: String = "0 MB",
    val downloadedAt: Long = System.currentTimeMillis(),
    val filePath: String = "",
    val status: String = "COMPLETED", // PENDING, DOWNLOADING, COMPLETED, FAILED
    val progress: Int = 100,
    val streamUrl: String = ""
)

package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episodes")
data class Episode(
    @PrimaryKey val id: String,
    val animeId: String,
    val number: Int,
    val title: String,
    val thumbnail: String,
    val durationSec: Long = 1440L,
    val hasSub: Boolean = true,
    val hasDub: Boolean = true,
    val isOffline: Boolean = false
)

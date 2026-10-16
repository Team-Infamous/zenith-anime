package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val animeThumbnail: String,
    val tag: String = "DOWNLOAD",
    val createdAt: Long = System.currentTimeMillis(),
    val isUnread: Boolean = true
)

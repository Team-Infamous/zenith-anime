package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Anime
import com.example.data.model.AppNotification
import com.example.data.model.DownloadItem
import com.example.data.model.Episode
import com.example.data.model.WatchProgress

@Database(
    entities = [
        Anime::class,
        Episode::class,
        WatchProgress::class,
        DownloadItem::class,
        AppNotification::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun downloadDao(): DownloadDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zenith_anime_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

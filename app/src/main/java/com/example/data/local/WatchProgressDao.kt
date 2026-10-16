package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WatchProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress ORDER BY lastWatchedAt DESC")
    fun getAllProgress(): Flow<List<WatchProgress>>

    @Query("SELECT * FROM watch_progress WHERE completed = 0 ORDER BY lastWatchedAt DESC")
    fun getContinueWatching(): Flow<List<WatchProgress>>

    /**
     * One row per anime: the most recent episode watched or queued, excluding finished series
     * where the latest episode has already been completed and no next episode is queued.
     */
    @Query(
        "SELECT * FROM watch_progress WHERE completed = 0 AND id = (" +
            "SELECT w2.id FROM watch_progress w2 WHERE w2.animeId = watch_progress.animeId " +
            "ORDER BY w2.lastWatchedAt DESC LIMIT 1" +
            ") ORDER BY lastWatchedAt DESC"
    )
    fun getLatestProgressPerAnime(): Flow<List<WatchProgress>>

    @Query("SELECT * FROM watch_progress WHERE id = :id")
    suspend fun getProgressById(id: String): WatchProgress?

    @Query("SELECT * FROM watch_progress WHERE animeId = :animeId ORDER BY lastWatchedAt DESC LIMIT 1")
    suspend fun getLatestProgressForAnime(animeId: String): WatchProgress?

    @Query("SELECT * FROM watch_progress WHERE animeId = :animeId ORDER BY lastWatchedAt DESC LIMIT 1")
    fun observeLatestProgressForAnime(animeId: String): Flow<WatchProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: WatchProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progressList: List<WatchProgress>)

    @Query("DELETE FROM watch_progress WHERE id = :id")
    suspend fun deleteProgress(id: String)

    @Query("DELETE FROM watch_progress WHERE animeId = :animeId")
    suspend fun deleteProgressByAnimeId(animeId: String)

    @Query("DELETE FROM watch_progress")
    suspend fun clearAllHistory()
}

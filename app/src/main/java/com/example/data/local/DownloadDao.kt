package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): DownloadItem?

    @Query("SELECT * FROM downloads WHERE animeId = :animeId AND episodeNumber = :epNumber LIMIT 1")
    suspend fun getDownloadForEpisode(animeId: String, epNumber: Int): DownloadItem?

    @Query("UPDATE downloads SET progress = :progress, status = :status, fileSize = :fileSize WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int, status: String, fileSize: String)

    @Query("UPDATE downloads SET status = :status, filePath = :filePath, fileSize = :fileSize, progress = 100 WHERE id = :id")
    suspend fun markCompleted(id: String, filePath: String, fileSize: String, status: String = "COMPLETED")

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(downloads: List<DownloadItem>)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)

    @Query("DELETE FROM downloads")
    suspend fun clearDownloads()
}

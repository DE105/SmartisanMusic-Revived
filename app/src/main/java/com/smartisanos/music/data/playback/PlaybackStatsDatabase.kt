package com.smartisanos.music.data.playback

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "playback_stats")
internal data class PlaybackStatsEntity(
    @PrimaryKey val mediaId: String,
    val playCount: Long,
    val updatedAt: Long,
)

internal data class PlaybackStatsRow(
    val mediaId: String,
    val playCount: Long,
)

@Dao
internal interface PlaybackStatsDao {

    @Query("SELECT mediaId, playCount FROM playback_stats")
    fun getStats(): List<PlaybackStatsRow>

    @Query("SELECT playCount FROM playback_stats WHERE mediaId = :mediaId")
    suspend fun getPlayCount(mediaId: String): Long?

    @Query(
        """
        UPDATE playback_stats
        SET playCount = playCount + 1,
            updatedAt = :updatedAt
        WHERE mediaId = :mediaId
        """,
    )
    suspend fun incrementExisting(mediaId: String, updatedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PlaybackStatsEntity): Long
}

@Database(
    entities = [PlaybackStatsEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class PlaybackStatsDatabase : RoomDatabase() {
    abstract fun playbackStatsDao(): PlaybackStatsDao

    companion object {
        @Volatile
        private var instance: PlaybackStatsDatabase? = null

        fun getInstance(context: Context): PlaybackStatsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlaybackStatsDatabase::class.java,
                    "playback_stats.db",
                )
                    .build()
                    .also { instance = it }
            }
        }
    }
}

package com.smartisanos.music.data.playback

import android.content.Context
import androidx.room.withTransaction

data class PlaybackStatsRecord(
    val playCount: Long,
    val score: Int,
)

class PlaybackStatsRepository private constructor(
    private val database: PlaybackStatsDatabase,
) {

    private val playbackStatsDao = database.playbackStatsDao()

    fun getStats(): Map<String, PlaybackStatsRecord> {
        return playbackStatsDao.getStats()
            .associate { row ->
                row.mediaId to PlaybackStatsRecord(
                    playCount = row.playCount,
                    score = row.score,
                )
            }
    }

    suspend fun incrementPlayCount(
        mediaId: String,
        updatedAt: Long = System.currentTimeMillis(),
    ): Long? {
        val normalizedMediaId = mediaId.trim()
        if (normalizedMediaId.isEmpty()) {
            return null
        }
        return database.withTransaction {
            if (playbackStatsDao.incrementExisting(normalizedMediaId, updatedAt) == 0) {
                playbackStatsDao.insert(
                    PlaybackStatsEntity(
                        mediaId = normalizedMediaId,
                        playCount = 1L,
                        score = 0,
                        updatedAt = updatedAt,
                    ),
                )
            }
            playbackStatsDao.getPlayCount(normalizedMediaId)
        }
    }

    suspend fun setScore(
        mediaId: String,
        score: Int,
        updatedAt: Long = System.currentTimeMillis(),
    ): Int? {
        val normalizedMediaId = mediaId.trim()
        if (normalizedMediaId.isEmpty()) {
            return null
        }
        val normalizedScore = score.coerceIn(MinScore, MaxScore)
        return database.withTransaction {
            if (playbackStatsDao.updateScore(normalizedMediaId, normalizedScore, updatedAt) == 0) {
                playbackStatsDao.insert(
                    PlaybackStatsEntity(
                        mediaId = normalizedMediaId,
                        playCount = 0L,
                        score = normalizedScore,
                        updatedAt = updatedAt,
                    ),
                )
            }
            playbackStatsDao.getScore(normalizedMediaId)
        }
    }

    companion object {
        @Volatile
        private var instance: PlaybackStatsRepository? = null

        fun getInstance(context: Context): PlaybackStatsRepository {
            return instance ?: synchronized(this) {
                instance ?: PlaybackStatsRepository(
                    PlaybackStatsDatabase.getInstance(context.applicationContext),
                ).also { instance = it }
            }
        }

        internal fun create(database: PlaybackStatsDatabase): PlaybackStatsRepository {
            return PlaybackStatsRepository(database)
        }

        private const val MinScore = 0
        private const val MaxScore = 5
    }
}

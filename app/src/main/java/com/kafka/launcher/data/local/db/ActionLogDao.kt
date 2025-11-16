package com.kafka.launcher.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kafka.launcher.domain.model.ActionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionLogDao {
    @Insert
    suspend fun insert(log: ActionLog)

    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<ActionLog>>

    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentSnapshot(limit: Int): List<ActionLog>

    @Query("SELECT actionId, COUNT(*) AS count FROM action_logs GROUP BY actionId ORDER BY count DESC LIMIT :limit")
    fun stats(limit: Int): Flow<List<ActionStatEntity>>

    @Query("SELECT actionId, COUNT(*) AS count FROM action_logs GROUP BY actionId ORDER BY count DESC LIMIT :limit")
    suspend fun statsSnapshot(limit: Int): List<ActionStatEntity>
}

data class ActionStatEntity(
    val actionId: String,
    val count: Long
)

package com.kafka.launcher.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kafka.launcher.domain.model.AiModel
import com.kafka.launcher.domain.model.AiModelStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AiModelDao {
    @Query("SELECT * FROM ai_models")
    fun getAllModels(): Flow<List<AiModel>>

    @Query("SELECT * FROM ai_models WHERE status = :status")
    fun getModelsByStatus(status: AiModelStatus): Flow<List<AiModel>>

    @Query("SELECT * FROM ai_models WHERE id = :id")
    suspend fun getModelById(id: String): AiModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: AiModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertModels(models: List<AiModel>)

    @Update
    suspend fun updateModel(model: AiModel)

    @Query("DELETE FROM ai_models WHERE id = :id")
    suspend fun deleteModelById(id: String)
}

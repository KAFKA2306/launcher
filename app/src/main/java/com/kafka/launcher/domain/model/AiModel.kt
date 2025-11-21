package com.kafka.launcher.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AiModelStatus {
    CANDIDATE, // Kouho
    ADOPTED,   // Saiyouzumi
    REJECTED   // Hisaiyou
}

@Entity(tableName = "ai_models")
data class AiModel(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val version: String,
    val downloadUrl: String,
    val status: AiModelStatus = AiModelStatus.CANDIDATE,
    val localPath: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

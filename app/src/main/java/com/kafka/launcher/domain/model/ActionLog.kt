package com.kafka.launcher.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_logs")
data class ActionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionId: String,
    val timestamp: Long
)

data class ActionStats(
    val actionId: String,
    val count: Long
)

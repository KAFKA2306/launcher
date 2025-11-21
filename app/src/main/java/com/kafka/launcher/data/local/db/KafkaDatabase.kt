package com.kafka.launcher.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kafka.launcher.domain.model.ActionLog
import com.kafka.launcher.domain.model.AiModel

@Database(entities = [ActionLog::class, AiModel::class], version = 2, exportSchema = false)
abstract class KafkaDatabase : RoomDatabase() {
    abstract fun actionLogDao(): ActionLogDao
    abstract fun aiModelDao(): AiModelDao

    companion object {
        fun build(context: Context): KafkaDatabase = Room.databaseBuilder(
            context.applicationContext,
            KafkaDatabase::class.java,
            "kafka_launcher.db"
        ).build()
    }
}

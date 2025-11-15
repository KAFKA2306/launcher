package com.kafka.launcher.data.repo

import com.kafka.launcher.data.local.db.ActionLogDao
import com.kafka.launcher.data.log.ActionLogFileWriter
import com.kafka.launcher.domain.model.ActionLog
import com.kafka.launcher.domain.model.ActionStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class ActionLogRepository(
    private val dao: ActionLogDao,
    private val fileWriter: ActionLogFileWriter
) {
    suspend fun log(actionId: String) {
        val log = ActionLog(actionId = actionId, timestamp = System.currentTimeMillis())
        dao.insert(log)
        fileWriter.append(log)
    }

    fun recent(limit: Int): Flow<List<ActionLog>> = dao.recent(limit).onEach { logs ->
        fileWriter.writeRecent(logs)
    }

    fun stats(limit: Int): Flow<List<ActionStats>> = dao.stats(limit).map { entries ->
        val stats = entries.map { ActionStats(it.actionId, it.count) }
        fileWriter.writeStats(stats)
        stats
    }
}

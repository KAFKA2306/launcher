package com.kafka.launcher.data.repo

import com.kafka.launcher.data.local.db.ActionLogDao
import com.kafka.launcher.domain.model.ActionLog
import com.kafka.launcher.domain.model.ActionStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ActionLogRepository(private val dao: ActionLogDao) {
    suspend fun log(actionId: String) {
        dao.insert(ActionLog(actionId = actionId, timestamp = System.currentTimeMillis()))
    }

    fun recent(limit: Int): Flow<List<ActionLog>> = dao.recent(limit)

    fun stats(limit: Int): Flow<List<ActionStats>> = dao.stats(limit).map { entries ->
        entries.map { ActionStats(it.actionId, it.count) }
    }
}

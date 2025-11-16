package com.kafka.launcher.launcher.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kafka.launcher.config.GeminiConfig
import java.util.concurrent.TimeUnit

object GeminiWorkScheduler {
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(GeminiConfig.networkType)
            .build()
        val request = PeriodicWorkRequestBuilder<GeminiSyncWorker>(GeminiConfig.periodHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            GeminiConfig.workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun refreshNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(GeminiConfig.networkType)
            .build()
        val request = OneTimeWorkRequestBuilder<GeminiSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            GeminiConfig.manualWorkName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

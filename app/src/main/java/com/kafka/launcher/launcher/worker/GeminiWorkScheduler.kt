package com.kafka.launcher.launcher.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
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
}

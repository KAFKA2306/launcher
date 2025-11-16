package com.kafka.launcher.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kafka.launcher.launcher.worker.GeminiWorkScheduler

class LauncherInitReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        GeminiWorkScheduler.schedule(context.applicationContext)
    }
}

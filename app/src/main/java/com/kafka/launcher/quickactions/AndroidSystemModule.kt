package com.kafka.launcher.quickactions

import android.content.Context
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.QuickAction

class AndroidSystemModule : QuickActionProvider {
    override val id: String = "android_system"

    override fun actions(context: Context): List<QuickAction> = listOf(
        QuickAction(
            id = "android_phone_dial",
            providerId = id,
            label = "電話",
            actionType = ActionType.PHONE_DIAL,
            priority = 2
        ),
        QuickAction(
            id = "android_sms_compose",
            providerId = id,
            label = "SMS",
            actionType = ActionType.SMS_COMPOSE,
            priority = 2
        ),
        QuickAction(
            id = "android_camera_still",
            providerId = id,
            label = "カメラ",
            actionType = ActionType.CAMERA_STILL,
            priority = 1
        ),
        QuickAction(
            id = "android_alarm_list",
            providerId = id,
            label = "アラーム",
            actionType = ActionType.ALARM_LIST,
            priority = 1
        ),
        QuickAction(
            id = "android_system_settings",
            providerId = id,
            label = "端末設定",
            actionType = ActionType.SYSTEM_SETTINGS,
            priority = 1
        )
    )
}

package com.kafka.launcher.quickactions

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kafka.launcher.domain.model.ActionStats
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.usecase.RecommendActionsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AndroidSystemModuleTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val module = AndroidSystemModule()

    @Test
    fun exposesDistinctCommonActionsWithoutPrivilegedOperations() {
        val actions = module.actions(context)

        assertEquals(5, actions.size)
        assertEquals(actions.size, actions.map { it.id }.distinct().size)
        assertEquals(
            setOf(
                ActionType.PHONE_DIAL,
                ActionType.SMS_COMPOSE,
                ActionType.CAMERA_STILL,
                ActionType.ALARM_LIST,
                ActionType.SYSTEM_SETTINGS
            ),
            actions.map { it.actionType }.toSet()
        )
        assertTrue(actions.all { it.providerId == "android_system" })
    }

    @Test
    fun usageStatsCanRecommendNewSystemActions() {
        val actions = module.actions(context)
        val recommended = RecommendActionsUseCase()(
            actions = actions,
            stats = listOf(
                ActionStats(actionId = "android_sms_compose", count = 8),
                ActionStats(actionId = "android_camera_still", count = 3)
            )
        )

        assertEquals(
            listOf("android_sms_compose", "android_camera_still"),
            recommended.map { it.id }
        )
    }
}

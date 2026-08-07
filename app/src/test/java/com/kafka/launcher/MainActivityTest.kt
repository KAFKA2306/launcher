package com.kafka.launcher

import android.content.Intent
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class MainActivityTest {
    @Test
    fun testMainActivityCreation() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        scenario.use {
            assert(it.state.isAtLeast(androidx.lifecycle.Lifecycle.State.CREATED))
        }
    }
}

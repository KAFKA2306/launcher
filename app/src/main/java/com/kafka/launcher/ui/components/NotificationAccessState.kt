package com.kafka.launcher.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kafka.launcher.util.hasNotificationListenerAccess

@Composable
fun rememberNotificationAccessState(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(context.hasNotificationListenerAccess()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestContext = rememberUpdatedState(context)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.value = latestContext.value.hasNotificationListenerAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return state
}

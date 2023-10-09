package com.jeremy.thunder

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun OnLifecycleEvent(
    onEvent: (
        owner: LifecycleOwner,
        event: Lifecycle.Event,
    ) -> Unit
) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(owner, event)
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun format(milliseconds: Long): String {
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(milliseconds),
        ZoneOffset.systemDefault()
    ).format(DateTimeFormatter.ISO_DATE)
}

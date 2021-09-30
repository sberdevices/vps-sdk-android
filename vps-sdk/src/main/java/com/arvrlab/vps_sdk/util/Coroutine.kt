package com.arvrlab.vps_sdk.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

internal inline fun CoroutineScope.waitIfNeedAsync(
    conditional: () -> Boolean,
    timeMillis: Long
): Deferred<Unit>? =
    if (conditional()) {
        async { delay(timeMillis) }
    } else {
        null
    }
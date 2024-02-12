package net.yukulab.fabsit.extension

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

suspend fun loop(duration: Duration = 50.milliseconds, block: suspend () -> Boolean) {
    while (true) {
        if (!block()) {
            break
        }
        delay(duration)
    }
}

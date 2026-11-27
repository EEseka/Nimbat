package com.eeseka.nimbat.core.data.audio.util

import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.timeout
import kotlin.time.Duration.Companion.seconds

private val STALL_TIMEOUT = 2.seconds
private val RESTART_DELAY = 3.seconds
private const val MAX_RESTARTS = 2

/** Restarts a recording that goes quiet for good (e.g. Siri took the mic) twice, then fails as unavailable. */
@OptIn(FlowPreview::class)
internal fun <T> Flow<T>.restartWhenStalled(): Flow<T> = this
    .timeout(STALL_TIMEOUT)
    .retryWhen { cause, attempt ->
        val canRestart = cause is TimeoutCancellationException && attempt < MAX_RESTARTS
        if (canRestart) delay(RESTART_DELAY)
        canRestart
    }
    .catch { error ->
        val isStalled = error is TimeoutCancellationException
        throw if (isStalled) AudioErrorException(AudioError.MICROPHONE_UNAVAILABLE) else error
    }

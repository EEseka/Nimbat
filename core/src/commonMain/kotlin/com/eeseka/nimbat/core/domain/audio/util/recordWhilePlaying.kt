package com.eeseka.nimbat.core.domain.audio.util

import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.util.EmptyResult
import com.eeseka.nimbat.core.domain.util.Result
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private val MIC_WARM_UP = 500.milliseconds
private val LISTEN_AFTER = 300.milliseconds

/** Records from [MIC_WARM_UP] before [sound] plays until [LISTEN_AFTER] after it. */
suspend fun AudioEngine.recordWhilePlaying(sound: FloatArray): Result<FloatArray, AudioError> {
    var played: EmptyResult<AudioError> = Result.Success(Unit)
    var isDone = false
    val blocks = try {
        coroutineScope {
            launch {
                delay(MIC_WARM_UP)
                played = play(sound)
                if (played is Result.Success) delay(LISTEN_AFTER)
                isDone = true
            }
            microphone().takeWhile { !isDone }.toList()
        }
    } catch (e: AudioErrorException) {
        return Result.Failure(e.error)
    }
    (played as? Result.Failure)?.let { return it }

    val samples = FloatArray(blocks.sumOf { it.size })
    var offset = 0
    blocks.forEach { block ->
        block.copyInto(samples, destinationOffset = offset)
        offset += block.size
    }
    return Result.Success(samples)
}

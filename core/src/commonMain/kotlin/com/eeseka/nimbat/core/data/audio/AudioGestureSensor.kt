package com.eeseka.nimbat.core.data.audio

import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.audio.GestureSensor
import com.eeseka.nimbat.core.domain.audio.util.inBlocksOf
import com.eeseka.nimbat.core.domain.audio.util.recordWhileLooping
import com.eeseka.nimbat.core.domain.settings.AppPreferences
import com.eeseka.nimbat.sound.DOPPLER_BLOCK_SIZE
import com.eeseka.nimbat.sound.DOPPLER_HOP
import com.eeseka.nimbat.sound.GestureDetector
import com.eeseka.nimbat.sound.GestureReading
import com.eeseka.nimbat.sound.dopplerTone
import com.eeseka.nimbat.sound.motion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.timeout
import kotlin.time.Duration.Companion.seconds

class AudioGestureSensor(
    private val audioEngine: AudioEngine,
    private val appPreferences: AppPreferences,
    private val soundDispatcher: CoroutineDispatcher = Dispatchers.Default
) : GestureSensor {

    private companion object {
        val STALL_TIMEOUT = 2.seconds
        val RESTART_DELAY = 3.seconds
        const val MAX_RESTARTS = 2
    }

    @OptIn(FlowPreview::class)
    override fun readings(): Flow<GestureReading> = flow {
        val temperatureC = appPreferences.roomTemperatureCelsius.first()
        val detector = GestureDetector()
        emitAll(
            audioEngine.recordWhileLooping(dopplerTone())
                .inBlocksOf(size = DOPPLER_BLOCK_SIZE, step = DOPPLER_HOP)
                .map { block -> detector.read(motion(block, temperatureC)) }
                .flowOn(soundDispatcher)
        )
    }
        .timeout(STALL_TIMEOUT)
        .retryWhen { cause, attempt ->
            val canRestart = cause is TimeoutCancellationException && attempt < MAX_RESTARTS
            if (canRestart) delay(RESTART_DELAY)
            canRestart
        }
        .catch { error ->
            throw if (error is TimeoutCancellationException) AudioErrorException(AudioError.MICROPHONE_UNAVAILABLE) else error
        }
}

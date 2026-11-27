package com.eeseka.nimbat.core.data.audio

import com.eeseka.nimbat.core.data.audio.util.restartWhenStalled
import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.MovementSensor
import com.eeseka.nimbat.core.domain.audio.util.inBlocksOf
import com.eeseka.nimbat.core.domain.audio.util.recordWhileLooping
import com.eeseka.nimbat.core.domain.settings.AppPreferences
import com.eeseka.nimbat.sound.DOPPLER_BLOCK_SIZE
import com.eeseka.nimbat.sound.DOPPLER_HOP
import com.eeseka.nimbat.sound.MovementDetector
import com.eeseka.nimbat.sound.WatchRange
import com.eeseka.nimbat.sound.WatchReading
import com.eeseka.nimbat.sound.dopplerTone
import com.eeseka.nimbat.sound.motion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AudioMovementSensor(
    private val audioEngine: AudioEngine,
    private val appPreferences: AppPreferences,
    private val soundDispatcher: CoroutineDispatcher = Dispatchers.Default
) : MovementSensor {

    override fun readings(range: WatchRange): Flow<WatchReading> = flow {
        val temperatureC = appPreferences.roomTemperatureCelsius.first()
        val detector = MovementDetector(range)
        emitAll(
            audioEngine.recordWhileLooping(dopplerTone())
                .inBlocksOf(size = DOPPLER_BLOCK_SIZE, step = DOPPLER_HOP)
                .map { block -> detector.read(motion(block, temperatureC)) }
                .flowOn(soundDispatcher)
        )
    }.restartWhenStalled()
}

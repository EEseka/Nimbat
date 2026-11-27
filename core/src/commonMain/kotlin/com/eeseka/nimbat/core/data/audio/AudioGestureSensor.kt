package com.eeseka.nimbat.core.data.audio

import com.eeseka.nimbat.core.data.audio.util.restartWhenStalled
import com.eeseka.nimbat.core.domain.audio.AudioEngine
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AudioGestureSensor(
    private val audioEngine: AudioEngine,
    private val appPreferences: AppPreferences,
    private val soundDispatcher: CoroutineDispatcher = Dispatchers.Default
) : GestureSensor {

    override fun readings(): Flow<GestureReading> = flow {
        val temperatureC = appPreferences.roomTemperatureCelsius.first()
        val detector = GestureDetector()
        emitAll(
            audioEngine.recordWhileLooping(dopplerTone())
                .inBlocksOf(size = DOPPLER_BLOCK_SIZE, step = DOPPLER_HOP)
                .map { block -> detector.read(motion(block, temperatureC)) }
                .flowOn(soundDispatcher)
        )
    }.restartWhenStalled()
}

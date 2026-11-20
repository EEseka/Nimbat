package com.eeseka.nimbat.core.domain.audio.util

import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.util.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/** What the mic hears while the speaker loops [samples], from the moment the mic is live until you stop collecting. */
fun AudioEngine.recordWhileLooping(samples: FloatArray): Flow<FloatArray> = channelFlow {
    var isLooping = false
    microphone().collect { block ->
        if (!isLooping) {
            isLooping = true
            // loop() only returns if the speaker fails; throwing stops the microphone too
            launch { loop(samples).onFailure { throw AudioErrorException(it) } }
        }
        send(block)
    }
}

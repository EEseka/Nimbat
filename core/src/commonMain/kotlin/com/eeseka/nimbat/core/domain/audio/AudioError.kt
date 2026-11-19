package com.eeseka.nimbat.core.domain.audio

import com.eeseka.nimbat.core.domain.util.Error

enum class AudioError : Error {
    MICROPHONE_UNAVAILABLE,
    SPEAKER_UNAVAILABLE,
    SAMPLE_RATE_UNSUPPORTED
}

package com.eeseka.nimbat.core.presentation.audio.mappers

import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.presentation.util.UiText
import nimbat.core.generated.resources.Res
import nimbat.core.generated.resources.audio_error_microphone_unavailable
import nimbat.core.generated.resources.audio_error_sample_rate_unsupported
import nimbat.core.generated.resources.audio_error_speaker_unavailable

fun AudioError.toUiText(): UiText = when (this) {
    AudioError.MICROPHONE_UNAVAILABLE -> UiText.Resource(Res.string.audio_error_microphone_unavailable)
    AudioError.SPEAKER_UNAVAILABLE -> UiText.Resource(Res.string.audio_error_speaker_unavailable)
    AudioError.SAMPLE_RATE_UNSUPPORTED -> UiText.Resource(Res.string.audio_error_sample_rate_unsupported)
}

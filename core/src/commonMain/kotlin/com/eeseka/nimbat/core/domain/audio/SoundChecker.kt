package com.eeseka.nimbat.core.domain.audio

import kotlinx.coroutines.flow.Flow

interface SoundChecker {
    /** Plays the steady tone, then (if [includeChirps]) the distance chirps, and listens for both; ends with [SoundCheckProgress.Finished] or [SoundCheckProgress.Failed]. */
    fun check(includeChirps: Boolean): Flow<SoundCheckProgress>
}

sealed interface SoundCheckProgress {
    data class Listening(val clarity: Float) : SoundCheckProgress
    data class Finished(val isHeard: Boolean) : SoundCheckProgress
    data class Failed(val error: AudioError) : SoundCheckProgress
}

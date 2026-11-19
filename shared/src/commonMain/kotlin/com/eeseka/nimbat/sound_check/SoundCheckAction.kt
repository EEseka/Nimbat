package com.eeseka.nimbat.sound_check

import com.eeseka.nimbat.core.presentation.permissions.PermissionState

sealed interface SoundCheckAction {
    data object OnChirpCheckClick : SoundCheckAction
    data object OnMessageCheckClick : SoundCheckAction
    data object OnMotionCheckClick : SoundCheckAction
    data class OnMicrophonePermissionResolved(val state: PermissionState) : SoundCheckAction
}

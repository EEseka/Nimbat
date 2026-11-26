package com.eeseka.nimbat.wave.presentation

import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.sound.GestureDirection
import com.eeseka.nimbat.wave.presentation.model.BatDash
import com.eeseka.nimbat.wave.presentation.model.WaveDemo
import com.eeseka.nimbat.wave.presentation.model.WaveSensorStatus

data class WaveState(
    val microphonePermission: PermissionState? = null,
    val isRequestingMicrophone: Boolean = false,

    val sensorStatus: WaveSensorStatus = WaveSensorStatus.CALIBRATING,
    val calibrationProgress: Float = 0f,
    val isToneClear: Boolean = true,
    val towardLevel: Float = 0f,
    val awayLevel: Float = 0f,

    val lastGesture: GestureDirection? = null,
    val gestureCount: Int = 0,

    val demo: WaveDemo = WaveDemo.SLIDES,
    val slideIndex: Int = 0,
    val animalIndex: Int = 0,
    val batDash: BatDash = BatDash()
)

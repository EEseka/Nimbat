package com.eeseka.nimbat.guard.presentation

import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.guard.model.GuardPhase
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.guard.presentation.model.GuardSessionUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class GuardState(
    val microphonePermission: PermissionState? = null,
    val isRequestingMicrophone: Boolean = false,

    val phase: GuardPhase = GuardPhase.IDLE,
    val mode: GuardMode = GuardMode.ALARM,
    val locksComputer: Boolean = false,
    val canLockComputer: Boolean = false,

    val countdownSeconds: Int = 0,
    val calibrationProgress: Float = 0f,
    val isToneClear: Boolean = true,
    val movementLevel: Float = 0f,
    val movementCount: Int = 0,

    val sessions: ImmutableList<GuardSessionUi> = persistentListOf(),
    val showClearLogDialog: Boolean = false
)

package com.eeseka.nimbat.core.domain.device.validation

import com.eeseka.nimbat.core.domain.device.DeviceConstants.MAX_NAME_LENGTH

object DeviceNameValidator {

    fun validate(name: String): DeviceNameValidationState {
        val trimmed = name.trim()
        return when {
            trimmed.isBlank() -> DeviceNameValidationState.BLANK
            trimmed.length > MAX_NAME_LENGTH -> DeviceNameValidationState.TOO_LONG
            else -> DeviceNameValidationState.VALID
        }
    }
}

enum class DeviceNameValidationState {
    VALID,
    BLANK,
    TOO_LONG
}

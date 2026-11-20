package com.eeseka.nimbat.core.presentation.device.mappers

import com.eeseka.nimbat.core.domain.device.validation.DeviceNameValidationState
import com.eeseka.nimbat.core.presentation.util.UiText
import nimbat.core.generated.resources.Res
import nimbat.core.generated.resources.error_device_name_blank
import nimbat.core.generated.resources.error_device_name_too_long

fun DeviceNameValidationState.toUiText(): UiText? = when (this) {
    DeviceNameValidationState.BLANK -> UiText.Resource(Res.string.error_device_name_blank)
    DeviceNameValidationState.TOO_LONG -> UiText.Resource(Res.string.error_device_name_too_long)
    DeviceNameValidationState.VALID -> null
}

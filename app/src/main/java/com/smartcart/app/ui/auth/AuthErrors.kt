package com.smartcart.app.ui.auth

import com.smartcart.app.R
import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode

internal fun Throwable.authErrorMessage(): Int = when ((this as? AppException)?.code) {
    ErrorCode.INVALID_NAME -> R.string.auth_invalid_name
    ErrorCode.INVALID_EMAIL -> R.string.auth_invalid_email
    ErrorCode.WEAK_PASSWORD -> R.string.auth_short_password
    ErrorCode.WRONG_CREDENTIALS -> R.string.auth_wrong_credentials
    ErrorCode.EMAIL_IN_USE -> R.string.auth_email_in_use
    ErrorCode.TOO_MANY_ATTEMPTS -> R.string.auth_rate_limit
    ErrorCode.NETWORK_ERROR -> R.string.auth_network_error
    else -> R.string.auth_unknown_error
}

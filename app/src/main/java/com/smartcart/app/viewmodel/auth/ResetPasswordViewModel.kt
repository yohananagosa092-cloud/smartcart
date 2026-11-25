package com.smartcart.app.viewmodel.auth

class ResetPasswordViewModel : AuthFormViewModel<Unit>() {
    fun reset(email: String) = runRequest { repository.resetPassword(email) }
}

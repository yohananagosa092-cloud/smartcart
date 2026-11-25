package com.smartcart.app.viewmodel.auth

import com.smartcart.app.data.auth.AuthUser

class SignInViewModel : AuthFormViewModel<AuthUser>() {
    fun restoreSession() {
        if (outcome.value == null && busy.value != true) {
            val user = repository.currentUser() ?: return
            runRequest { Result.success(user) }
        }
    }

    fun signIn(email: String, password: String) {
        runRequest { repository.signIn(email, password) }
    }
}

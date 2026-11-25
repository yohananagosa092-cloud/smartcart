package com.smartcart.app.viewmodel.auth

import com.smartcart.app.data.auth.AuthRepository
import com.smartcart.app.data.auth.AuthUser

class CreateAccountViewModel : AuthFormViewModel<AuthUser>() {
    fun register(name: String, email: String, password: String, acceptedTerms: Boolean) {
        if (!acceptedTerms) return
        runRequest { repository.signUp(name, email, password) }
    }

    fun passwordStrength(password: String): Int = when {
        password.isEmpty() -> 0
        password.length < AuthRepository.MIN_PASSWORD_LENGTH -> 1
        password.length >= 12 && password.any(Char::isLetter) && password.any { !it.isLetter() } -> 3
        else -> 2
    }
}

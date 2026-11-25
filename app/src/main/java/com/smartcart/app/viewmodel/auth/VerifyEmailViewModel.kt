package com.smartcart.app.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VerifyEmailViewModel : AuthFormViewModel<VerifyOutcome>() {
    private val _remaining = MutableLiveData(0L)
    val remaining: LiveData<Long> = _remaining

    init {
        viewModelScope.launch {
            while (true) {
                val seconds = ((repository.resendAvailableAt() - System.currentTimeMillis() + 999) / 1000)
                    .coerceAtLeast(0)
                if (_remaining.value != seconds) _remaining.value = seconds
                delay(500)
            }
        }
    }

    fun maskedEmail(): String {
        val email = repository.currentUser()?.email.orEmpty()
        val separator = email.lastIndexOf('@')
        if (separator <= 0) return "•••"
        return "${email.first()}•••${email.substring(separator)}"
    }

    fun verify() = runRequest {
        repository.checkEmailVerified().map { if (it) VerifyOutcome.VERIFIED else VerifyOutcome.PENDING }
    }

    fun resend() {
        if (repository.resendAvailableAt() > System.currentTimeMillis()) return
        runRequest { repository.resendVerification().map { VerifyOutcome.SENT } }
    }

    fun changeEmail() = runRequest {
        repository.deleteUnverifiedAccount().map { if (it) VerifyOutcome.DELETED else VerifyOutcome.VERIFIED }
    }
}

enum class VerifyOutcome { VERIFIED, PENDING, SENT, DELETED }

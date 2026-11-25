package com.smartcart.app.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcart.app.data.ServiceLocator
import kotlinx.coroutines.launch

abstract class AuthFormViewModel<T> : ViewModel() {
    protected val repository get() = ServiceLocator.authRepository

    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    private val _outcome = MutableLiveData<Result<T>?>()
    val outcome: LiveData<Result<T>?> = _outcome

    fun consumeOutcome() {
        _outcome.value = null
    }

    protected fun runRequest(request: suspend () -> Result<T>) {
        if (_busy.value == true) return
        _busy.value = true
        viewModelScope.launch {
            val result = request()
            _busy.value = false
            _outcome.value = result
        }
    }
}

package com.smartcart.app.viewmodel.lists

import androidx.lifecycle.ViewModel
import com.smartcart.app.data.ServiceLocator

class HomeViewModel : ViewModel() {
    fun displayName(): String = ServiceLocator.authRepository.currentUser()?.displayName.orEmpty()

    suspend fun ownedLists() = ServiceLocator.listRepository.ownedLists()
}

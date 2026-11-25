package com.smartcart.app.viewmodel.lists

import androidx.lifecycle.ViewModel
import com.smartcart.app.data.ServiceLocator

class NewListViewModel : ViewModel() {
    suspend fun createList(name: String) = ServiceLocator.listRepository.createList(name)
}

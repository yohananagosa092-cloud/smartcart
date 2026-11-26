package com.smartcart.app.viewmodel.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcart.app.data.ServiceLocator
import com.smartcart.app.data.model.Product
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddProductsViewModel : ViewModel() {
    data class Results(val products: List<Product> = emptyList(), val loading: Boolean = false,
        val empty: Boolean = false, val failed: Boolean = false)
    private val products = ServiceLocator.productRepository
    private val items = ServiceLocator.itemRepository
    private var lookup: Job? = null
    private val _results = MutableStateFlow(Results())
    val results = _results.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    fun search(query: String) {
        lookup?.cancel()
        _results.value = Results()
        if (query.trim().length < 2) return
        lookup = viewModelScope.launch {
            delay(250)
            _results.value = Results(loading = true)
            show(products.search(query))
        }
    }

    private fun show(result: Result<List<Product>>) {
        _results.value = result.fold(
            onSuccess = { Results(products = it, empty = it.isEmpty()) },
            onFailure = { Results(failed = true) },
        )
    }

    suspend fun add(listId: String, product: Product): Boolean =
        save { items.addProduct(listId, product).isSuccess }

    suspend fun addFree(listId: String, name: String, quantity: Int, note: String): Boolean =
        save { items.addFreeItem(listId, name, quantity, note).isSuccess }

    private suspend fun save(write: suspend () -> Boolean): Boolean {
        if (_saving.value) return false
        _saving.value = true
        return try { write() } finally { _saving.value = false }
    }
}

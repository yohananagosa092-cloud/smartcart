package com.smartcart.app.viewmodel.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcart.app.data.ServiceLocator
import com.smartcart.app.data.model.ListItem
import com.smartcart.app.data.model.ShoppingList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class ListEditorViewModel : ViewModel() {
    data class State(val list: ShoppingList?, val items: List<ListItem>, val owner: Boolean)
    private val lists = ServiceLocator.listRepository
    private val items = ServiceLocator.itemRepository
    private val sharing = ServiceLocator.sharingRepository
    private val _state = MutableStateFlow<State?>(null)
    val state = _state.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed = _failed.asStateFlow()
    private val _itemsError = MutableStateFlow<Throwable?>(null)
    val itemsError = _itemsError.asStateFlow()
    private var observation: Job? = null

    fun observe(id: String) {
        observation?.cancel()
        _failed.value = false
        _itemsError.value = null
        val uid = ServiceLocator.authRepository.currentUser()?.uid
        observation = viewModelScope.launch {
            val entriesFlow = items.observeItems(id).onStart { emit(emptyList()) }
                .catch { _itemsError.value = it }
            combine(lists.observeList(id), entriesFlow) { list, entries ->
                State(list, entries, uid != null && list?.ownerUid == uid)
            }.catch { _failed.value = true }.collect { _state.value = it }
        }
    }

    suspend fun quantity(id: String, item: ListItem, quantity: Int) =
        items.changeQuantity(id, item, quantity).isSuccess

    suspend fun note(id: String, item: ListItem, note: String) =
        items.changeNote(id, item, note).isSuccess

    suspend fun remove(id: String, item: ListItem) = items.removeItem(id, item.id).isSuccess

    suspend fun rename(id: String, name: String) = lists.renameList(id, name).isSuccess

    suspend fun delete(id: String) = lists.deleteList(id).getOrDefault(false)

    suspend fun members(id: String) = sharing.members(id)
}

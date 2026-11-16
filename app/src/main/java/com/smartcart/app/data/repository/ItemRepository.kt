package com.smartcart.app.data.repository

import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import com.smartcart.app.data.model.ListItem
import com.smartcart.app.data.model.Product
import com.smartcart.app.data.safeCall
import com.smartcart.app.data.source.DataSource
import com.smartcart.app.data.source.requireUid

/** הפריטים ברשימה (מסכים 07, 08). */
class ItemRepository(private val dataSource: DataSource) {

    suspend fun items(listId: String): Result<List<ListItem>> = safeCall { dataSource.getItems(listId) }

    /**
     * לחיצה על + במסך 07: מוסיפה את המוצר בכמות 1,
     * או מגדילה את הכמות אם המוצר כבר ברשימה.
     */
    suspend fun addProduct(listId: String, product: Product): Result<ListItem> = safeCall {
        val existing = dataSource.getItems(listId).firstOrNull { it.barcode == product.barcode }
        if (existing != null) {
            val updated = existing.copy(quantity = validQuantity(existing.quantity + 1))
            dataSource.updateItem(listId, updated)
            updated
        } else {
            val now = System.currentTimeMillis()
            val newItem = ListItem(
                id = "", // מקור הנתונים נותן את המזהה
                barcode = product.barcode,
                name = product.name,
                quantity = 1,
                addedBy = dataSource.requireUid(),
                addedAt = now,
                updatedAt = now,
            )
            dataSource.addItem(listId, newItem)
        }
    }

    /** בורר הכמות במסך 08. */
    suspend fun changeQuantity(listId: String, item: ListItem, quantity: Int): Result<ListItem> = safeCall {
        val updated = item.copy(quantity = validQuantity(quantity))
        dataSource.updateItem(listId, updated)
        updated
    }

    suspend fun changeNote(listId: String, item: ListItem, note: String): Result<ListItem> = safeCall {
        val updated = item.copy(note = note.trim())
        dataSource.updateItem(listId, updated)
        updated
    }

    suspend fun removeItem(listId: String, itemId: String): Result<Unit> = safeCall {
        dataSource.deleteItem(listId, itemId)
    }

    /** כמות: 1 עד 999. */
    private fun validQuantity(quantity: Int): Int =
        quantity.also { if (it !in 1..999) throw AppException(ErrorCode.INVALID_QUANTITY) }
}

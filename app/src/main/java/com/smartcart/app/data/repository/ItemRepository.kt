package com.smartcart.app.data.repository

import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import com.smartcart.app.data.model.ListItem
import com.smartcart.app.data.model.Product
import com.smartcart.app.data.safeCall
import com.smartcart.app.data.source.DataSource
import com.smartcart.app.data.source.requireUid
import kotlinx.coroutines.flow.Flow

/** הפריטים ברשימה (מסכים 07, 08). */
class ItemRepository(private val dataSource: DataSource) {

    suspend fun addFreeItem(listId: String, name: String, quantity: Int = 1, note: String = ""): Result<ListItem> = safeCall {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw AppException(ErrorCode.INVALID_NAME)
        val now = System.currentTimeMillis()
        dataSource.addItem(listId, ListItem(
            id = "", barcode = "", name = trimmed, quantity = validQuantity(quantity), note = note.trim(),
            addedBy = dataSource.requireUid(), addedAt = now, updatedAt = now,
        ))
    }

    suspend fun items(listId: String): Result<List<ListItem>> = safeCall { dataSource.getItems(listId) }

    /**
     * משימה 21: הפריטים ברשימה, ואחר כך רשימה מעודכנת בכל שינוי, גם כשמשתתפת אחרת משנה.
     * מסך 08 מאזין לזה ומתעדכן לבד. שגיאה (למשל אין הרשאה) מגיעה דרך ה-Flow עצמו.
     */
    fun observeItems(listId: String): Flow<List<ListItem>> = dataSource.observeItems(listId)

    /**
     * לחיצה על + במסך 07: מוסיפה את המוצר בכמות 1,
     * או מגדילה את הכמות אם המוצר כבר ברשימה.
     */
    suspend fun addProduct(listId: String, product: Product): Result<ListItem> = safeCall {
        val now = System.currentTimeMillis()
        dataSource.addCatalogItem(listId, ListItem(
            id = "", barcode = product.barcode, name = product.name, quantity = 1,
            addedBy = dataSource.requireUid(), addedAt = now, updatedAt = now,
        ))
    }

    /** בורר הכמות במסך 08. */
    suspend fun changeQuantity(listId: String, item: ListItem, quantity: Int): Result<ListItem> = safeCall {
        val updated = item.copy(quantity = validQuantity(quantity))
        dataSource.updateItemQuantity(listId, updated)
        updated
    }

    suspend fun changeNote(listId: String, item: ListItem, note: String): Result<ListItem> = safeCall {
        val updated = item.copy(note = note.trim())
        dataSource.updateItemNote(listId, updated)
        updated
    }

    suspend fun removeItem(listId: String, itemId: String): Result<Unit> = safeCall {
        dataSource.deleteItem(listId, itemId)
    }

    /** כמות: 1 עד 999. */
    private fun validQuantity(quantity: Int): Int =
        quantity.also { if (it !in 1..999) throw AppException(ErrorCode.INVALID_QUANTITY) }
}

package com.smartcart.app.data.repository

import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import com.smartcart.app.data.model.ShoppingList
import com.smartcart.app.data.safeCall
import com.smartcart.app.data.source.DataSource
import com.smartcart.app.data.source.requireUid
import kotlinx.coroutines.flow.Flow

/** רשימות הקניות (מסכים 05, 06, 08, 09, 16). */
class ListRepository(private val dataSource: DataSource) {

    /** כל הרשימות שהמשתמשת משתתפת בהן, מהחדשה לישנה (מסכים 05, 16). */
    suspend fun myLists(): Result<List<ShoppingList>> = safeCall {
        dataSource.getListsForMember(dataSource.requireUid())
    }

    /** הרשימות שהמשתמשת יצרה, מהחדשה לישנה (מסך 09). */
    suspend fun ownedLists(): Result<List<ShoppingList>> = safeCall {
        dataSource.getListsOwnedBy(dataSource.requireUid())
    }

    suspend fun getList(listId: String): Result<ShoppingList?> = safeCall { dataSource.getList(listId) }

    /** משימה 21: הרשימה, ואחר כך גרסה חדשה בכל שינוי (למשל שם חדש). מסך 08 מאזין לזה. */
    fun observeList(listId: String): Flow<ShoppingList?> = dataSource.observeList(listId)

    /** יצירת רשימה חדשה (מסך 06). */
    suspend fun createList(name: String): Result<ShoppingList> = safeCall {
        dataSource.createList(validListName(name), dataSource.requireUid())
    }

    /** שינוי שם. רק בעלת הרשימה יכולה. */
    suspend fun renameList(listId: String, name: String): Result<Unit> = safeCall {
        dataSource.renameList(listId, validListName(name))
    }

    /** מחיקת הרשימה עם כל הפריטים שלה. רק בעלת הרשימה יכולה. */
    suspend fun deleteList(listId: String): Result<Boolean> = safeCall { dataSource.deleteList(listId) }

    /** שם הרשימה: 1 עד 40 תווים. */
    private fun validListName(name: String): String =
        name.trim().also { if (it.length !in 1..40) throw AppException(ErrorCode.INVALID_NAME) }
}

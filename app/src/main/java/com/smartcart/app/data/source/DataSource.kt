package com.smartcart.app.data.source

import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import com.smartcart.app.data.model.Chain
import com.smartcart.app.data.model.Comparison
import com.smartcart.app.data.model.ComparisonMode
import com.smartcart.app.data.model.ComparisonStatus
import com.smartcart.app.data.model.InviteLink
import com.smartcart.app.data.model.ListItem
import com.smartcart.app.data.model.Member
import com.smartcart.app.data.model.Product
import com.smartcart.app.data.model.ShoppingList
import com.smartcart.app.data.model.User

/**
 * מקור הנתונים: כל מה שהאפליקציה קוראת וכותבת ב-Firebase.
 * כרגע יש לו מימוש מדומה ([FakeDataSource]). בהמשך יתווסף מימוש שעובד מול Firestore ופונקציות הענן,
 * וה-Repositories לא ישתנו.
 * פעולה שנכשלת זורקת AppException עם קוד השגיאה.
 */
interface DataSource {

    /** המזהה של המשתמשת המחוברת, או null אם אין משתמשת מחוברת. */
    fun currentUid(): String?

    // ---------- users ----------
    suspend fun getUser(uid: String): User?
    suspend fun saveUser(user: User)

    // ---------- shoppingLists ----------
    /** רשימות שהמשתמשת משתתפת בהן, מהחדשה לישנה (מסכים 05, 16). */
    suspend fun getListsForMember(uid: String): List<ShoppingList>

    /** הרשימות שהמשתמשת יצרה, מהחדשה לישנה (מסך 09). */
    suspend fun getListsOwnedBy(uid: String): List<ShoppingList>
    suspend fun getList(listId: String): ShoppingList?
    suspend fun createList(name: String, ownerUid: String): ShoppingList
    suspend fun renameList(listId: String, name: String)

    // ---------- items ----------
    suspend fun getItems(listId: String): List<ListItem>
    suspend fun addItem(listId: String, item: ListItem): ListItem
    suspend fun updateItem(listId: String, item: ListItem)
    suspend fun deleteItem(listId: String, itemId: String)

    // ---------- members ----------
    suspend fun getMembers(listId: String): List<Member>

    // ---------- products, chains ----------
    /** מוצרים שאחת המילים בשם שלהם מתחילה ב-[prefix]. */
    suspend fun searchProducts(prefix: String, limit: Int): List<Product>
    suspend fun getProducts(barcodes: List<String>): List<Product>
    suspend fun getChains(): List<Chain>

    // ---------- comparisons ----------
    /** יוצרת מסמך השוואה בסטטוס PENDING ומחזירה את המזהה שלו. */
    suspend fun createComparison(
        listId: String,
        requestedBy: String,
        mode: ComparisonMode,
        budget: Int?,
    ): String
    suspend fun getComparison(comparisonId: String): Comparison?

    // ---------- inviteLinks ----------
    suspend fun getInviteLink(token: String): InviteLink?

    // ---------- פונקציות הענן מסוג callable (פרק 14.1) ----------
    suspend fun compareBasket(listId: String, comparisonId: String): ComparisonStatus

    /** מחזירה את הקישור (url) לרשימה. */
    suspend fun createInviteLink(listId: String): String

    /** מחזירה את המזהה של הרשימה שהמשתמשת הצטרפה אליה. */
    suspend fun joinList(token: String): String
    suspend fun cancelInviteLink(listId: String): Boolean
    suspend fun removeMember(listId: String, memberUid: String): Boolean
    suspend fun deleteList(listId: String): Boolean
    suspend fun deleteAccount(): Boolean
}

/** המזהה של המשתמשת המחוברת; אם אין משתמשת מחוברת, נזרקת שגיאה NOT_SIGNED_IN. */
fun DataSource.requireUid(): String =
    currentUid() ?: throw AppException(ErrorCode.NOT_SIGNED_IN)

package com.smartcart.app.data.model

/** רשימת קניות. מסמך shoppingLists/{listId} ב-Firestore. */
data class ShoppingList(
    val id: String,
    val name: String,
    val ownerUid: String,
    /** כל המשתתפות ברשימה, כולל בעלת הרשימה. */
    val memberUids: List<String>,
    /** המזהה של קישור ההזמנה הפעיל, או null אם עוד לא נוצר קישור. */
    val inviteToken: String? = null,
    /** מספר הפריטים. פונקציית onItemWritten מעדכנת אותו. */
    val itemCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    // תוצאת ההשוואה האחרונה. פונקציית compareBasket מעדכנת את השדות האלה.
    val lastComparisonId: String? = null,
    val lastCheapestChain: ChainId? = null,
    val lastCheapestTotal: Double? = null,
    val lastBudget: Int? = null,
)

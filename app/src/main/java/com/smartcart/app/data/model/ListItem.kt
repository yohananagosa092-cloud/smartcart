package com.smartcart.app.data.model

/** פריט ברשימה. מסמך shoppingLists/{listId}/items/{itemId} ב-Firestore. */
data class ListItem(
    val id: String,
    /** הברקוד של המוצר בקטלוג. לפיו מוצאים את המחירים בכל רשת. */
    val barcode: String,
    val name: String,
    /** כמות בטווח 1–999. */
    val quantity: Int,
    val note: String = "",
    /** המשתמשת שהוסיפה את הפריט. */
    val addedBy: String,
    val addedAt: Long,
    val updatedAt: Long,
)

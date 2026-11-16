package com.smartcart.app.data.model

/**
 * משתמשת. מסמך users/{uid} ב-Firestore.
 * כל התאריכים בשכבת הנתונים נשמרים כמילישניות מאז 1970 (Long).
 */
data class User(
    val uid: String,
    val email: String,
    val displayName: String,
    val createdAt: Long,
    /** מספר ההשוואות שהמשתמשת הריצה. רק פונקציית compareBasket מעדכנת אותו. */
    val comparisonCount: Int = 0,
)

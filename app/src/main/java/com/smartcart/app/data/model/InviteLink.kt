package com.smartcart.app.data.model

/**
 * קישור ההזמנה של רשימה. מסמך inviteLinks/{token}.
 * הוא שומר עותק של פרטי הרשימה, כי מי שעוד לא הצטרפה לא יכולה לקרוא את הרשימה עצמה (מסך 15).
 */
data class InviteLink(
    val token: String,
    val listId: String,
    val listName: String,
    val ownerName: String,
    val itemCount: Int,
    val memberNames: List<String>,
    val createdAt: Long,
    /** false אחרי שבעלת הרשימה ביטלה את הקישור. */
    val active: Boolean,
)

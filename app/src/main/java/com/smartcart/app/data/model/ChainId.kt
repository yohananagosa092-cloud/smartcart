package com.smartcart.app.data.model

/** חמש הרשתות שהאפליקציה משווה ביניהן. [id] הוא המזהה שנשמר ב-Firestore (פרק 14.3). */
enum class ChainId(val id: String, val displayName: String) {
    RAMI_LEVY("ramiLevy", "רמי לוי"),
    SHUFERSAL("shufersal", "שופרסל"),
    YOCHANANOF("yochananof", "יוחננוף"),
    CHATZI_HINAM("chatziHinam", "חצי חינם"),
    OSHER_AD("osherAd", "אושר עד");

    companion object {
        /** מחזירה את הרשת לפי המזהה שלה ב-Firestore, או null אם המזהה לא מוכר. */
        fun fromId(id: String): ChainId? = entries.firstOrNull { it.id == id }
    }
}

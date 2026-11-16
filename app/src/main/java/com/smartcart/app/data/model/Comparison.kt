package com.smartcart.app.data.model

/** סוג ההשוואה שנבחר במסך 11: לפי רשימת הקניות, או לפי תקציב. */
enum class ComparisonMode { LIST, BUDGET }

enum class ComparisonStatus { PENDING, RUNNING, DONE, FAILED }

/** השוואה. מסמך comparisons/{id}. האפליקציה יוצרת אותו, ופונקציית compareBasket ממלאת את התוצאה. */
data class Comparison(
    val id: String,
    val listId: String,
    val requestedBy: String,
    val mode: ComparisonMode,
    /** התקציב בשקלים (1–10,000) בהשוואה לפי תקציב; null בהשוואה לפי רשימה. */
    val budget: Int?,
    val status: ComparisonStatus,
    /** שלב החישוב, מוצג במסך 12. */
    val step: Int,
    val result: ComparisonResult?,
    val computeTimeMs: Long?,
    val createdAt: Long,
)

/** התוצאה של השוואה שהסתיימה (פרק 14.2). */
data class ComparisonResult(
    /** מספר הפריטים בסל המשותף: פריטים שנמצאו בכל חמש הרשתות. */
    val commonItems: Int,
    /** פריטים שחסרים ברשת אחת לפחות. הם לא נכללים בסכומים. */
    val missingItems: List<MissingItem>,
    /** עלות הסל המשותף בכל רשת, בשקלים. */
    val totals: Map<ChainId, Double>,
    /** הרשת הזולה ביותר, או null אם אין אף פריט בסל המשותף. */
    val cheapestChain: ChainId?,
    /** בהשוואה לפי תקציב: כמה נשאר בכל רשת (מספר שלילי = חריגה). null בהשוואה לפי רשימה. */
    val budgetLeft: Map<ChainId, Double>?,
    /** מתי עודכנו המחירים של כל רשת. */
    val pricesUpdatedAt: Map<ChainId, Long>,
)

/** פריט שלא נמצא בכל הרשתות, עם הרשתות שבהן הוא כן נמצא. */
data class MissingItem(
    val barcode: String,
    val name: String,
    val quantity: Int,
    val foundIn: List<ChainId>,
)

/** שורה במסך 14 (פירוט לפי רשת): פריט מהסל המשותף והמחיר שלו ברשת אחת. */
data class PricedItem(
    val barcode: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    /** unitPrice × quantity. */
    val cost: Double,
)

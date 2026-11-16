package com.smartcart.app.data.model

/** התוצאה של קליטת המחירים האחרונה. */
enum class IngestStatus { SUCCESS, FAILED }

/** רשת. מסמך chains/{chainId}. רק פונקציית ingestPrices כותבת אותו. */
data class Chain(
    val id: ChainId,
    val displayName: String,
    /** מתי המחירים של הרשת נקלטו בהצלחה בפעם האחרונה, או null אם עוד לא נקלטו. */
    val lastSuccessAt: Long?,
    val lastStatus: IngestStatus?,
    /** מספר המוצרים בקליטה האחרונה. */
    val itemCount: Int,
)

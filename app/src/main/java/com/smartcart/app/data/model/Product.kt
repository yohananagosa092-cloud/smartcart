package com.smartcart.app.data.model

/** המחיר של מוצר ברשת אחת. */
data class ChainPrice(
    val price: Double,
    val updatedAt: Long,
)

/**
 * מוצר בקטלוג. מסמך products/{barcode}. רק פונקציית ingestPrices כותבת אותו.
 * ב-[prices] יש רק את הרשתות שהמוצר נמצא אצלן.
 */
data class Product(
    val barcode: String,
    val name: String,
    val brand: String,
    /** לדוגמה "1 ליטר". */
    val unitDescription: String,
    val prices: Map<ChainId, ChainPrice>,
)

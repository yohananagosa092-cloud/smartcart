package com.smartcart.app.data.repository

import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import com.smartcart.app.data.model.ChainId
import com.smartcart.app.data.model.Comparison
import com.smartcart.app.data.model.ComparisonMode
import com.smartcart.app.data.model.PricedItem
import com.smartcart.app.data.safeCall
import com.smartcart.app.data.source.DataSource
import com.smartcart.app.data.source.requireUid

/** השוואת הסל בין הרשתות (מסכים 11–14). */
class ComparisonRepository(private val dataSource: DataSource) {

    /**
     * מתחילה השוואה: יוצרת מסמך השוואה ואז קוראת לפונקציה compareBasket.
     * מחזירה את המזהה של ההשוואה, כדי שמסכים 12 ו-13 יקראו את התוצאה.
     *
     * @param budget התקציב בשקלים (1–10,000). חובה רק בהשוואה לפי תקציב.
     */
    suspend fun startComparison(listId: String, mode: ComparisonMode, budget: Int? = null): Result<String> =
        safeCall {
            val validBudget = when (mode) {
                ComparisonMode.LIST -> null
                ComparisonMode.BUDGET ->
                    budget?.takeIf { it in 1..10_000 } ?: throw AppException(ErrorCode.INVALID_BUDGET)
            }
            val comparisonId = dataSource.createComparison(listId, dataSource.requireUid(), mode, validBudget)
            dataSource.compareBasket(listId, comparisonId)
            comparisonId
        }

    suspend fun comparison(comparisonId: String): Result<Comparison?> = safeCall {
        dataSource.getComparison(comparisonId)
    }

    /** מסך 14: כל פריטי הסל המשותף והמחיר של כל אחד מהם ברשת אחת. */
    suspend fun chainBreakdown(listId: String, chain: ChainId): Result<List<PricedItem>> = safeCall {
        val items = dataSource.getItems(listId)
        val products = dataSource.getProducts(items.map { it.barcode }).associateBy { it.barcode }
        items.mapNotNull { item ->
            val prices = products[item.barcode]?.prices
            // פריט שחסר ברשת אחת לפחות אינו בסל המשותף
            if (prices == null || !prices.keys.containsAll(ChainId.entries)) return@mapNotNull null
            val unitPrice = prices.getValue(chain).price
            PricedItem(
                barcode = item.barcode,
                name = item.name,
                quantity = item.quantity,
                unitPrice = unitPrice,
                cost = Math.round(unitPrice * 100) * item.quantity / 100.0,
            )
        }
    }
}

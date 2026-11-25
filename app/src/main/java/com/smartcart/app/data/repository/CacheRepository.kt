package com.smartcart.app.data.repository

import com.smartcart.app.data.local.CacheDao
import com.smartcart.app.data.local.RecentProduct
import com.smartcart.app.data.local.SearchQuery
import com.smartcart.app.data.model.Product
import com.smartcart.app.data.safeCall
import kotlinx.coroutines.flow.Flow

/**
 * משימה 24: המוצרים האחרונים והיסטוריית החיפוש, שנשמרים בטלפון ב-Room (מסך 07).
 * ה-ViewModel של מסך 07 קורא ל-[rememberProduct] כשמוסיפים מוצר, ול-[rememberSearch] כשמחפשים.
 */
class CacheRepository(
    private val dao: CacheDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /** עד [SHOW] המוצרים האחרונים, מהחדש לישן. מתעדכן לבד כשנוסף מוצר. */
    fun recentProducts(): Flow<List<RecentProduct>> = dao.recentProducts(SHOW)

    /** זוכרת מוצר שנוסף לרשימה, ומשאירה רק את [KEEP] האחרונים. */
    suspend fun rememberProduct(product: Product): Result<Unit> = safeCall {
        dao.saveRecentProduct(
            RecentProduct(
                barcode = product.barcode,
                name = product.name,
                unitDescription = product.unitDescription,
                addedAt = clock(),
            ),
        )
        dao.trimRecentProducts(KEEP)
    }

    /** עד [SHOW] החיפושים האחרונים, מהחדש לישן. */
    fun searchHistory(): Flow<List<String>> = dao.searchHistory(SHOW)

    /** זוכרת חיפוש. חיפוש קצר מ-2 תווים לא נשמר, כי הוא גם לא מחפש בקטלוג. */
    suspend fun rememberSearch(query: String): Result<Unit> = safeCall {
        val text = query.trim()
        if (text.length >= ProductRepository.MIN_QUERY_LENGTH) {
            dao.saveSearch(SearchQuery(text, clock()))
            dao.trimSearchHistory(KEEP)
        }
    }

    suspend fun clearSearchHistory(): Result<Unit> = safeCall { dao.clearSearchHistory() }

    companion object {
        /** כמה מוצרים או חיפושים מציגים. */
        const val SHOW = 10

        /** כמה שומרים בטלפון לכל היותר. */
        const val KEEP = 20
    }
}

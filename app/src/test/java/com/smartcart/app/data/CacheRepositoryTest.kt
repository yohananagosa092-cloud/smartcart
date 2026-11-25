package com.smartcart.app.data

import com.smartcart.app.data.local.CacheDao
import com.smartcart.app.data.local.RecentProduct
import com.smartcart.app.data.local.SearchQuery
import com.smartcart.app.data.model.Product
import com.smartcart.app.data.repository.CacheRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** בדיקות למטמון (משימה 24) מול DAO מדומה ששומר בזיכרון, בלי טלפון. */
class CacheRepositoryTest {

    /** DAO מדומה שמתנהג כמו השאילתות ב-CacheDao. */
    private class FakeCacheDao : CacheDao {
        val products = MutableStateFlow(mapOf<String, RecentProduct>())
        val searches = MutableStateFlow(mapOf<String, SearchQuery>())

        override suspend fun saveRecentProduct(product: RecentProduct) {
            products.value = products.value + (product.barcode to product)
        }

        override fun recentProducts(limit: Int): Flow<List<RecentProduct>> =
            products.map { all -> all.values.sortedByDescending { it.addedAt }.take(limit) }

        override suspend fun trimRecentProducts(keep: Int) {
            products.value = products.value.values.sortedByDescending { it.addedAt }.take(keep)
                .associateBy { it.barcode }
        }

        override suspend fun saveSearch(search: SearchQuery) {
            searches.value = searches.value + (search.query to search)
        }

        override fun searchHistory(limit: Int): Flow<List<String>> =
            searches.map { all -> all.values.sortedByDescending { it.searchedAt }.take(limit).map { it.query } }

        override suspend fun trimSearchHistory(keep: Int) {
            searches.value = searches.value.values.sortedByDescending { it.searchedAt }.take(keep)
                .associateBy { it.query }
        }

        override suspend fun clearSearchHistory() {
            searches.value = emptyMap()
        }
    }

    private val dao = FakeCacheDao()
    private var time = 0L
    private val cache = CacheRepository(dao, clock = { ++time })

    private fun product(n: Int) = Product("barcode$n", "מוצר $n", "מותג", "1 יח׳", emptyMap())

    @Test
    fun addingTheSameProductAgainMovesItToTheTop() = runBlocking {
        cache.rememberProduct(product(1)).getOrThrow()
        cache.rememberProduct(product(2)).getOrThrow()
        cache.rememberProduct(product(1)).getOrThrow() // שוב מוצר 1

        val recent = cache.recentProducts().first()
        assertEquals(listOf("barcode1", "barcode2"), recent.map { it.barcode }) // פעם אחת, והוא ראשון
    }

    @Test
    fun onlyTheLatestProductsAreKept() = runBlocking {
        (1..25).forEach { cache.rememberProduct(product(it)).getOrThrow() }

        assertEquals(CacheRepository.KEEP, dao.products.value.size) // נשארו 20 בטלפון
        val shown = cache.recentProducts().first()
        assertEquals(CacheRepository.SHOW, shown.size) // מוצגים 10
        assertEquals("barcode25", shown.first().barcode) // האחרון שנוסף ראשון
    }

    @Test
    fun shortSearchesAreNotSavedAndHistoryCanBeCleared() = runBlocking {
        cache.rememberSearch("ח").getOrThrow() // תו אחד: לא נשמר
        cache.rememberSearch("  חלב ").getOrThrow()
        cache.rememberSearch("לחם").getOrThrow()

        assertEquals(listOf("לחם", "חלב"), cache.searchHistory().first())

        cache.clearSearchHistory().getOrThrow()
        assertEquals(emptyList<String>(), cache.searchHistory().first())
    }
}

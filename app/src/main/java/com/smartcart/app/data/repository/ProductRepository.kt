package com.smartcart.app.data.repository

import com.smartcart.app.data.model.Chain
import com.smartcart.app.data.model.Product
import com.smartcart.app.data.safeCall
import com.smartcart.app.data.source.DataSource

/** קטלוג המוצרים והרשתות (מסכים 07, 12). */
class ProductRepository(private val dataSource: DataSource) {

    suspend fun findBarcode(barcode: String): Result<Product?> = safeCall {
        val code = barcode.trim()
        if (code.isNotEmpty() && code.all { it in '0'..'9' }) {
            dataSource.getProducts(listOf(code)).firstOrNull()
        } else null
    }

    /**
     * חיפוש מוצרים במסך 07. מחפשים לפי המילה הראשונה בקטלוג, ומשאירים רק מוצרים
     * שכל מילה בחיפוש היא תחילת מילה בשם שלהם.
     */
    suspend fun search(query: String): Result<List<Product>> = safeCall {
        val words = wordsOf(query)
        if (words.isEmpty() || words.first().length < MIN_QUERY_LENGTH) {
            emptyList()
        } else {
            dataSource.searchProducts(words.first(), SEARCH_LIMIT).filter { product ->
                val nameWords = wordsOf(product.name)
                words.all { word -> nameWords.any { it.startsWith(word) } }
            }
        }
    }

    suspend fun chains(): Result<List<Chain>> = safeCall { dataSource.getChains() }

    private fun wordsOf(text: String): List<String> =
        text.lowercase().split(WORD_SEPARATORS).filter(String::isNotEmpty)

    companion object {
        /** התחיליות בקטלוג מתחילות באורך 2, ולכן החיפוש מתחיל מ-2 תווים. */
        const val MIN_QUERY_LENGTH = 2
        const val SEARCH_LIMIT = 20
        private val WORD_SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
    }
}

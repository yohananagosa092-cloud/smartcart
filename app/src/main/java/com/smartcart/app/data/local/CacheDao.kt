package com.smartcart.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * משימה 24: השאילתות על שתי הטבלאות שבטלפון.
 * @Dao (Data Access Object) = "כאן כתוב איך קוראים וכותבים בטבלאות".
 * Room כותב לבד את הקוד שמבצע כל פונקציה, ובודק את ה-SQL כבר בזמן הבנייה.
 */
@Dao
interface CacheDao {

    // ---------- מוצרים אחרונים ----------

    /** @Upsert = מוסיפה שורה, או מעדכנת אותה אם כבר יש מוצר עם אותו ברקוד (המפתח). */
    @Upsert
    suspend fun saveRecentProduct(product: RecentProduct)

    /**
     * המוצרים האחרונים, מהחדש לישן (מסך 07).
     * מחזירה Flow: כשנוסף מוצר, המסך מקבל את הרשימה החדשה לבד (כמו במשימה 21).
     */
    @Query("SELECT * FROM recent_products ORDER BY addedAt DESC LIMIT :limit")
    fun recentProducts(limit: Int): Flow<List<RecentProduct>>

    /** משאירה רק את [keep] המוצרים האחרונים ומוחקת את הישנים, כדי שהטבלה לא תגדל בלי סוף. */
    @Query(
        "DELETE FROM recent_products WHERE barcode NOT IN " +
            "(SELECT barcode FROM recent_products ORDER BY addedAt DESC LIMIT :keep)",
    )
    suspend fun trimRecentProducts(keep: Int)

    // ---------- היסטוריית חיפוש ----------

    /** שומרת חיפוש; חיפוש שחוזר על עצמו רק מעדכן את הזמן שלו. */
    @Upsert
    suspend fun saveSearch(search: SearchQuery)

    /** החיפושים האחרונים, מהחדש לישן. מחזירה רק את הטקסט של כל חיפוש. */
    @Query("SELECT `query` FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun searchHistory(limit: Int): Flow<List<String>>

    /** משאירה רק את [keep] החיפושים האחרונים. */
    @Query(
        "DELETE FROM search_history WHERE `query` NOT IN " +
            "(SELECT `query` FROM search_history ORDER BY searchedAt DESC LIMIT :keep)",
    )
    suspend fun trimSearchHistory(keep: Int)

    /** מחיקת כל היסטוריית החיפוש. */
    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}

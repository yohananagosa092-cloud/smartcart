package com.smartcart.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// משימה 24: מטמון Room. שתי טבלאות קטנות שנשמרות בתוך הטלפון בלבד, לא ב-Firebase:
// הן אישיות למשתמשת, צריך אותן מיד (בלי אינטרנט), ואין סיבה לשתף אותן או לשלוח אותן לשרת.

/**
 * מוצר שהמשתמשת הוסיפה לאחרונה לרשימה. מסך 07 מציג אותם בראש הרשימה כהצעות מהירות.
 * @Entity = "זו טבלה". כל אובייקט מהמחלקה הזאת הוא שורה בטבלה recent_products.
 */
@Entity(tableName = "recent_products")
data class RecentProduct(
    /** הברקוד הוא המפתח (@PrimaryKey): אותו מוצר נשמר פעם אחת בלבד, ובהוספה חוזרת רק מתעדכן הזמן. */
    @PrimaryKey val barcode: String,
    val name: String,
    /** לדוגמה "1 ליטר". */
    val unitDescription: String,
    /** מתי נוסף בפעם האחרונה, במילישניות. לפי זה מסדרים מהחדש לישן. */
    val addedAt: Long,
)

/**
 * חיפוש שהמשתמשת עשתה במסך 07 (היסטוריית חיפוש).
 * הטקסט עצמו הוא המפתח: חיפוש שחוזר על עצמו נשמר פעם אחת, ורק הזמן מתעדכן.
 */
@Entity(tableName = "search_history")
data class SearchQuery(
    @PrimaryKey val query: String,
    /** מתי חיפשו בפעם האחרונה. */
    val searchedAt: Long,
)

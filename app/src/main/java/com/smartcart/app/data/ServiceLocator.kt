package com.smartcart.app.data

import com.smartcart.app.data.auth.AuthRepository
import com.smartcart.app.data.auth.FirebaseAuthService
import com.smartcart.app.data.repository.ComparisonRepository
import com.smartcart.app.data.repository.ItemRepository
import com.smartcart.app.data.repository.ListRepository
import com.smartcart.app.data.repository.ProductRepository
import com.smartcart.app.data.repository.SharingRepository
import com.smartcart.app.data.repository.UserRepository
import com.smartcart.app.data.source.DataSource
import com.smartcart.app.data.source.FakeDataSource
import com.smartcart.app.data.source.FirestoreDataSource

/**
 * המקום היחיד שבו נבחר מקור הנתונים, ומכאן ה-ViewModels מקבלים את ה-Repositories.
 * כרגע זה מקור הנתונים המדומה. כשהחיבור ל-Firebase יהיה מוכן, מחליפים כאן שורה אחת.
 */
object ServiceLocator {

    /**
     * true = הרשימות והפריטים נשמרים ב-Firestore (משימה 18); כל השאר עדיין מנתוני הדמה.
     * כבוי עד שכללי האבטחה של shoppingLists ו-items יעלו לפרויקט (משימה 28 של הודיה),
     * כי בלעדיהם Firestore דוחה כל קריאה וכתיבה. כשהם עולים, משנים כאן ל-true.
     */
    private const val USE_FIRESTORE = false

    private val dataSource: DataSource by lazy {
        if (USE_FIRESTORE) FirestoreDataSource(demo = FakeDataSource()) else FakeDataSource()
    }

    /** האימות כבר עובד מול Firebase Authentication של הפרויקט (משימה 12). */
    val authRepository by lazy { AuthRepository(FirebaseAuthService()) }

    val userRepository by lazy { UserRepository(dataSource) }
    val listRepository by lazy { ListRepository(dataSource) }
    val itemRepository by lazy { ItemRepository(dataSource) }
    val sharingRepository by lazy { SharingRepository(dataSource) }
    val productRepository by lazy { ProductRepository(dataSource) }
    val comparisonRepository by lazy { ComparisonRepository(dataSource) }
}

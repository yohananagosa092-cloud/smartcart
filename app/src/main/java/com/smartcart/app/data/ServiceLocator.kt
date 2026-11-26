package com.smartcart.app.data

import com.smartcart.app.BuildConfig

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
 * בניית Debug משתמשת בנתוני הדמה כברירת מחדל. אימות החשבון נפרד ממקור זה.
 */
object ServiceLocator {

    private val dataSource: DataSource by lazy {
        val demo = FakeDataSource()
        if (BuildConfig.USE_FIRESTORE) FirestoreDataSource(demo = demo) else demo
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

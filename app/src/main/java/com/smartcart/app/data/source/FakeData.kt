package com.smartcart.app.data.source

import com.smartcart.app.data.model.Chain
import com.smartcart.app.data.model.ChainId
import com.smartcart.app.data.model.ChainId.CHATZI_HINAM
import com.smartcart.app.data.model.ChainId.OSHER_AD
import com.smartcart.app.data.model.ChainId.RAMI_LEVY
import com.smartcart.app.data.model.ChainId.SHUFERSAL
import com.smartcart.app.data.model.ChainId.YOCHANANOF
import com.smartcart.app.data.model.ChainPrice
import com.smartcart.app.data.model.IngestStatus
import com.smartcart.app.data.model.Product

/**
 * נתוני דמה למקור הנתונים המדומה. המוצרים, הברקודים והמחירים מומצאים, ומשמשים רק לפיתוח
 * המסכים עד שקליטת המחירים האמיתית תעבוד.
 */
internal object FakeData {

    const val DEMO_UID = "demo_user"
    const val DEMO_EMAIL = "demo@smartcart.test"
    const val DEMO_NAME = "משתמשת לדוגמה"

    /** כתובת דמה. הכתובת האמיתית תיקבע יחד עם פרויקט Firebase Hosting. */
    const val INVITE_BASE_URL = "https://smartcart-demo.web.app/join/"

    private const val HOUR_MS = 60 * 60 * 1000L

    fun chains(now: Long): List<Chain> = ChainId.entries.map {
        Chain(
            id = it,
            displayName = it.displayName,
            lastSuccessAt = now - 2 * HOUR_MS,
            lastStatus = IngestStatus.SUCCESS,
            itemCount = products(now).count { p -> it in p.prices },
        )
    }

    fun products(now: Long): List<Product> {
        // סדר המחירים: רמי לוי, שופרסל, יוחננוף, חצי חינם, אושר עד. null = המוצר לא נמצא ברשת.
        fun product(barcode: String, name: String, brand: String, unit: String, vararg prices: Double?) =
            Product(
                barcode = barcode,
                name = name,
                brand = brand,
                unitDescription = unit,
                prices = listOf(RAMI_LEVY, SHUFERSAL, YOCHANANOF, CHATZI_HINAM, OSHER_AD)
                    .zip(prices.toList())
                    .mapNotNull { (chain, price) -> price?.let { chain to ChainPrice(it, now - 2 * HOUR_MS) } }
                    .toMap(),
            )

        return listOf(
            product("0000000000017", "חלב 3% שומן", "מחלבה", "1 ליטר", 6.20, 6.90, 6.50, 6.30, 6.40),
            product("0000000000024", "לחם אחיד פרוס", "מאפייה", "750 גרם", 7.30, 7.30, 7.30, 7.30, 7.30),
            product("0000000000031", "ביצים L", "משק", "12 יחידות", 11.90, 13.50, 12.40, 12.20, 12.60),
            product("0000000000048", "עגבניות", "ירקות", "1 ק״ג", 5.90, 7.90, 6.90, 4.90, 6.50),
            product("0000000000055", "גבינה לבנה 5%", "מחלבה", "250 גרם", 4.90, 5.60, 5.20, 5.10, 5.30),
            product("0000000000062", "אורז בסמטי", "מזווה", "1 ק״ג", 9.90, 11.90, 10.90, 10.50, 10.90),
            product("0000000000079", "שמן קנולה", "מזווה", "1 ליטר", 8.90, 9.90, 9.40, 9.20, 9.10),
            // חסר ביוחננוף, כדי שיהיה פריט חסר בהשוואה
            product("0000000000086", "קפה נמס", "מזווה", "200 גרם", 21.90, 24.90, null, 22.90, 23.50),
        )
    }
}

package com.smartcart.app.data.source

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.smartcart.app.data.model.ChainId
import com.smartcart.app.data.model.ListItem
import com.smartcart.app.data.model.ShoppingList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * מקור נתונים שעובד מול Firestore (משימה 18): הרשימות והפריטים נשמרים באמת ב-Firebase.
 * כל מה שעוד לא מוכן בשרת (מוצרים, השוואה, שיתוף) ממשיך לעבוד מנתוני הדמה, דרך [demo].
 */
class FirestoreDataSource(
    private val demo: DataSource,
    private val db: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth,
) : DataSource by demo {

    /** התיקייה של כל הרשימות ב-Firestore. */
    private val lists = db.collection("shoppingLists")

    /** המשתמשת המחוברת ב-Firebase Authentication (משימה 12). */
    override fun currentUid(): String? = auth.currentUser?.uid

    // ---------- שלב 2: קריאת רשימות ----------

    /**
     * כל הרשימות שהמשתמשת משתתפת בהן, מהחדשה לישנה (מסכים 05, 16).
     * האפליקציה "שואלת" את Firebase פעם אחת ומקבלת תשובה אחת.
     */
    override suspend fun getListsForMember(uid: String): List<ShoppingList> =
        lists.whereArrayContains("memberUids", uid)                 // רק רשימות שהמזהה שלי ברשימת המשתתפות שלהן
            .orderBy("updatedAt", Query.Direction.DESCENDING)        // מסדרים מהרשימה שעודכנה אחרונה
            .get()                                                   // שולחים את הבקשה ל-Firebase
            .await()                                                 // מחכים לתשובה ברקע, בלי שהמסך ייתקע
            .documents
            .mapNotNull { it.toShoppingList() }                      // כל דף שחזר הופך לאובייקט ShoppingList

    /** הרשימות שהמשתמשת יצרה, מהחדשה לישנה (מסך 09). */
    override suspend fun getListsOwnedBy(uid: String): List<ShoppingList> =
        lists.whereEqualTo("ownerUid", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toShoppingList() }

    /** רשימה אחת לפי המזהה שלה, או null אם היא לא קיימת. */
    override suspend fun getList(listId: String): ShoppingList? =
        lists.document(listId).get().await().toShoppingList()

    // ---------- שלב 3: יצירת רשימה ושינוי שם ----------

    /** יצירת רשימה חדשה (מסך 06). בעלת הרשימה היא גם המשתתפת הראשונה. */
    override suspend fun createList(name: String, ownerUid: String): ShoppingList {
        val doc = lists.document() // 1. דף חדש בתיקייה shoppingLists, עם מזהה אקראי
        doc.set( // 2. כותבים בדף את פרטי הרשימה
            mapOf(
                "name" to name,
                "ownerUid" to ownerUid,                       // מי יצרה
                "memberUids" to listOf(ownerUid),             // מי משתתפת (בהתחלה רק היא)
                "inviteToken" to null,
                "itemCount" to 0,
                "createdAt" to FieldValue.serverTimestamp(),  // השעה של השרת, אותה שעה לכל הטלפונים
                "updatedAt" to FieldValue.serverTimestamp(),
                "lastComparisonId" to null,
                "lastCheapestChain" to null,
                "lastCheapestTotal" to null,
                "lastBudget" to null,
            ),
        ).logIfFailed("createList") // 3. לא מחכים לשרת: נשמר בטלפון מיד, נשלח כשיש אינטרנט

        val now = System.currentTimeMillis()
        return ShoppingList( // 4. מחזירים למסך את הרשימה החדשה
            id = doc.id,
            name = name,
            ownerUid = ownerUid,
            memberUids = listOf(ownerUid),
            createdAt = now,
            updatedAt = now,
        )
    }

    /** שינוי שם הרשימה. כללי האבטחה בשרת מאשרים את זה רק לבעלת הרשימה. */
    override suspend fun renameList(listId: String, name: String) {
        lists.document(listId)
            .update(mapOf("name" to name, "updatedAt" to FieldValue.serverTimestamp()))
            .logIfFailed("renameList")
    }

    // ---------- שלב 4: פריטים ----------

    /** התיקייה של הפריטים בתוך רשימה אחת: shoppingLists/{listId}/items */
    private fun items(listId: String) = lists.document(listId).collection("items")

    /** כל הפריטים ברשימה, לפי סדר ההוספה (מסך 08). */
    override suspend fun getItems(listId: String): List<ListItem> =
        items(listId)
            .orderBy("addedAt")
            .get()
            .await()
            .documents
            .mapNotNull { it.toListItem() }

    /** הוספת פריט חדש לרשימה (מסך 07). מחזירה את הפריט עם המזהה החדש שלו. */
    override suspend fun addItem(listId: String, item: ListItem): ListItem {
        val doc = items(listId).document() // דף חדש עם מזהה אקראי
        doc.set(
            mapOf(
                "barcode" to item.barcode,
                "name" to item.name,
                "quantity" to item.quantity,
                "note" to item.note,
                "addedBy" to item.addedBy,
                "addedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).logIfFailed("addItem")
        return item.copy(id = doc.id)
    }

    /** שינוי כמות או הערה של פריט (מסך 08). */
    override suspend fun updateItem(listId: String, item: ListItem) {
        items(listId).document(item.id)
            .update(
                mapOf(
                    "quantity" to item.quantity,
                    "note" to item.note,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).logIfFailed("updateItem")
    }

    /** מחיקת פריט מהרשימה (מסך 08). */
    override suspend fun deleteItem(listId: String, itemId: String) {
        items(listId).document(itemId).delete().logIfFailed("deleteItem")
    }

    // ---------- משימה 21: סנכרון בזמן אמת ----------
    // משימה 18 "שואלת" את Firebase ומקבלת תשובה פעם אחת (כמו להתקשר ולשאול).
    // משימה 21 "מאזינה": Firebase שולח את המצב עכשיו, ושוב בכל פעם שמישהי משנה משהו,
    // גם מטלפון אחר (כמו קבוצת וואטסאפ: כל הודעה חדשה מגיעה לבד).

    /** מאזינה לרשימה עצמה, למשל לשינוי שם. */
    override fun observeList(listId: String): Flow<ShoppingList?> = callbackFlow { // 1. "צינור" (Flow) שעדכונים זורמים בו
        val registration = lists.document(listId)
            .addSnapshotListener { snapshot, error -> // 2. "תודיעי לי על כל שינוי בדף של הרשימה"
                if (error != null) {
                    close(error) // שגיאה (למשל אין הרשאה) סוגרת את הצינור, והמסך מקבל אותה
                    return@addSnapshotListener
                }
                trySend(snapshot?.toShoppingList()) // 3. כל עדכון נשלח לצינור
            }
        awaitClose { registration.remove() } // 4. כשהמסך נסגר מפסיקים להאזין, כדי לא לבזבז סוללה
    }

    /** מאזינה לפריטים: כל הוספה, שינוי כמות או מחיקה שולחים את רשימת הפריטים המעודכנת. */
    override fun observeItems(listId: String): Flow<List<ListItem>> = callbackFlow { // 1. "צינור" של עדכונים
        val registration = items(listId).orderBy("addedAt")
            .addSnapshotListener { snapshot, error -> // 2. "תודיעי לי על כל שינוי בתיקיית הפריטים"
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toListItem() }.orEmpty()) // 3. הרשימה החדשה נשלחת לצינור
            }
        awaitClose { registration.remove() } // 4. כשהמסך נסגר מפסיקים להאזין
    }

    /**
     * כתיבה בלי לחכות לשרת: Firestore שומר את השינוי בטלפון מיד, ושולח אותו לשרת כשיש חיבור.
     * כך אפשר לערוך גם בלי אינטרנט. אם השרת דוחה את הכתיבה, היא נרשמת ביומן (Logcat).
     */
    private fun Task<Void>.logIfFailed(action: String) {
        addOnFailureListener { Log.w(TAG, "$action failed", it) }
    }

    // ---------- המרה מדף ב-Firestore לאובייקט שלנו ----------

    /** הופכת דף של רשימה ב-Firestore לאובייקט ShoppingList. דף ריק או חסר שם מחזיר null. */
    private fun DocumentSnapshot.toShoppingList(): ShoppingList? {
        val name = getString("name") ?: return null
        return ShoppingList(
            id = id,
            name = name,
            ownerUid = getString("ownerUid").orEmpty(),
            memberUids = (get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            inviteToken = getString("inviteToken"),
            itemCount = getLong("itemCount")?.toInt() ?: 0,
            createdAt = millis("createdAt"),
            updatedAt = millis("updatedAt"),
            lastComparisonId = getString("lastComparisonId"),
            lastCheapestChain = getString("lastCheapestChain")?.let { ChainId.fromId(it) },
            lastCheapestTotal = getDouble("lastCheapestTotal"),
            lastBudget = getLong("lastBudget")?.toInt(),
        )
    }

    /** הופכת דף של פריט ב-Firestore לאובייקט ListItem. דף בלי ברקוד או שם מחזיר null. */
    private fun DocumentSnapshot.toListItem(): ListItem? {
        val barcode = getString("barcode") ?: return null
        val name = getString("name") ?: return null
        return ListItem(
            id = id,
            barcode = barcode,
            name = name,
            quantity = getLong("quantity")?.toInt() ?: 1,
            note = getString("note").orEmpty(),
            addedBy = getString("addedBy").orEmpty(),
            addedAt = millis("addedAt"),
            updatedAt = millis("updatedAt"),
        )
    }

    /**
     * תאריך מתוך הדף, במילישניות. כשאין חיבור, השרת עוד לא קבע את הזמן,
     * ולכן משתמשים בהערכה של הטלפון (ESTIMATE).
     */
    private fun DocumentSnapshot.millis(field: String): Long =
        getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L

    private companion object {
        const val TAG = "FirestoreDataSource"
    }
}

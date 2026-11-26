package com.smartcart.app.data.source

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import com.smartcart.app.data.model.ChainId
import com.smartcart.app.data.model.InviteLink
import com.smartcart.app.data.model.Product
import com.smartcart.app.data.model.ChainPrice
import com.smartcart.app.data.model.Member
import com.smartcart.app.data.model.Role
import com.smartcart.app.data.model.ListItem
import com.smartcart.app.data.model.ShoppingList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * מקור נתונים שעובד מול Firestore (משימה 18): הרשימות והפריטים נשמרים באמת ב-Firebase.
 * הרשימות, הפריטים, המשתתפות והקטלוג נקראים מ-Firestore.
 */
class FirestoreDataSource(
    private val demo: DataSource,
    private val db: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth,
) : DataSource by demo {

    /** התיקייה של כל הרשימות ב-Firestore. */
    private val lists = db.collection("shoppingLists")
    private val products = db.collection("products")

    override suspend fun deleteList(listId: String): Boolean {
        val response = FirebaseFunctions.getInstance("europe-west1")
            .getHttpsCallable("deleteList").call(mapOf("listId" to listId)).await()
        return (response.data as? Map<*, *>)?.get("deleted") == true
    }

    override suspend fun searchProducts(prefix: String, limit: Int): List<Product> =
        products.whereArrayContains("searchPrefixes", prefix).limit(limit.toLong())
            .get().await().documents.mapNotNull { it.toProduct() }

    override suspend fun getProducts(barcodes: List<String>): List<Product> =
        barcodes.distinct().mapNotNull { products.document(it).get().await().toProduct() }

    override suspend fun getMembers(listId: String): List<Member> =
        lists.document(listId).collection("members").get().await().documents.mapNotNull { doc ->
            val role = Role.entries.firstOrNull { it.name == doc.getString("role") }
                ?: return@mapNotNull null
            Member(doc.id, role, doc.getString("displayName").orEmpty(),
                doc.getString("email").orEmpty(), doc.millis("joinedAt"))
        }

    private fun DocumentSnapshot.toProduct(): Product? {
        val name = getString("name") ?: return null
        val prices = ChainId.entries.mapNotNull { chain ->
            val price = getDouble("prices.${chain.id}.price") ?: return@mapNotNull null
            chain to ChainPrice(price, millis("prices.${chain.id}.updatedAt"))
        }.toMap()
        return Product(id, name, getString("brand").orEmpty(),
            getString("unitDescription").orEmpty(), prices)
    }

    /** המשתמשת המחוברת ב-Firebase Authentication (משימה 12). */
    override fun currentUid(): String? = auth.currentUser?.uid

    // The live email invitation collection and callable are pending team integration.
    override suspend fun pendingInvitations(): List<InviteLink> = emptyList()

    override suspend fun respondToPendingInvitation(token: String, accept: Boolean): String? =
        throw AppException(ErrorCode.LINK_NOT_FOUND)

    override suspend fun updateBudget(listId: String, amount: Int) {
        lists.document(listId).update(
            mapOf("budget" to amount, "updatedAt" to FieldValue.serverTimestamp()),
        ).await()
    }

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
        ).await()

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
            .await()
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
        val doc = items(listId).document()
        doc.set(itemFields(item)).await()
        return item.copy(id = doc.id)
    }

    override suspend fun addCatalogItem(listId: String, item: ListItem): ListItem {
        val collection = items(listId)
        // Older releases used random document IDs. Continue updating those rows.
        val previous = collection.whereEqualTo("barcode", item.barcode).limit(1).get().await()
        val canonical = collection.document(item.barcode)
        val target = previous.documents.firstOrNull()?.reference ?: canonical
        val result = db.runTransaction { transaction ->
            val previousItem = transaction.get(target).toListItem()
            val document = if (previousItem == null) canonical else target
            val existing = if (previousItem == null && target != canonical) {
                transaction.get(canonical).toListItem()
            } else previousItem
            if (existing == null) {
                transaction.set(canonical, itemFields(item))
                item.copy(id = canonical.id)
            } else {
                if (existing.quantity >= 999) return@runTransaction null
                val updated = existing.copy(quantity = existing.quantity + 1)
                transaction.update(document, "quantity", updated.quantity, "updatedAt", FieldValue.serverTimestamp())
                updated
            }
        }.await()
        return result ?: throw AppException(ErrorCode.INVALID_QUANTITY)
    }

    private fun itemFields(item: ListItem): Map<String, Any> = mapOf(
        "barcode" to item.barcode,
        "name" to item.name,
        "quantity" to item.quantity,
        "note" to item.note,
        "addedBy" to item.addedBy,
        "addedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
        "isFree" to item.barcode.isEmpty(),
    ).filterKeys { it != "isFree" || item.barcode.isEmpty() }

    /** שינוי כמות או הערה של פריט (מסך 08). */
    override suspend fun updateItem(listId: String, item: ListItem) {
        items(listId).document(item.id)
            .update(
                mapOf(
                    "quantity" to item.quantity,
                    "note" to item.note,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
    }

    /** מחיקת פריט מהרשימה (מסך 08). */
    override suspend fun deleteItem(listId: String, itemId: String) {
        items(listId).document(itemId).delete().await()
    }

    override suspend fun updateItemQuantity(listId: String, item: ListItem) {
        items(listId).document(item.id)
            .update("quantity", item.quantity, "updatedAt", FieldValue.serverTimestamp()).await()
    }

    override suspend fun updateItemNote(listId: String, item: ListItem) {
        items(listId).document(item.id)
            .update("note", item.note, "updatedAt", FieldValue.serverTimestamp()).await()
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
            lastBudget = (getLong("budget") ?: getLong("lastBudget"))?.toInt(),
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

}

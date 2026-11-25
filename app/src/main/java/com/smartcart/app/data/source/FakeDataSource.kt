package com.smartcart.app.data.source

import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import com.smartcart.app.data.model.Chain
import com.smartcart.app.data.model.ChainId
import com.smartcart.app.data.model.Comparison
import com.smartcart.app.data.model.ComparisonMode
import com.smartcart.app.data.model.ComparisonResult
import com.smartcart.app.data.model.ComparisonStatus
import com.smartcart.app.data.model.InviteLink
import com.smartcart.app.data.model.ListItem
import com.smartcart.app.data.model.Member
import com.smartcart.app.data.model.MissingItem
import com.smartcart.app.data.model.Product
import com.smartcart.app.data.model.Role
import com.smartcart.app.data.model.ShoppingList
import com.smartcart.app.data.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * מקור נתונים מדומה: שומר הכל בזיכרון של הטלפון ומחזיר תשובות דמה לפי פרק 14 במסמך האפיון.
 * הוא גם מחקה את מה שהשרת עושה (הטריגרים ופונקציות הענן), כדי שאפשר יהיה לבנות ולבדוק
 * את כל המסכים לפני שהחיבור ל-Firebase מוכן.
 *
 * @param delayMs השהיה מלאכותית לכל קריאה, כדי לראות במסכים מצב טעינה. בבדיקות מעבירים 0.
 * @param seed האם להתחיל עם משתמשת דמה ורשימה לדוגמה.
 */
class FakeDataSource(
    private val delayMs: Long = 300,
    seed: Boolean = true,
) : DataSource {

    /** המשתמשת שנחשבת מחוברת. עד שתהיה שכבת אימות (משימה 12), זו משתמשת הדמה. */
    var signedInUid: String? = FakeData.DEMO_UID

    private val users = mutableMapOf<String, User>()
    private val lists = mutableMapOf<String, ShoppingList>()
    private val items = mutableMapOf<String, MutableMap<String, ListItem>>()   // listId → פריטים
    private val members = mutableMapOf<String, MutableMap<String, Member>>()  // listId → משתתפות
    private val inviteLinks = mutableMapOf<String, InviteLink>()
    private val comparisons = mutableMapOf<String, Comparison>()
    private val products: Map<String, Product>
    private val chains: List<Chain>
    private val changes = MutableStateFlow(0)

    init {
        val now = now()
        products = FakeData.products(now).associateBy { it.barcode }
        chains = FakeData.chains(now)
        if (seed) seedDemo(now)
    }

    override fun currentUid(): String? = signedInUid

    // ---------- users ----------

    override suspend fun getUser(uid: String): User? {
        network()
        return users[uid]
    }

    override suspend fun saveUser(user: User) {
        network()
        users[user.uid] = user
    }

    // ---------- shoppingLists ----------

    override suspend fun getListsForMember(uid: String): List<ShoppingList> {
        network()
        return lists.values.filter { uid in it.memberUids }.sortedByDescending { it.updatedAt }
    }

    override suspend fun getListsOwnedBy(uid: String): List<ShoppingList> {
        network()
        return lists.values.filter { it.ownerUid == uid }.sortedByDescending { it.updatedAt }
    }

    override suspend fun getList(listId: String): ShoppingList? {
        network()
        return lists[listId]
    }

    override suspend fun createList(name: String, ownerUid: String): ShoppingList {
        network()
        return insertList(name, ownerUid, now())
    }

    override suspend fun renameList(listId: String, name: String) {
        network()
        val list = requireOwnedList(listId)
        lists[listId] = list.copy(name = name, updatedAt = now())
        notifyChanged()
    }

    // ---------- items ----------

    override suspend fun getItems(listId: String): List<ListItem> {
        network()
        return items[listId]?.values?.sortedBy { it.addedAt }.orEmpty()
    }

    override suspend fun addItem(listId: String, item: ListItem): ListItem {
        network()
        requireMember(listId)
        return insertItem(listId, item)
    }

    override suspend fun updateItem(listId: String, item: ListItem) {
        network()
        requireMember(listId)
        val listItems = items.getValue(listId)
        if (item.id in listItems) {
            listItems[item.id] = item.copy(updatedAt = now())
            onItemWritten(listId)
        }
    }

    override suspend fun deleteItem(listId: String, itemId: String) {
        network()
        requireMember(listId)
        if (items[listId]?.remove(itemId) != null) onItemWritten(listId)
    }

    // ---------- משימה 21: סנכרון בזמן אמת ----------

    /**
     * מונה שינויים: כל כתיבה מגדילה אותו (notifyChanged), וכל מי שמאזין מקבל את המצב החדש.
     * כך נתוני הדמה מתנהגים כמו Firestore, שמודיע על כל שינוי.
     */
    private fun notifyChanged() = changes.update { it + 1 }

    override fun observeList(listId: String): Flow<ShoppingList?> =
        changes.map { lists[listId] }.distinctUntilChanged() // שולחים רק כשהרשימה באמת השתנתה

    override fun observeItems(listId: String): Flow<List<ListItem>> =
        changes.map { items[listId]?.values?.sortedBy { it.addedAt }.orEmpty() }.distinctUntilChanged()

    // ---------- members ----------

    override suspend fun getMembers(listId: String): List<Member> {
        network()
        return members[listId]?.values?.sortedBy { it.joinedAt }.orEmpty()
    }

    // ---------- products, chains ----------

    override suspend fun searchProducts(prefix: String, limit: Int): List<Product> {
        network()
        return products.values
            .filter { product -> product.name.lowercase().split(" ").any { it.startsWith(prefix) } }
            .take(limit)
    }

    override suspend fun getProducts(barcodes: List<String>): List<Product> {
        network()
        return barcodes.mapNotNull { products[it] }
    }

    override suspend fun getChains(): List<Chain> {
        network()
        return chains
    }

    // ---------- comparisons ----------

    override suspend fun createComparison(
        listId: String,
        requestedBy: String,
        mode: ComparisonMode,
        budget: Int?,
    ): String {
        network()
        requireMember(listId)
        val id = newId()
        comparisons[id] = Comparison(
            id = id,
            listId = listId,
            requestedBy = requestedBy,
            mode = mode,
            budget = budget,
            status = ComparisonStatus.PENDING,
            step = 0,
            result = null,
            computeTimeMs = null,
            createdAt = now(),
        )
        return id
    }

    override suspend fun getComparison(comparisonId: String): Comparison? {
        network()
        return comparisons[comparisonId]
    }

    // ---------- inviteLinks ----------

    override suspend fun getInviteLink(token: String): InviteLink? {
        network()
        return inviteLinks[token]
    }

    // ---------- פונקציות הענן ----------

    override suspend fun compareBasket(listId: String, comparisonId: String): ComparisonStatus {
        val uid = requireUid()
        val startedAt = now()
        val comparison = comparisons[comparisonId]
        if (comparison == null || lists[listId]?.memberUids?.contains(uid) != true) {
            throw AppException(ErrorCode.NOT_MEMBER)
        }
        comparisons[comparisonId] = comparison.copy(status = ComparisonStatus.RUNNING, step = 1)
        network()

        val listItems = items[listId]?.values.orEmpty().filter { it.barcode.isNotBlank() }
        if (listItems.isEmpty()) failComparison(comparisonId, ErrorCode.LIST_EMPTY)
        if (comparison.mode == ComparisonMode.BUDGET && (comparison.budget ?: 0) !in 1..10_000) {
            failComparison(comparisonId, ErrorCode.INVALID_BUDGET)
        }

        val result = computeResult(listItems, comparison)
        comparisons[comparisonId] = comparison.copy(
            status = ComparisonStatus.DONE,
            step = 5,
            result = result,
            computeTimeMs = now() - startedAt,
        )
        // בסיום, הפונקציה שומרת ברשימה את תוצאת ההשוואה האחרונה ומגדילה את המונה של המשתמשת
        lists[listId]?.let { list ->
            lists[listId] = list.copy(
                lastComparisonId = comparisonId,
                lastCheapestChain = result.cheapestChain,
                lastCheapestTotal = result.cheapestChain?.let { result.totals[it] },
                lastBudget = comparison.budget,
            )
        }
        users[uid]?.let { users[uid] = it.copy(comparisonCount = it.comparisonCount + 1) }
        notifyChanged()
        return ComparisonStatus.DONE
    }

    override suspend fun createInviteLink(listId: String): String {
        network()
        val list = requireOwnedList(listId)
        // לכל רשימה יש קישור קבוצתי אחד: אם כבר יש קישור פעיל, מחזירים אותו
        val current = list.inviteToken?.let { inviteLinks[it] }
        if (current != null && current.active) return FakeData.INVITE_BASE_URL + current.token

        val token = UUID.randomUUID().toString().replace("-", "")
        inviteLinks[token] = InviteLink(
            token = token,
            listId = listId,
            listName = list.name,
            ownerName = users[list.ownerUid]?.displayName.orEmpty(),
            itemCount = list.itemCount,
            memberNames = members[listId]?.values?.map { it.displayName }.orEmpty(),
            createdAt = now(),
            active = true,
        )
        lists[listId] = list.copy(inviteToken = token)
        notifyChanged()
        return FakeData.INVITE_BASE_URL + token
    }

    override suspend fun joinList(token: String): String {
        network()
        val uid = requireUid()
        val link = inviteLinks[token] ?: throw AppException(ErrorCode.LINK_NOT_FOUND)
        if (!link.active) throw AppException(ErrorCode.LINK_CANCELLED)
        val list = lists[link.listId] ?: throw AppException(ErrorCode.LINK_NOT_FOUND)
        if (uid in list.memberUids) throw AppException(ErrorCode.ALREADY_MEMBER)

        // מי שמצטרפת דרך הקישור מקבלת הרשאת עריכה (פרק 14.3)
        val user = users[uid]
        members.getOrPut(list.id) { mutableMapOf() }[uid] = Member(
            uid = uid,
            role = Role.EDITOR,
            displayName = user?.displayName.orEmpty(),
            email = user?.email.orEmpty(),
            joinedAt = now(),
        )
        lists[list.id] = list.copy(memberUids = list.memberUids + uid)
        notifyChanged()
        inviteLinks[token] = link.copy(memberNames = link.memberNames + user?.displayName.orEmpty())
        return list.id
    }

    override suspend fun cancelInviteLink(listId: String): Boolean {
        network()
        val list = requireOwnedList(listId)
        list.inviteToken?.let { token ->
            inviteLinks[token]?.let { inviteLinks[token] = it.copy(active = false) }
        }
        lists[listId] = list.copy(inviteToken = null)
        notifyChanged()
        return true
    }

    override suspend fun removeMember(listId: String, memberUid: String): Boolean {
        network()
        val list = requireOwnedList(listId)
        if (memberUid == list.ownerUid) throw AppException(ErrorCode.CANNOT_REMOVE_OWNER)
        if (memberUid !in list.memberUids) throw AppException(ErrorCode.MEMBER_NOT_FOUND)
        members[listId]?.remove(memberUid)
        lists[listId] = list.copy(memberUids = list.memberUids - memberUid)
        notifyChanged()
        return true
    }

    override suspend fun deleteList(listId: String): Boolean {
        network()
        val list = requireOwnedList(listId)
        removeListData(list)
        return true
    }

    override suspend fun deleteAccount(): Boolean {
        network()
        // בשרת הפונקציה דורשת התחברות מהחמש דקות האחרונות (RECENT_LOGIN_REQUIRED); כאן זה לא נבדק
        val uid = requireUid()
        lists.values.filter { it.ownerUid == uid }.forEach { removeListData(it) }
        lists.values.filter { uid in it.memberUids }.forEach { list ->
            members[list.id]?.remove(uid)
            lists[list.id] = list.copy(memberUids = list.memberUids - uid)
        }
        users.remove(uid)
        signedInUid = null
        notifyChanged()
        return true
    }

    // ---------- עזרים פנימיים ----------

    /** השהיה מלאכותית, כמו זמן התגובה של הרשת. */
    private suspend fun network() {
        if (delayMs > 0) delay(delayMs)
    }

    private fun now() = System.currentTimeMillis()

    private fun newId() = UUID.randomUUID().toString()

    private fun requireUid(): String = signedInUid ?: throw AppException(ErrorCode.NOT_SIGNED_IN)

    /** כמו כללי האבטחה: רק משתתפת ברשימה יכולה לכתוב בה. */
    private fun requireMember(listId: String): ShoppingList {
        val uid = requireUid()
        val list = lists[listId]
        if (list == null || uid !in list.memberUids) throw AppException(ErrorCode.NOT_MEMBER)
        return list
    }

    /** כמו הבדיקה בפונקציות הענן: רק בעלת הרשימה יכולה לבצע את הפעולה. */
    private fun requireOwnedList(listId: String): ShoppingList {
        val uid = requireUid()
        val list = lists[listId]
        if (list == null || list.ownerUid != uid) throw AppException(ErrorCode.NOT_OWNER)
        return list
    }

    private fun insertList(name: String, ownerUid: String, now: Long): ShoppingList {
        val list = ShoppingList(
            id = newId(),
            name = name,
            ownerUid = ownerUid,
            memberUids = listOf(ownerUid),
            createdAt = now,
            updatedAt = now,
        )
        lists[list.id] = list
        items[list.id] = mutableMapOf()
        onListCreated(list)
        notifyChanged()
        return list
    }

    private fun insertItem(listId: String, item: ListItem): ListItem {
        val saved = item.copy(id = newId())
        items.getOrPut(listId) { mutableMapOf() }[saved.id] = saved
        onItemWritten(listId)
        return saved
    }

    private fun removeListData(list: ShoppingList) {
        lists.remove(list.id)
        items.remove(list.id)
        members.remove(list.id)
        list.inviteToken?.let { inviteLinks.remove(it) }
        notifyChanged()
    }

    /** מחקה את הטריגר onListCreated: בעלת הרשימה נרשמת כמשתתפת בתפקיד OWNER. */
    private fun onListCreated(list: ShoppingList) {
        val owner = users[list.ownerUid]
        members[list.id] = mutableMapOf(
            list.ownerUid to Member(
                uid = list.ownerUid,
                role = Role.OWNER,
                displayName = owner?.displayName.orEmpty(),
                email = owner?.email.orEmpty(),
                joinedAt = list.createdAt,
            ),
        )
    }

    /** מחקה את הטריגר onItemWritten: מעדכן ברשימה את מספר הפריטים ואת זמן העדכון. */
    private fun onItemWritten(listId: String) {
        val list = lists[listId] ?: return
        lists[listId] = list.copy(itemCount = items[listId]?.size ?: 0, updatedAt = now())
        notifyChanged()
    }

    private fun failComparison(comparisonId: String, code: ErrorCode): Nothing {
        comparisons[comparisonId]?.let {
            comparisons[comparisonId] = it.copy(status = ComparisonStatus.FAILED)
        }
        throw AppException(code)
    }

    /**
     * מחקה את החישוב של compareBasket (פרק 3.4): עלות הסל המשותף בכל רשת, הרשת הזולה,
     * הפריטים החסרים, ובהשוואה לפי תקציב גם כמה נשאר בכל רשת.
     * החישוב נעשה באגורות (מספרים שלמים) כדי למנוע שגיאות עיגול.
     */
    private fun computeResult(listItems: List<ListItem>, comparison: Comparison): ComparisonResult {
        val allChains = ChainId.entries
        val (common, missing) = listItems.partition { item ->
            products[item.barcode]?.prices?.keys?.containsAll(allChains) == true
        }
        val totalsAgorot = allChains.associateWith { chain ->
            common.sumOf { item ->
                toAgorot(products.getValue(item.barcode).prices.getValue(chain).price) * item.quantity
            }
        }
        val budgetLeft = comparison.budget
            ?.takeIf { comparison.mode == ComparisonMode.BUDGET }
            ?.let { budget -> totalsAgorot.mapValues { (budget * 100L - it.value) / 100.0 } }

        return ComparisonResult(
            commonItems = common.size,
            missingItems = missing.map { item ->
                val prices = products[item.barcode]?.prices.orEmpty()
                MissingItem(item.barcode, item.name, item.quantity, foundIn = allChains.filter { it in prices })
            },
            totals = totalsAgorot.mapValues { it.value / 100.0 },
            cheapestChain = if (common.isEmpty()) null else totalsAgorot.minBy { it.value }.key,
            budgetLeft = budgetLeft,
            pricesUpdatedAt = chains.mapNotNull { chain -> chain.lastSuccessAt?.let { chain.id to it } }.toMap(),
        )
    }

    private fun toAgorot(price: Double): Long = Math.round(price * 100)

    /** משתמשת דמה ורשימה לדוגמה עם ארבעה פריטים, אחד מהם חסר ברשת אחת. */
    private fun seedDemo(now: Long) {
        users[FakeData.DEMO_UID] = User(FakeData.DEMO_UID, FakeData.DEMO_EMAIL, FakeData.DEMO_NAME, now)
        val list = insertList("קניות לשבת", FakeData.DEMO_UID, now)
        listOf("0000000000017" to 2, "0000000000024" to 1, "0000000000031" to 1, "0000000000086" to 1)
            .forEach { (barcode, quantity) ->
                insertItem(
                    list.id,
                    ListItem(
                        id = "",
                        barcode = barcode,
                        name = products.getValue(barcode).name,
                        quantity = quantity,
                        addedBy = FakeData.DEMO_UID,
                        addedAt = now,
                        updatedAt = now,
                    ),
                )
            }
    }
}

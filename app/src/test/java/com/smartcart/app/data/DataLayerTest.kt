package com.smartcart.app.data

import com.smartcart.app.data.model.ChainId.CHATZI_HINAM
import com.smartcart.app.data.model.ChainId.RAMI_LEVY
import com.smartcart.app.data.model.ChainId.SHUFERSAL
import com.smartcart.app.data.model.ComparisonMode
import com.smartcart.app.data.model.Role
import com.smartcart.app.data.repository.ComparisonRepository
import com.smartcart.app.data.repository.ItemRepository
import com.smartcart.app.data.repository.ListRepository
import com.smartcart.app.data.repository.ProductRepository
import com.smartcart.app.data.repository.SharingRepository
import com.smartcart.app.data.repository.UserRepository
import com.smartcart.app.data.source.FakeDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** בדיקות לשכבת הנתונים מול מקור הנתונים המדומה. */
class DataLayerTest {

    private val source = FakeDataSource(delayMs = 0, seed = false)
    private val users = UserRepository(source)
    private val lists = ListRepository(source)
    private val items = ItemRepository(source)
    private val products = ProductRepository(source)
    private val comparisons = ComparisonRepository(source)
    private val sharing = SharingRepository(source)

    private suspend fun product(query: String) = products.search(query).getOrThrow().first()

    private fun errorOf(result: Result<*>) = (result.exceptionOrNull() as AppException).code

    @Test
    fun addingTheSameProductTwiceIncreasesTheQuantity() = runBlocking {
        val list = lists.createList("קניות").getOrThrow()
        val milk = product("חלב")

        items.addProduct(list.id, milk).getOrThrow()
        items.addProduct(list.id, milk).getOrThrow()

        val saved = items.items(list.id).getOrThrow()
        assertEquals(1, saved.size)
        assertEquals(2, saved.first().quantity)
        assertEquals(1, lists.getList(list.id).getOrThrow()!!.itemCount)
    }

    @Test
    fun quantityOutsideTheRangeIsRejected() = runBlocking {
        val list = lists.createList("קניות").getOrThrow()
        val item = items.addProduct(list.id, product("חלב")).getOrThrow()

        assertEquals(ErrorCode.INVALID_QUANTITY, errorOf(items.changeQuantity(list.id, item, 0)))
        assertEquals(ErrorCode.INVALID_QUANTITY, errorOf(items.changeQuantity(list.id, item, 1000)))
    }

    @Test
    fun budgetComparisonCountsOnlyItemsFoundInAllChains() = runBlocking {
        val list = lists.createList("קניות").getOrThrow()
        val milk = product("חלב")
        items.addProduct(list.id, milk).getOrThrow()
        items.addProduct(list.id, milk).getOrThrow()
        items.addProduct(list.id, product("לחם")).getOrThrow()
        items.addProduct(list.id, product("ביצים")).getOrThrow()
        items.addProduct(list.id, product("קפה")).getOrThrow() // חסר ביוחננוף

        val id = comparisons.startComparison(list.id, ComparisonMode.BUDGET, budget = 32).getOrThrow()
        val result = comparisons.comparison(id).getOrThrow()!!.result!!

        assertEquals(3, result.commonItems)
        assertEquals("קפה נמס", result.missingItems.single().name)
        // רמי לוי: 2 × 6.20 + 7.30 + 11.90 = 31.60
        assertEquals(31.60, result.totals.getValue(RAMI_LEVY), 0.001)
        // שופרסל: 2 × 6.90 + 7.30 + 13.50 = 34.60
        assertEquals(34.60, result.totals.getValue(SHUFERSAL), 0.001)
        assertEquals(RAMI_LEVY, result.cheapestChain)
        assertEquals(0.40, result.budgetLeft!!.getValue(RAMI_LEVY), 0.001)
        assertEquals(-0.10, result.budgetLeft!!.getValue(CHATZI_HINAM), 0.001)
        assertEquals(31.60, lists.getList(list.id).getOrThrow()!!.lastCheapestTotal!!, 0.001)
    }

    @Test
    fun emptyListCannotBeCompared() = runBlocking {
        val list = lists.createList("ריקה").getOrThrow()

        assertEquals(ErrorCode.LIST_EMPTY, errorOf(comparisons.startComparison(list.id, ComparisonMode.LIST)))
    }

    @Test
    fun joiningThroughTheLinkAddsAnEditorOnce() = runBlocking {
        users.createProfile("בעלת הרשימה", "Owner@Test.com").getOrThrow()
        val list = lists.createList("משותפת").getOrThrow()
        val token = sharing.createInviteLink(list.id).getOrThrow().substringAfterLast("/")

        source.signedInUid = "friend"
        users.createProfile("חברה", "friend@test.com").getOrThrow()

        assertEquals(list.id, sharing.joinList(token).getOrThrow())
        assertEquals(ErrorCode.ALREADY_MEMBER, errorOf(sharing.joinList(token)))
        assertEquals(listOf(Role.OWNER, Role.EDITOR), sharing.members(list.id).getOrThrow().map { it.role })
    }

    @Test
    fun cancelledLinkCannotBeUsed() = runBlocking {
        val list = lists.createList("משותפת").getOrThrow()
        val token = sharing.createInviteLink(list.id).getOrThrow().substringAfterLast("/")
        sharing.cancelInviteLink(list.id).getOrThrow()

        source.signedInUid = "friend"
        assertEquals(ErrorCode.LINK_CANCELLED, errorOf(sharing.joinList(token)))
    }
}

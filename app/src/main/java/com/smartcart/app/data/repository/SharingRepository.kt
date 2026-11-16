package com.smartcart.app.data.repository

import com.smartcart.app.data.model.InviteLink
import com.smartcart.app.data.model.Member
import com.smartcart.app.data.safeCall
import com.smartcart.app.data.source.DataSource

/** שיתוף רשימה דרך קישור הזמנה, והמשתתפות ברשימה (מסכים 10, 15, 17). */
class SharingRepository(private val dataSource: DataSource) {

    suspend fun members(listId: String): Result<List<Member>> = safeCall { dataSource.getMembers(listId) }

    /** הקישור הקבוצתי של הרשימה (מסך 10). רק בעלת הרשימה יכולה. */
    suspend fun createInviteLink(listId: String): Result<String> = safeCall {
        dataSource.createInviteLink(listId)
    }

    /** פרטי הרשימה שמוצגים במסך 15 לפני ההצטרפות. */
    suspend fun inviteLink(token: String): Result<InviteLink?> = safeCall { dataSource.getInviteLink(token) }

    /** הצטרפות לרשימה (מסך 15). מחזירה את המזהה של הרשימה. */
    suspend fun joinList(token: String): Result<String> = safeCall { dataSource.joinList(token) }

    suspend fun cancelInviteLink(listId: String): Result<Boolean> = safeCall {
        dataSource.cancelInviteLink(listId)
    }

    /** הסרת משתתפת (מסך 17). רק בעלת הרשימה יכולה. */
    suspend fun removeMember(listId: String, memberUid: String): Result<Boolean> = safeCall {
        dataSource.removeMember(listId, memberUid)
    }
}

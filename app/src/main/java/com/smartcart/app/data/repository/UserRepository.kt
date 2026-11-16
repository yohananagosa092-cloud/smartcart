package com.smartcart.app.data.repository

import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import com.smartcart.app.data.model.User
import com.smartcart.app.data.safeCall
import com.smartcart.app.data.source.DataSource
import com.smartcart.app.data.source.requireUid

/** הפרופיל של המשתמשת (מסכים 03, 18). */
class UserRepository(private val dataSource: DataSource) {

    /** הפרופיל של המשתמשת המחוברת, או null אם אין משתמשת מחוברת. */
    suspend fun currentUser(): Result<User?> = safeCall {
        dataSource.currentUid()?.let { dataSource.getUser(it) }
    }

    /** יוצרת את הפרופיל אחרי אימות כתובת הדוא״ל. הכתובת נשמרת באותיות קטנות. */
    suspend fun createProfile(displayName: String, email: String): Result<User> = safeCall {
        val user = User(
            uid = dataSource.requireUid(),
            email = email.trim().lowercase(),
            displayName = validName(displayName),
            createdAt = System.currentTimeMillis(),
        )
        dataSource.saveUser(user)
        user
    }

    /** שינוי השם במסך 18. */
    suspend fun updateDisplayName(displayName: String): Result<User> = safeCall {
        val user = dataSource.getUser(dataSource.requireUid()) ?: throw AppException(ErrorCode.NOT_SIGNED_IN)
        val updated = user.copy(displayName = validName(displayName))
        dataSource.saveUser(updated)
        updated
    }

    suspend fun deleteAccount(): Result<Boolean> = safeCall { dataSource.deleteAccount() }

    /** שם מלא: 2 עד 40 תווים. */
    private fun validName(name: String): String =
        name.trim().also { if (it.length !in 2..40) throw AppException(ErrorCode.INVALID_NAME) }
}

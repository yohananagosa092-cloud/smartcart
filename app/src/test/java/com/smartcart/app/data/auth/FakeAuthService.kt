package com.smartcart.app.data.auth

import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode

/** שירות אימות מדומה לבדיקות: שומר את החשבונות בזיכרון. */
class FakeAuthService : AuthService {

    private data class Account(val user: AuthUser, val password: String)

    private val accounts = mutableMapOf<String, Account>() // email → חשבון
    private var current: String? = null

    /** כמה קישורי אימות נשלחו, כדי שהבדיקה תוודא שנשלח קישור. */
    var verificationEmailsSent = 0
        private set

    /** מדמה לחיצה על קישור האימות שבמייל. */
    fun verifyEmail(email: String) {
        accounts[email]?.let { accounts[email] = it.copy(user = it.user.copy(isEmailVerified = true)) }
    }

    override fun currentUser(): AuthUser? = current?.let { accounts[it]?.user }

    override suspend fun signUp(displayName: String, email: String, password: String): AuthUser {
        if (email in accounts) throw AppException(ErrorCode.EMAIL_IN_USE)
        val user = AuthUser("uid_${accounts.size + 1}", email, displayName, isEmailVerified = false)
        accounts[email] = Account(user, password)
        current = email
        return user
    }

    override suspend fun signIn(email: String, password: String): AuthUser {
        val account = accounts[email]
        if (account == null || account.password != password) throw AppException(ErrorCode.WRONG_CREDENTIALS)
        current = email
        return account.user
    }

    override suspend fun sendEmailVerification() {
        if (current == null) throw AppException(ErrorCode.NOT_SIGNED_IN)
        verificationEmailsSent++
    }

    override suspend fun reloadEmailVerified(): Boolean =
        currentUser()?.isEmailVerified ?: throw AppException(ErrorCode.NOT_SIGNED_IN)

    override suspend fun deleteUnverifiedAccount(): Boolean {
        if (reloadEmailVerified()) return false
        val email = current ?: throw AppException(ErrorCode.NOT_SIGNED_IN)
        accounts.remove(email)
        current = null
        return true
    }

    override suspend fun sendPasswordReset(email: String) = Unit

    override suspend fun reauthenticate(password: String) {
        val account = current?.let { accounts[it] } ?: throw AppException(ErrorCode.NOT_SIGNED_IN)
        if (account.password != password) throw AppException(ErrorCode.WRONG_CREDENTIALS)
    }

    override suspend fun updatePassword(newPassword: String) {
        val email = current ?: throw AppException(ErrorCode.NOT_SIGNED_IN)
        accounts[email] = accounts.getValue(email).copy(password = newPassword)
    }

    override fun signOut() {
        current = null
    }
}

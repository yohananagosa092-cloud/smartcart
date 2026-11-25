package com.smartcart.app.data.auth

import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import com.smartcart.app.data.safeCall

/**
 * שכבת האימות (מסכים 01–04 ו-18): הרשמה, התחברות, אימות כתובת הדוא״ל, איפוס סיסמה,
 * שמירת ההתחברות, התנתקות ושינוי סיסמה. בודקת את הקלט לפני הפנייה ל-Firebase.
 */
class AuthRepository(private val auth: AuthService) {

    private var lastVerification: Pair<String, Long>? = null

    /** המשתמשת המחוברת, או null. כך האפליקציה יודעת לדלג על ההתחברות בפתיחה הבאה. */
    fun currentUser(): AuthUser? = auth.currentUser()

    /**
     * הרשמה (מסך 02): יוצרת חשבון ושולחת קישור לאימות הכתובת.
     * אחרי ההרשמה המשתמשת עוברת למסך 03.
     */
    suspend fun signUp(displayName: String, email: String, password: String): Result<AuthUser> = safeCall {
        val name = displayName.trim()
        if (name.length !in 2..40) throw AppException(ErrorCode.INVALID_NAME)
        val user = auth.signUp(name, validEmail(email), validNewPassword(password))
        // החשבון כבר נוצר; אם שליחת הקישור נכשלה, אפשר לשלוח שוב ממסך 03
        resendVerification()
        user
    }

    /**
     * התחברות (מסך 01). אם הכתובת עוד לא אומתה, isEmailVerified הוא false
     * והמסך מעביר למסך 03 במקום למסך הבית.
     */
    suspend fun signIn(email: String, password: String): Result<AuthUser> = safeCall {
        if (password.isEmpty()) throw AppException(ErrorCode.WRONG_CREDENTIALS)
        auth.signIn(validEmail(email), password)
    }

    /** "שלחי שוב" במסך 03. */
    suspend fun resendVerification(): Result<Unit> = safeCall {
        val uid = currentUser()?.uid ?: throw AppException(ErrorCode.NOT_SIGNED_IN)
        auth.sendEmailVerification()
        lastVerification = uid to System.currentTimeMillis()
    }

    fun resendAvailableAt(): Long = lastVerification
        ?.takeIf { it.first == currentUser()?.uid }
        ?.let { it.second + 60_000 } ?: 0

    suspend fun deleteUnverifiedAccount(): Result<Boolean> = safeCall { auth.deleteUnverifiedAccount() }

    /** "אימתתי, המשך" במסך 03: בודקת מול השרת אם המשתמשת כבר לחצה על הקישור. */
    suspend fun checkEmailVerified(): Result<Boolean> = safeCall { auth.reloadEmailVerified() }

    /** איפוס סיסמה (מסך 04). */
    suspend fun resetPassword(email: String): Result<Unit> = safeCall {
        auth.sendPasswordReset(validEmail(email))
    }

    /** שינוי סיסמה (מסך 18): קודם אימות מחדש עם הסיסמה הנוכחית, ואז הסיסמה החדשה. */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = safeCall {
        val password = validNewPassword(newPassword)
        auth.reauthenticate(currentPassword)
        auth.updatePassword(password)
    }

    /** אימות מחדש לפני מחיקת החשבון (מסך 18). */
    suspend fun confirmPassword(password: String): Result<Unit> = safeCall { auth.reauthenticate(password) }

    /** התנתקות (מסך 18). */
    fun signOut() = auth.signOut()

    private fun validEmail(email: String): String {
        val trimmed = email.trim()
        if (!EMAIL_PATTERN.matches(trimmed)) throw AppException(ErrorCode.INVALID_EMAIL)
        return trimmed
    }

    /** סיסמה: לפחות 8 תווים (מסך 02). */
    private fun validNewPassword(password: String): String {
        if (password.length < MIN_PASSWORD_LENGTH) throw AppException(ErrorCode.WEAK_PASSWORD)
        return password
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

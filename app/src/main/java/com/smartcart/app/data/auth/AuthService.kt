package com.smartcart.app.data.auth

/** המשתמשת המחוברת, כפי ש-Firebase Authentication מכיר אותה. */
data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val isEmailVerified: Boolean,
)

/**
 * כל הפעולות מול שירות האימות. המימוש האמיתי הוא [FirebaseAuthService];
 * בבדיקות משתמשים במימוש מדומה.
 * פעולה שנכשלת זורקת AppException עם קוד השגיאה.
 */
interface AuthService {

    /** המשתמשת המחוברת, או null. Firebase שומר את ההתחברות במכשיר עד להתנתקות. */
    fun currentUser(): AuthUser?

    /** יוצרת חשבון חדש ושומרת בו את השם המלא. */
    suspend fun signUp(displayName: String, email: String, password: String): AuthUser

    suspend fun signIn(email: String, password: String): AuthUser

    /** שולחת לכתובת של המשתמשת המחוברת קישור לאימות. */
    suspend fun sendEmailVerification()

    /** טוענת מחדש את פרטי המשתמשת מהשרת, ומחזירה אם הכתובת כבר אומתה. */
    suspend fun reloadEmailVerified(): Boolean

    /** שולחת קישור לבחירת סיסמה חדשה (מסך 04). */
    suspend fun sendPasswordReset(email: String)

    /** אימות מחדש עם הסיסמה הנוכחית, לפני שינוי סיסמה או מחיקת חשבון. */
    suspend fun reauthenticate(password: String)

    /** משנה את הסיסמה של המשתמשת המחוברת. צריך לקרוא קודם ל-[reauthenticate]. */
    suspend fun updatePassword(newPassword: String)

    fun signOut()
}

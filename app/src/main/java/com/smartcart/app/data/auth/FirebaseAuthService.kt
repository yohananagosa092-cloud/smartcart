package com.smartcart.app.data.auth

import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

/**
 * האימות מול Firebase Authentication בשיטת דוא״ל וסיסמה.
 * הסיסמה נשלחת ל-Firebase ואינה נשמרת באפליקציה.
 */
class FirebaseAuthService(
    private val auth: FirebaseAuth = Firebase.auth,
) : AuthService {

    override fun currentUser(): AuthUser? = auth.currentUser?.toAuthUser()

    override suspend fun signUp(displayName: String, email: String, password: String): AuthUser = call {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw AppException(ErrorCode.NOT_SIGNED_IN)
        user.updateProfile(userProfileChangeRequest { this.displayName = displayName }).await()
        user.toAuthUser()
    }

    override suspend fun signIn(email: String, password: String): AuthUser = call {
        auth.signInWithEmailAndPassword(email, password).await().user?.toAuthUser()
            ?: throw AppException(ErrorCode.NOT_SIGNED_IN)
    }

    override suspend fun sendEmailVerification() = call {
        requireUser().sendEmailVerification().await()
        Unit
    }

    override suspend fun reloadEmailVerified(): Boolean = call {
        val user = requireUser()
        user.reload().await()
        if (user.isEmailVerified) user.getIdToken(true).await()
        user.isEmailVerified
    }

    override suspend fun deleteUnverifiedAccount(): Boolean = call {
        val user = requireUser()
        user.reload().await()
        if (user.isEmailVerified) {
            user.getIdToken(true).await()
            false
        } else {
            user.delete().await()
            true
        }
    }

    override suspend fun sendPasswordReset(email: String) = call {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (error: FirebaseAuthInvalidUserException) {
            if (error.errorCode != "ERROR_USER_NOT_FOUND" && error.errorCode != "ERROR_USER_DISABLED") {
                throw error
            }
        }
        Unit
    }

    override suspend fun reauthenticate(password: String) = call {
        val user = requireUser()
        val credential = EmailAuthProvider.getCredential(user.email.orEmpty(), password)
        user.reauthenticate(credential).await()
        Unit
    }

    override suspend fun updatePassword(newPassword: String) = call {
        requireUser().updatePassword(newPassword).await()
        Unit
    }

    override fun signOut() = auth.signOut()

    private fun requireUser(): FirebaseUser = auth.currentUser ?: throw AppException(ErrorCode.NOT_SIGNED_IN)

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName.orEmpty(),
        isEmailVerified = isEmailVerified,
    )

    /** מריצה קריאה ל-Firebase ומתרגמת את השגיאות שלו לקודים שלנו. */
    private suspend fun <T> call(block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw toAppException(e)
        }

    private fun toAppException(e: Exception): Exception = when (e) {
        is AppException -> e
        // סיסמה חלשה היא סוג של "פרטים לא תקינים", ולכן נבדקת קודם
        is FirebaseAuthWeakPasswordException -> AppException(ErrorCode.WEAK_PASSWORD)
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> AppException(ErrorCode.WRONG_CREDENTIALS)
        is FirebaseAuthUserCollisionException -> AppException(ErrorCode.EMAIL_IN_USE)
        is FirebaseAuthRecentLoginRequiredException -> AppException(ErrorCode.RECENT_LOGIN_REQUIRED)
        is FirebaseTooManyRequestsException -> AppException(ErrorCode.TOO_MANY_ATTEMPTS)
        is FirebaseNetworkException -> AppException(ErrorCode.NETWORK_ERROR)
        else -> e
    }
}

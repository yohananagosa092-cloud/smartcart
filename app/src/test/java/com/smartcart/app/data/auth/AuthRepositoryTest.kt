package com.smartcart.app.data.auth

import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** בדיקות לשכבת האימות מול שירות אימות מדומה. */
class AuthRepositoryTest {

    private val service = FakeAuthService()
    private val auth = AuthRepository(service)

    private fun errorOf(result: Result<*>) = (result.exceptionOrNull() as AppException).code

    @Test
    fun signUpCreatesAnUnverifiedAccountAndSendsAVerificationLink() = runBlocking<Unit> {
        val user = auth.signUp("  יסכה  ", " yiska@test.com ", "12345678").getOrThrow()

        assertEquals("יסכה", user.displayName)
        assertEquals("yiska@test.com", user.email)
        assertFalse(user.isEmailVerified)
        assertEquals(1, service.verificationEmailsSent)
    }

    @Test
    fun signUpChecksTheNameEmailAndPassword() = runBlocking<Unit> {
        assertEquals(ErrorCode.INVALID_NAME, errorOf(auth.signUp("י", "yiska@test.com", "12345678")))
        assertEquals(ErrorCode.INVALID_EMAIL, errorOf(auth.signUp("יסכה", "yiska.test.com", "12345678")))
        assertEquals(ErrorCode.WEAK_PASSWORD, errorOf(auth.signUp("יסכה", "yiska@test.com", "1234567")))
        assertNull(auth.currentUser())
    }

    @Test
    fun resetPasswordValidatesAndTrimsBeforeMakingTheRequest() = runBlocking<Unit> {
        assertEquals(ErrorCode.INVALID_EMAIL, errorOf(auth.resetPassword("incorrect")))
        assertTrue(service.requestedResetEmails.isEmpty())

        auth.resetPassword("  example@example.invalid  ").getOrThrow()

        assertEquals(listOf("example@example.invalid"), service.requestedResetEmails)
    }

    @Test
    fun verificationResendWindowStartsAtTheLastSuccessfulSend() = runBlocking<Unit> {
        val before = System.currentTimeMillis()
        auth.signUp("יסכה", "yiska@test.com", "12345678").getOrThrow()
        val after = System.currentTimeMillis()

        assertTrue(auth.resendAvailableAt() in (before + 60_000)..(after + 60_000))
        auth.signOut()
        assertEquals(0L, auth.resendAvailableAt())
    }

    @Test
    fun verificationIsCheckedAgainstTheServer() = runBlocking<Unit> {
        auth.signUp("יסכה", "yiska@test.com", "12345678").getOrThrow()
        assertFalse(auth.checkEmailVerified().getOrThrow())

        service.verifyEmail("yiska@test.com")

        assertTrue(auth.checkEmailVerified().getOrThrow())
    }

    @Test
    fun correctingAnEmailDeletesOnlyAnUnverifiedAccount() = runBlocking<Unit> {
        val email = "yiska@test.com"
        auth.signUp("יסכה", email, "12345678").getOrThrow()
        assertTrue(auth.deleteUnverifiedAccount().getOrThrow())
        assertNull(auth.currentUser())

        auth.signUp("יסכה", email, "12345678").getOrThrow()
        service.verifyEmail(email)
        assertFalse(auth.deleteUnverifiedAccount().getOrThrow())
        assertEquals(email, auth.currentUser()?.email)
    }

    @Test
    fun wrongPasswordGivesAGeneralError() = runBlocking<Unit> {
        auth.signUp("יסכה", "yiska@test.com", "12345678").getOrThrow()
        auth.signOut()

        assertEquals(ErrorCode.WRONG_CREDENTIALS, errorOf(auth.signIn("yiska@test.com", "wrong-password")))
        assertEquals(ErrorCode.WRONG_CREDENTIALS, errorOf(auth.signIn("nobody@test.com", "12345678")))
    }

    @Test
    fun signInKeepsTheUserUntilSignOut() = runBlocking<Unit> {
        auth.signUp("יסכה", "yiska@test.com", "12345678").getOrThrow()
        auth.signOut()
        assertNull(auth.currentUser())

        auth.signIn("yiska@test.com", "12345678").getOrThrow()
        assertEquals("yiska@test.com", auth.currentUser()?.email)

        auth.signOut()
        assertNull(auth.currentUser())
    }

    @Test
    fun changePasswordNeedsTheCurrentPassword() = runBlocking<Unit> {
        auth.signUp("יסכה", "yiska@test.com", "12345678").getOrThrow()

        assertEquals(ErrorCode.WRONG_CREDENTIALS, errorOf(auth.changePassword("wrong-password", "87654321")))
        assertEquals(ErrorCode.WEAK_PASSWORD, errorOf(auth.changePassword("12345678", "short")))

        auth.changePassword("12345678", "87654321").getOrThrow()
        auth.signOut()
        auth.signIn("yiska@test.com", "87654321").getOrThrow()
    }
}

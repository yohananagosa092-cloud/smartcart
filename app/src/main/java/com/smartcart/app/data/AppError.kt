package com.smartcart.app.data

import kotlin.coroutines.cancellation.CancellationException

/** קודי השגיאה של שכבת הנתונים. המסכים מציגים הודעה לפי הקוד. */
enum class ErrorCode {
    // קודים שפונקציות הענן מחזירות (פרק 14.1)
    NOT_MEMBER,
    LIST_EMPTY,
    INVALID_BUDGET,
    NOT_OWNER,
    LINK_NOT_FOUND,
    LINK_CANCELLED,
    ALREADY_MEMBER,
    MEMBER_NOT_FOUND,
    CANNOT_REMOVE_OWNER,
    RECENT_LOGIN_REQUIRED,

    // בדיקות שהאפליקציה עושה לפני שהיא שולחת משהו לשרת
    NOT_SIGNED_IN,
    INVALID_NAME,
    INVALID_QUANTITY,
    INVALID_EMAIL,

    // שגיאות של Firebase Authentication (משימה 12)
    /** כתובת או סיסמה שגויות. ההודעה כללית ולא מפרטת מה מהשניים שגוי (מסך 01). */
    WRONG_CREDENTIALS,
    /** סיסמה קצרה מ-8 תווים. */
    WEAK_PASSWORD,
    EMAIL_IN_USE,
    /** Firebase חוסם זמנית אחרי ניסיונות כושלים רבים. */
    TOO_MANY_ATTEMPTS,
    NETWORK_ERROR,
}

/** שגיאה עם קוד מתוך [ErrorCode]. */
class AppException(val code: ErrorCode) : Exception(code.name)

/**
 * מריצה פעולה ומחזירה Result: הצלחה עם הערך, או כישלון עם השגיאה.
 * ביטול של Coroutine לא נתפס כאן, כדי שהביטול ימשיך לעבוד כרגיל.
 */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

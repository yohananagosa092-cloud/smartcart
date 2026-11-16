package com.smartcart.app.data.model

/** התפקיד של משתתפת ברשימה. */
enum class Role { OWNER, EDITOR }

/** משתתפת ברשימה. מסמך shoppingLists/{listId}/members/{uid}. רק פונקציות הענן כותבות אותו. */
data class Member(
    val uid: String,
    val role: Role,
    val displayName: String,
    val email: String,
    val joinedAt: Long,
)

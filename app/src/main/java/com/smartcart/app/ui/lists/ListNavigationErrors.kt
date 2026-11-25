package com.smartcart.app.ui.lists

import com.google.firebase.firestore.FirebaseFirestoreException
import com.smartcart.app.R

internal fun Throwable.listErrorMessage(
    fallback: Int,
    permission: Int = R.string.sc_list_access_error,
): Int = when ((this as? FirebaseFirestoreException)?.code) {
    FirebaseFirestoreException.Code.PERMISSION_DENIED -> permission
    FirebaseFirestoreException.Code.FAILED_PRECONDITION -> R.string.sc_list_index_error
    FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.sc_list_network_error
    else -> fallback
}

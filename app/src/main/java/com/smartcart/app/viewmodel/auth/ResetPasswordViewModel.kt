package com.smartcart.app.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 04 · איפוס סיסמה. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class ResetPasswordViewModel : ViewModel() {

    private val _title = MutableLiveData("04 · איפוס סיסמה")
    val title: LiveData<String> = _title
}

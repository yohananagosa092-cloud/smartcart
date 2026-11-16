package com.smartcart.app.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 03 · אימות מייל. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class VerifyEmailViewModel : ViewModel() {

    private val _title = MutableLiveData("03 · אימות מייל")
    val title: LiveData<String> = _title
}

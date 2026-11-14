package com.smartcart.app.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 01 · התחברות. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class SignInViewModel : ViewModel() {

    private val _title = MutableLiveData("01 · התחברות")
    val title: LiveData<String> = _title
}

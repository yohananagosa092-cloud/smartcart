package com.smartcart.app.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 02 · הרשמה. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class CreateAccountViewModel : ViewModel() {

    private val _title = MutableLiveData("02 · הרשמה")
    val title: LiveData<String> = _title
}

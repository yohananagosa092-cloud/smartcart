package com.smartcart.app.viewmodel.sharing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 17 · ניהול משתתפות. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class MembersViewModel : ViewModel() {

    private val _title = MutableLiveData("17 · ניהול משתתפות")
    val title: LiveData<String> = _title
}

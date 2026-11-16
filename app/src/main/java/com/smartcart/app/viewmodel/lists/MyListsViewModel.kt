package com.smartcart.app.viewmodel.lists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 09 · הרשימות שלי. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class MyListsViewModel : ViewModel() {

    private val _title = MutableLiveData("09 · הרשימות שלי")
    val title: LiveData<String> = _title
}

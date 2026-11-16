package com.smartcart.app.viewmodel.sharing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 16 · רשימות משותפות. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class SharedListsViewModel : ViewModel() {

    private val _title = MutableLiveData("16 · רשימות משותפות")
    val title: LiveData<String> = _title
}

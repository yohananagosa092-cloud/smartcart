package com.smartcart.app.viewmodel.lists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 06 · רשימה חדשה. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class NewListViewModel : ViewModel() {

    private val _title = MutableLiveData("06 · רשימה חדשה")
    val title: LiveData<String> = _title
}

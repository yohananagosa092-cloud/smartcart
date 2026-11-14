package com.smartcart.app.viewmodel.lists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 10 · שיתוף רשימה. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class ShareListViewModel : ViewModel() {

    private val _title = MutableLiveData("10 · שיתוף רשימה")
    val title: LiveData<String> = _title
}

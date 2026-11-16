package com.smartcart.app.viewmodel.lists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 08 · עריכת רשימה. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class ListEditorViewModel : ViewModel() {

    private val _title = MutableLiveData("08 · עריכת רשימה")
    val title: LiveData<String> = _title
}

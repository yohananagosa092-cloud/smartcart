package com.smartcart.app.viewmodel.lists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 05 · מסך הבית. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class HomeViewModel : ViewModel() {

    private val _title = MutableLiveData("05 · מסך הבית")
    val title: LiveData<String> = _title
}

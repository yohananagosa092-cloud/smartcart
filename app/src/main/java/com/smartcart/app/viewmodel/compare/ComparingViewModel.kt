package com.smartcart.app.viewmodel.compare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 12 · חישוב ההשוואה. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class ComparingViewModel : ViewModel() {

    private val _title = MutableLiveData("12 · חישוב ההשוואה")
    val title: LiveData<String> = _title
}

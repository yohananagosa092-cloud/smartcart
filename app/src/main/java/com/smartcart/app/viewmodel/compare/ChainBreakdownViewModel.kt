package com.smartcart.app.viewmodel.compare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 14 · פירוט לפי רשת. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class ChainBreakdownViewModel : ViewModel() {

    private val _title = MutableLiveData("14 · פירוט לפי רשת")
    val title: LiveData<String> = _title
}

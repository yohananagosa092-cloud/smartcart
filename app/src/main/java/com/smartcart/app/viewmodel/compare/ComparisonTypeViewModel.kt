package com.smartcart.app.viewmodel.compare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 11 · סוג ההשוואה. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class ComparisonTypeViewModel : ViewModel() {

    private val _title = MutableLiveData("11 · סוג ההשוואה")
    val title: LiveData<String> = _title
}

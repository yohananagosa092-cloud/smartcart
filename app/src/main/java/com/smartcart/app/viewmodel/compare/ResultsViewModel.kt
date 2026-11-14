package com.smartcart.app.viewmodel.compare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 13 · תוצאות ההשוואה. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class ResultsViewModel : ViewModel() {

    private val _title = MutableLiveData("13 · תוצאות ההשוואה")
    val title: LiveData<String> = _title
}

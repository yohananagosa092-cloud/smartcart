package com.smartcart.app.viewmodel.sharing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 15 · הצטרפות לרשימה. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class JoinListViewModel : ViewModel() {

    private val _title = MutableLiveData("15 · הצטרפות לרשימה")
    val title: LiveData<String> = _title
}

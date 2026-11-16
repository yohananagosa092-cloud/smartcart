package com.smartcart.app.viewmodel.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 18 · הפרופיל שלי. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class ProfileViewModel : ViewModel() {

    private val _title = MutableLiveData("18 · הפרופיל שלי")
    val title: LiveData<String> = _title
}

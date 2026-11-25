package com.smartcart.app.viewmodel.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.smartcart.app.data.ServiceLocator

/** ה-ViewModel של מסך 18 · הפרופיל שלי. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class ProfileViewModel : ViewModel() {
    fun signOut() = ServiceLocator.authRepository.signOut()

    private val _title = MutableLiveData("18 · הפרופיל שלי")
    val title: LiveData<String> = _title
}

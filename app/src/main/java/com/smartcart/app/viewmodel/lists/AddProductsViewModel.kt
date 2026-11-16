package com.smartcart.app.viewmodel.lists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** ה-ViewModel של מסך 07 · הוספת מוצרים. בשלב השלד הוא מחזיק רק את כותרת המסך. */
class AddProductsViewModel : ViewModel() {

    private val _title = MutableLiveData("07 · הוספת מוצרים")
    val title: LiveData<String> = _title
}

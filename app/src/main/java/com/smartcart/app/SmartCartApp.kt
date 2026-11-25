package com.smartcart.app

import android.app.Application
import com.smartcart.app.data.ServiceLocator

/**
 * האפליקציה עצמה. היא נוצרת לפני כל מסך, ונותנת ל-ServiceLocator את ה-Context
 * שצריך כדי לפתוח את מסד הנתונים שבטלפון (Room, משימה 24).
 */
class SmartCartApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}

package com.smartcart.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * משימה 24: מסד הנתונים שבתוך הטלפון (קובץ בשם smartcart.db).
 * @Database = "זה מסד נתונים, ואלה הטבלאות שלו". version עולה כשמשנים את מבנה הטבלאות.
 */
@Database(entities = [RecentProduct::class, SearchQuery::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /** הגישה לשאילתות. Room כותב את המימוש לבד. */
    abstract fun cacheDao(): CacheDao

    companion object {
        /** פותחת את מסד הנתונים, או יוצרת אותו בפתיחה הראשונה של האפליקציה. */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "smartcart.db").build()
    }
}

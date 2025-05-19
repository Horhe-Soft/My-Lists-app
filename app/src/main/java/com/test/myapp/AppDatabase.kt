package com.test.myapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [CheckListItemData::class, CheckListTitleData::class, SettingsInfo::class], version = 6, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun checkListItemDao(): CheckListItemDao
    abstract fun checkListTitleDao(): CheckListTitleDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "checklist_app_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
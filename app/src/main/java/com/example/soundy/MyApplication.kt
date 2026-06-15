package com.example.soundy

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.soundy.db.AppDatabase

class MyApplication : Application() {
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soundy-db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

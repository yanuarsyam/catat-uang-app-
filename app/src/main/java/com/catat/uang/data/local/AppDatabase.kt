package com.catat.uang.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.catat.uang.data.model.Account
import com.catat.uang.data.model.Category
import com.catat.uang.data.model.TransactionEntity

@Database(
    entities = [Account::class, Category::class, TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "catat_uang.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

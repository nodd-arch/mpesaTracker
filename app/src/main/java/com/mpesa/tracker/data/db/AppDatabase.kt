package com.mpesa.tracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mpesa.tracker.data.model.Invoice
import com.mpesa.tracker.data.model.MpesaTransaction

@Database(
    entities = [MpesaTransaction::class, Invoice::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mpesa_tracker_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

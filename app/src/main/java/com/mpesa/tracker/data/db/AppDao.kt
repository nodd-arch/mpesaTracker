package com.mpesa.tracker.data.db

import androidx.room.*
import com.mpesa.tracker.data.model.Invoice
import com.mpesa.tracker.data.model.MpesaTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: MpesaTransaction): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<MpesaTransaction>>

    @Query("SELECT * FROM transactions WHERE transactionId = :txId LIMIT 1")
    suspend fun findByTransactionId(txId: String): MpesaTransaction?

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<MpesaTransaction>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getTransactionsBetweenSync(start: Long, end: Long): List<MpesaTransaction>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSync(): List<MpesaTransaction>

    @Query("UPDATE transactions SET isConfirmed = :confirmed, confirmedAt = :confirmedAt WHERE id = :id")
    suspend fun setConfirmed(id: Long, confirmed: Boolean, confirmedAt: Long = 0L)

    @Query("SELECT * FROM transactions WHERE isConfirmed = 0 ORDER BY timestamp DESC")
    fun getUnconfirmed(): Flow<List<MpesaTransaction>>

    @Query("SELECT * FROM transactions WHERE isConfirmed = 1 ORDER BY confirmedAt DESC")
    fun getConfirmed(): Flow<List<MpesaTransaction>>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT DISTINCT date FROM transactions ORDER BY timestamp DESC")
    fun getDistinctDates(): Flow<List<String>>
}

@Dao
interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: Invoice): Long

    @Update
    suspend fun update(invoice: Invoice)

    @Delete
    suspend fun delete(invoice: Invoice)

    @Query("SELECT * FROM invoices ORDER BY dateCreated DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: Long): Invoice?
}

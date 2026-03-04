package com.mpesa.tracker.data.repository

import com.mpesa.tracker.data.db.AppDatabase
import com.mpesa.tracker.data.model.Invoice
import com.mpesa.tracker.data.model.MpesaTransaction
import kotlinx.coroutines.flow.Flow

class AppRepository(db: AppDatabase) {
    private val txDao = db.transactionDao()
    private val invoiceDao = db.invoiceDao()

    fun getAllTransactions(): Flow<List<MpesaTransaction>> = txDao.getAllTransactions()
    fun getUnconfirmedTransactions(): Flow<List<MpesaTransaction>> = txDao.getUnconfirmed()
    fun getConfirmedTransactions(): Flow<List<MpesaTransaction>> = txDao.getConfirmed()
    suspend fun setConfirmed(id: Long, confirmed: Boolean) = txDao.setConfirmed(id, confirmed)
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<MpesaTransaction>> =
        txDao.getTransactionsBetween(start, end)
    suspend fun getAllTransactionsSync(): List<MpesaTransaction> = txDao.getAllTransactionsSync()
    suspend fun getTransactionsBetweenSync(start: Long, end: Long): List<MpesaTransaction> =
        txDao.getTransactionsBetweenSync(start, end)
    fun getDistinctDates(): Flow<List<String>> = txDao.getDistinctDates()
    suspend fun deleteTransaction(id: Long) = txDao.delete(id)

    fun getAllInvoices(): Flow<List<Invoice>> = invoiceDao.getAllInvoices()
    suspend fun insertInvoice(invoice: Invoice) = invoiceDao.insert(invoice)
    suspend fun updateInvoice(invoice: Invoice) = invoiceDao.update(invoice)
    suspend fun deleteInvoice(invoice: Invoice) = invoiceDao.delete(invoice)
}

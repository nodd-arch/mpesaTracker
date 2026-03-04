package com.mpesa.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpesa.tracker.data.db.AppDatabase
import com.mpesa.tracker.data.model.Invoice
import com.mpesa.tracker.data.model.MpesaTransaction
import com.mpesa.tracker.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(AppDatabase.getInstance(app))

    val transactions: StateFlow<List<MpesaTransaction>> = repo.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<Invoice>> = repo.getAllInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctDates: StateFlow<List<String>> = repo.getDistinctDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTransactionsForDate(date: String): List<MpesaTransaction> {
        return transactions.value.filter { it.date == date }
    }

    suspend fun getAllForExport(): List<MpesaTransaction> = repo.getAllTransactionsSync()

    suspend fun getRangeForExport(start: Long, end: Long): List<MpesaTransaction> =
        repo.getTransactionsBetweenSync(start, end)

    fun deleteTransaction(id: Long) = viewModelScope.launch {
        repo.deleteTransaction(id)
    }

    fun createInvoice(invoice: Invoice) = viewModelScope.launch {
        repo.insertInvoice(invoice)
    }

    fun updateInvoice(invoice: Invoice) = viewModelScope.launch {
        repo.updateInvoice(invoice)
    }

    fun deleteInvoice(invoice: Invoice) = viewModelScope.launch {
        repo.deleteInvoice(invoice)
    }

    fun getDayStartEnd(date: String): Pair<Long, Long> {
        // date format: "M/d/yy" e.g. "3/3/26"
        val parts = date.split("/")
        val cal = Calendar.getInstance()
        if (parts.size == 3) {
            val year = parts[2].toIntOrNull()?.let { if (it < 100) 2000 + it else it } ?: cal.get(Calendar.YEAR)
            cal.set(year, (parts[0].toIntOrNull() ?: 1) - 1, parts[1].toIntOrNull() ?: 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val end = cal.timeInMillis
            return Pair(start, end)
        }
        return Pair(0L, System.currentTimeMillis())
    }
}

package com.mpesa.tracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val clientName: String,
    val clientPhone: String,
    val description: String,
    val amount: Double,
    val dateCreated: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val paidTransactionId: String = ""
)

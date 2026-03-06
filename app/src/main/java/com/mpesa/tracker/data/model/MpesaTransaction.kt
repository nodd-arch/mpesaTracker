package com.mpesa.tracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class MpesaTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: String,       // e.g. UC3BL8IIRM
    val date: String,                // e.g. 3/3/26
    val time: String,                // e.g. 9:39 PM
    val amount: Double,              // e.g. 25.00
    val senderPhone: String,         // e.g. 254114842082
    val senderName: String,          // e.g. TEDDY MAINA MACHARIA
    val newBalance: Double,          // e.g. 1190.00
    val transactionCost: Double,     // e.g. 0.00
    val rawMessage: String,          // original SMS text
    val timestamp: Long,             // System.currentTimeMillis() for sorting
    val simSlot: Int,                // 0 or 1
    val isConfirmed: Boolean = false,
    val confirmedAt: Long = 0L
)

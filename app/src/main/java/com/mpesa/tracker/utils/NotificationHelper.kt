package com.mpesa.tracker.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mpesa.tracker.MainActivity
import com.mpesa.tracker.R
import com.mpesa.tracker.data.model.MpesaTransaction

object NotificationHelper {
    const val CHANNEL_ID = "mpesa_channel"
    const val CHANNEL_NAME = "M-PESA Transactions"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "M-PESA incoming payment alerts"
            enableVibration(true)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showTransactionNotification(context: Context, transaction: MpesaTransaction) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mpesa_notify)
            .setContentTitle("💚 Payment Received")
            .setContentText("${transaction.senderName} sent you ${MpesaParser.formatAmount(transaction.amount)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "From: ${transaction.senderName}\n" +
                        "Amount: ${MpesaParser.formatAmount(transaction.amount)}\n" +
                        "Balance: ${MpesaParser.formatAmount(transaction.newBalance)}"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(transaction.id.toInt(), notification)
    }
}

package com.mpesa.tracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.mpesa.tracker.data.db.AppDatabase
import com.mpesa.tracker.utils.MpesaParser
import com.mpesa.tracker.utils.NotificationHelper
import com.mpesa.tracker.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MpesaSmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive triggered, action: ${intent.action}")

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.d(TAG, "No messages in intent")
            return
        }

        // Detect SIM slot - try multiple known extras across Android versions/manufacturers
        val simSlot = when {
            intent.hasExtra("android.telephony.extra.SLOT_INDEX") ->
                intent.getIntExtra("android.telephony.extra.SLOT_INDEX", 0)
            intent.hasExtra("slot") ->
                intent.getIntExtra("slot", 0)
            intent.hasExtra("simId") ->
                intent.getIntExtra("simId", 0)
            intent.hasExtra("android.telephony.extra.SUBSCRIPTION_INDEX") ->
                intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", 0)
            else -> 0
        }

        // Combine multi-part messages
        val fullBody = messages.joinToString("") { it.messageBody ?: "" }
        val sender = messages.firstOrNull()?.originatingAddress ?: run {
            Log.d(TAG, "No sender address found")
            return
        }

        Log.d(TAG, "SMS from: $sender, slot: $simSlot, body: ${fullBody.take(80)}")

        // Only process M-PESA messages
        if (!MpesaParser.isMpesaSender(sender)) {
            Log.d(TAG, "Not M-PESA sender, ignoring: $sender")
            return
        }

        Log.d(TAG, "M-PESA message detected! Processing...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check which SIMs are being monitored
                val prefs = context.dataStore.data.first()
                val monitorSim1 = prefs[booleanPreferencesKey("monitor_sim1")] ?: true
                val monitorSim2 = prefs[booleanPreferencesKey("monitor_sim2")] ?: true

                // Be permissive: unknown slot always processes
                val shouldProcess = when (simSlot) {
                    0 -> monitorSim1
                    1 -> monitorSim2
                    else -> true
                }

                if (!shouldProcess) {
                    Log.d(TAG, "SIM slot $simSlot not monitored, skipping")
                    return@launch
                }

                val transaction = MpesaParser.parse(fullBody, simSlot)
                if (transaction == null) {
                    Log.e(TAG, "Failed to parse M-PESA message: $fullBody")
                    return@launch
                }

                Log.d(TAG, "Parsed: ${transaction.transactionId}, KSH${transaction.amount}")

                val db = AppDatabase.getInstance(context)

                // Avoid duplicates
                val existing = db.transactionDao().findByTransactionId(transaction.transactionId)
                if (existing != null) {
                    Log.d(TAG, "Duplicate, skipping: ${transaction.transactionId}")
                    return@launch
                }

                val insertedId = db.transactionDao().insert(transaction)
                val saved = transaction.copy(id = insertedId)
                Log.d(TAG, "Saved transaction id: $insertedId")

                NotificationHelper.showTransactionNotification(context, saved)

            } catch (e: Exception) {
                Log.e(TAG, "Error processing M-PESA SMS", e)
            }
        }
    }
}

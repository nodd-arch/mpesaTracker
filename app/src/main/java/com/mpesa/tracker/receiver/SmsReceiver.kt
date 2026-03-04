package com.mpesa.tracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import com.mpesa.tracker.data.db.AppDatabase
import com.mpesa.tracker.utils.MpesaParser
import com.mpesa.tracker.utils.NotificationHelper
import com.mpesa.tracker.utils.PreferencesManager
import com.mpesa.tracker.utils.dataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val simSlot = intent.getIntExtra("slot", -1)
            .let { if (it == -1) intent.getIntExtra("android.telephony.extra.SLOT_INDEX", 0) else it }

        // Combine multi-part messages
        val fullBody = messages.joinToString("") { it.messageBody ?: "" }
        val sender = messages.firstOrNull()?.originatingAddress ?: return

        // Only process M-PESA messages
        if (!MpesaParser.isMpesaSender(sender)) return

        CoroutineScope(Dispatchers.IO).launch {
            // Check preferences to see if we should monitor this SIM
            val prefs = context.dataStore.data.first()
            val monitorSim1 = prefs[booleanPreferencesKey("monitor_sim1")] ?: true
            val monitorSim2 = prefs[booleanPreferencesKey("monitor_sim2")] ?: false

            val shouldProcess = when (simSlot) {
                0 -> monitorSim1
                1 -> monitorSim2
                else -> monitorSim1 || monitorSim2 // Unknown slot, process if any enabled
            }

            if (!shouldProcess) return@launch

            val transaction = MpesaParser.parse(fullBody, simSlot) ?: return@launch

            val db = AppDatabase.getInstance(context)

            // Avoid duplicates
            val existing = db.transactionDao().findByTransactionId(transaction.transactionId)
            if (existing != null) return@launch

            val insertedId = db.transactionDao().insert(transaction)
            val saved = transaction.copy(id = insertedId)

            NotificationHelper.showTransactionNotification(context, saved)
        }
    }
}

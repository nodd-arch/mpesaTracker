package com.mpesa.tracker.utils

import com.mpesa.tracker.data.model.MpesaTransaction

object MpesaParser {

    // M-PESA sender shortcode (Safaricom Kenya)
    const val MPESA_SENDER = "MPESA"
    const val MPESA_SENDER_ALT = "M-PESA"

    /**
     * Parses an M-PESA received-money SMS into a structured transaction.
     * Expected format:
     * "UC3BL8IIRM Confirmed.on 3/3/26 at 9:39 PMKSH25.00 received from 254114842082 TEDDY MAINA MACHARIA.
     *  New Account balance is KSH1,190.00. Transaction cost, KSH0.00."
     */
    fun parse(raw: String, simSlot: Int): MpesaTransaction? {
        return try {
            // Transaction ID - first word before space
            val transactionId = raw.trim().split(" ").firstOrNull() ?: return null

            // Must contain "received from" to be an incoming payment
            if (!raw.contains("received from", ignoreCase = true)) return null

            // Date: pattern like "on 3/3/26" or "on 03/03/2026"
            val dateRegex = Regex("""on\s+(\d{1,2}/\d{1,2}/\d{2,4})""", RegexOption.IGNORE_CASE)
            val date = dateRegex.find(raw)?.groupValues?.get(1) ?: return null

            // Time: handles "9:39 PM", "9:39PM", "9:39 PMKSH" all correctly
            val timeRegex = Regex("""at\s+(\d{1,2}:\d{2})\s*([AP]M)""", RegexOption.IGNORE_CASE)
            val timeMatch = timeRegex.find(raw)
            val time = if (timeMatch != null) {
                "${timeMatch.groupValues[1]} ${timeMatch.groupValues[2].uppercase()}"
            } else return null

            // Amount: KSH value immediately before "received" - handles "KSH25.00received" or "KSH25.00 received"
            val amountRegex = Regex("""KSH\s*([\d,]+\.?\d*)\s*received""", RegexOption.IGNORE_CASE)
            val amountStr = amountRegex.find(raw)?.groupValues?.get(1)?.replace(",", "") ?: return null
            val amount = amountStr.toDoubleOrNull() ?: return null

            // Sender phone: digits after "received from "
            val phoneRegex = Regex("""received from\s+(\d+)\s+""", RegexOption.IGNORE_CASE)
            val senderPhone = phoneRegex.find(raw)?.groupValues?.get(1) ?: return null

            // Sender name: text after phone number, up to the next period
            val nameRegex = Regex("""received from\s+\d+\s+([A-Z][A-Z\s]+?)[\.\n]""", RegexOption.IGNORE_CASE)
            val senderName = nameRegex.find(raw)?.groupValues?.get(1)?.trim() ?: return null

            // New balance after "balance is KSH"
            val balanceRegex = Regex("""balance is KSH\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
            val balanceStr = balanceRegex.find(raw)?.groupValues?.get(1)?.replace(",", "") ?: "0"
            val newBalance = balanceStr.toDoubleOrNull() ?: 0.0

            // Transaction cost
            val costRegex = Regex("""cost,\s*KSH\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
            val costStr = costRegex.find(raw)?.groupValues?.get(1)?.replace(",", "") ?: "0"
            val transactionCost = costStr.toDoubleOrNull() ?: 0.0

            MpesaTransaction(
                transactionId = transactionId,
                date = date,
                time = time,
                amount = amount,
                senderPhone = senderPhone,
                senderName = senderName,
                newBalance = newBalance,
                transactionCost = transactionCost,
                rawMessage = raw,
                timestamp = System.currentTimeMillis(),
                simSlot = simSlot
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isMpesaSender(sender: String): Boolean {
        return sender.equals(MPESA_SENDER, ignoreCase = true) ||
               sender.equals(MPESA_SENDER_ALT, ignoreCase = true) ||
               sender.contains("MPESA", ignoreCase = true)
    }

    fun formatAmount(amount: Double): String {
        return "KSH %,.2f".format(amount)
    }
}

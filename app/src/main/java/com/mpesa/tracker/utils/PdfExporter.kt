package com.mpesa.tracker.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import com.mpesa.tracker.data.model.MpesaTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    fun exportTransactions(
        context: Context,
        transactions: List<MpesaTransaction>,
        fileName: String = "mpesa_transactions_${System.currentTimeMillis()}.pdf"
    ): File? {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir
            val file = File(dir, fileName)

            val document = Document(PageSize.A4, 40f, 40f, 60f, 40f)
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()

            // Fonts
            val titleFont = Font(Font.FontFamily.HELVETICA, 20f, Font.BOLD, BaseColor(0, 150, 57))
            val headerFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.WHITE)
            val cellFont = Font(Font.FontFamily.HELVETICA, 9f, Font.NORMAL, BaseColor.DARK_GRAY)
            val subFont = Font(Font.FontFamily.HELVETICA, 11f, Font.NORMAL, BaseColor.GRAY)
            val totalFont = Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD, BaseColor(0, 150, 57))

            // Header
            val title = Paragraph("M-PESA Transaction Report", titleFont)
            title.alignment = Element.ALIGN_CENTER
            document.add(title)

            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val generated = Paragraph("Generated: ${sdf.format(Date())}", subFont)
            generated.alignment = Element.ALIGN_CENTER
            generated.spacingAfter = 20f
            document.add(generated)

            // Summary
            val total = transactions.sumOf { it.amount }
            val summaryFont = Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD)
            document.add(Paragraph("Total Transactions: ${transactions.size}  |  Total Received: ${MpesaParser.formatAmount(total)}", summaryFont).apply {
                spacingAfter = 16f
            })

            // Table
            val table = PdfPTable(6)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(1.5f, 1.2f, 1.2f, 2f, 2f, 1.5f))

            val headers = listOf("TX ID", "Date", "Time", "From", "Amount", "Balance")
            val headerBg = BaseColor(0, 150, 57)
            headers.forEach { h ->
                val cell = PdfPCell(Phrase(h, headerFont)).apply {
                    backgroundColor = headerBg
                    horizontalAlignment = Element.ALIGN_CENTER
                    paddingTop = 8f
                    paddingBottom = 8f
                    border = Rectangle.NO_BORDER
                }
                table.addCell(cell)
            }

            transactions.forEachIndexed { idx, tx ->
                val bg = if (idx % 2 == 0) BaseColor(240, 255, 244) else BaseColor.WHITE
                val row = listOf(
                    tx.transactionId,
                    tx.date,
                    tx.time,
                    tx.senderName.split(" ").take(2).joinToString(" "),
                    MpesaParser.formatAmount(tx.amount),
                    MpesaParser.formatAmount(tx.newBalance)
                )
                row.forEach { value ->
                    val cell = PdfPCell(Phrase(value, cellFont)).apply {
                        backgroundColor = bg
                        paddingTop = 6f
                        paddingBottom = 6f
                        paddingLeft = 4f
                        border = Rectangle.BOTTOM
                        borderColor = BaseColor(220, 220, 220)
                    }
                    table.addCell(cell)
                }
            }

            document.add(table)

            // Total row
            document.add(Paragraph("\nTotal Received: ${MpesaParser.formatAmount(total)}", totalFont).apply {
                spacingBefore = 12f
                alignment = Element.ALIGN_RIGHT
            })

            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    }
}

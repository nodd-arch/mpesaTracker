package com.mpesa.tracker.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpesa.tracker.data.model.MpesaTransaction
import com.mpesa.tracker.ui.theme.*
import com.mpesa.tracker.utils.MpesaParser
import com.mpesa.tracker.utils.PdfExporter
import com.mpesa.tracker.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordsScreen(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val distinctDates by viewModel.distinctDates.collectAsState()
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(MpesaBackground)) {
        if (selectedDate != null) {
            // Day detail view
            DayDetailView(
                date = selectedDate!!,
                transactions = viewModel.getTransactionsForDate(selectedDate!!),
                onBack = { selectedDate = null }
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Records", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = MpesaGreen)
                    }
                }

                if (distinctDates.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No records yet", color = TextSecondary)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(distinctDates) { date ->
                            val dayTx = viewModel.getTransactionsForDate(date)
                            DayCard(
                                date = date,
                                transactions = dayTx,
                                onClick = { selectedDate = date }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }

        if (showExportDialog) {
            ExportDialog(
                onDismiss = { showExportDialog = false },
                onExportAll = {
                    showExportDialog = false
                    scope.launch {
                        val all = viewModel.getAllForExport()
                        val file = PdfExporter.exportTransactions(context, all)
                        file?.let { PdfExporter.sharePdf(context, it) }
                    }
                },
                onExportRange = { start, end ->
                    showExportDialog = false
                    scope.launch {
                        val ranged = viewModel.getRangeForExport(start, end)
                        val file = PdfExporter.exportTransactions(context, ranged)
                        file?.let { PdfExporter.sharePdf(context, it) }
                    }
                },
                context = context
            )
        }
    }
}

@Composable
fun DayCard(date: String, transactions: List<MpesaTransaction>, onClick: () -> Unit) {
    val total = transactions.sumOf { it.amount }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MpesaGreen.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📅", fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(formatDate(date), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                Text("${transactions.size} transaction${if (transactions.size != 1) "s" else ""}", fontSize = 12.sp, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("+${MpesaParser.formatAmount(total)}", color = MpesaGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary.copy(0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun DayDetailView(date: String, transactions: List<MpesaTransaction>, onBack: () -> Unit) {
    var selectedTx by remember { mutableStateOf<MpesaTransaction?>(null) }
    val total = transactions.sumOf { it.amount }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(formatDate(date), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                Text("${transactions.size} transactions • ${MpesaParser.formatAmount(total)}", fontSize = 13.sp, color = TextSecondary)
            }
        }
        Divider(color = DividerColor)

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(transactions) { tx ->
                TransactionCard(tx, onClick = { selectedTx = tx })
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    selectedTx?.let { tx ->
        TransactionDetailSheet(tx, onDismiss = { selectedTx = null })
    }
}

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExportAll: () -> Unit,
    onExportRange: (Long, Long) -> Unit,
    context: Context
) {
    var mode by remember { mutableStateOf(0) } // 0=choose, 1=range
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Transactions", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (mode == 0) {
                    Text("Choose export type:", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onExportAll,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MpesaGreen)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export All")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { mode = 1 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select Date Range")
                    }
                } else {
                    Text("Select date range:", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    DatePickerButton("Start Date", startDate) { startDate = it }
                    Spacer(Modifier.height(8.dp))
                    DatePickerButton("End Date", endDate) { endDate = it }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val s = startDate ?: return@Button
                            val e = (endDate ?: System.currentTimeMillis()).let { it + 86399999 }
                            onExportRange(s, e)
                        },
                        enabled = startDate != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MpesaGreen)
                    ) {
                        Text("Export PDF")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DatePickerButton(label: String, date: Long?, onDateSelected: (Long) -> Unit) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    OutlinedButton(
        onClick = {
            val cal = Calendar.getInstance()
            DatePickerDialog(context, { _, year, month, day ->
                val c = Calendar.getInstance()
                c.set(year, month, day, 0, 0, 0)
                onDateSelected(c.timeInMillis)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.DateRange, contentDescription = null, tint = MpesaGreen)
        Spacer(Modifier.width(8.dp))
        Text(if (date != null) sdf.format(Date(date)) else label)
    }
}

fun formatDate(rawDate: String): String {
    return try {
        val parts = rawDate.split("/")
        if (parts.size == 3) {
            val month = parts[0].toInt()
            val day = parts[1].toInt()
            val year = parts[2].toIntOrNull()?.let { if (it < 100) 2000 + it else it } ?: return rawDate
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day)
            val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            sdf.format(cal.time)
        } else rawDate
    } catch (e: Exception) {
        rawDate
    }
}

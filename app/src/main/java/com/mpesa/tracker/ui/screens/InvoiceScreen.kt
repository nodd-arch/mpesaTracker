package com.mpesa.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpesa.tracker.data.model.Invoice
import com.mpesa.tracker.ui.theme.*
import com.mpesa.tracker.utils.MpesaParser
import com.mpesa.tracker.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InvoiceScreen(viewModel: MainViewModel) {
    val invoices by viewModel.invoices.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var selectedInvoice by remember { mutableStateOf<Invoice?>(null) }

    Box(Modifier.fillMaxSize().background(MpesaBackground)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Invoices", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
                FloatingActionButton(
                    onClick = { showCreate = true },
                    modifier = Modifier.size(44.dp),
                    containerColor = MpesaGreen,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Invoice", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            if (invoices.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧾", fontSize = 56.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("No Invoices Yet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap + to create your first invoice", color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Stats
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InvoiceStatCard(
                                modifier = Modifier.weight(1f),
                                label = "Pending",
                                value = invoices.count { !it.isPaid }.toString(),
                                color = Color(0xFFFF9800)
                            )
                            InvoiceStatCard(
                                modifier = Modifier.weight(1f),
                                label = "Paid",
                                value = invoices.count { it.isPaid }.toString(),
                                color = MpesaGreen
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    items(invoices) { invoice ->
                        InvoiceCard(invoice, onClick = { selectedInvoice = invoice })
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }

        if (showCreate) {
            CreateInvoiceDialog(
                onDismiss = { showCreate = false },
                onCreate = { inv ->
                    viewModel.createInvoice(inv)
                    showCreate = false
                }
            )
        }

        selectedInvoice?.let { inv ->
            InvoiceDetailDialog(
                invoice = inv,
                onDismiss = { selectedInvoice = null },
                onMarkPaid = {
                    viewModel.updateInvoice(inv.copy(isPaid = true))
                    selectedInvoice = null
                },
                onDelete = {
                    viewModel.deleteInvoice(inv)
                    selectedInvoice = null
                }
            )
        }
    }
}

@Composable
fun InvoiceStatCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(0.1f))) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@Composable
fun InvoiceCard(invoice: Invoice, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (invoice.isPaid) MpesaGreen.copy(0.1f) else Color(0xFFFF9800).copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (invoice.isPaid) Icons.Default.CheckCircle else Icons.Default.PendingActions,
                    contentDescription = null,
                    tint = if (invoice.isPaid) MpesaGreen else Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(invoice.clientName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                Text("#${invoice.invoiceNumber}", fontSize = 12.sp, color = TextSecondary)
                Text(sdf.format(Date(invoice.dateCreated)), fontSize = 11.sp, color = TextSecondary.copy(0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(MpesaParser.formatAmount(invoice.amount), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Text(
                    if (invoice.isPaid) "PAID" else "PENDING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (invoice.isPaid) MpesaGreen else Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun CreateInvoiceDialog(onDismiss: () -> Unit, onCreate: (Invoice) -> Unit) {
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val invoiceNum = remember { "INV-${System.currentTimeMillis().toString().takeLast(6)}" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Invoice", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Invoice #$invoiceNum", fontSize = 12.sp, color = TextSecondary)
                OutlinedTextField(value = clientName, onValueChange = { clientName = it },
                    label = { Text("Client Name") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = clientPhone, onValueChange = { clientPhone = it },
                    label = { Text("Client Phone") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Description / For") }, modifier = Modifier.fillMaxWidth(),
                    minLines = 2, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it },
                    label = { Text("Amount (KSH)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    onCreate(Invoice(
                        invoiceNumber = invoiceNum,
                        clientName = clientName,
                        clientPhone = clientPhone,
                        description = description,
                        amount = amt
                    ))
                },
                enabled = clientName.isNotBlank() && amount.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MpesaGreen)
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun InvoiceDetailDialog(invoice: Invoice, onDismiss: () -> Unit, onMarkPaid: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invoice #${invoice.invoiceNumber}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Client", invoice.clientName)
                DetailRow("Phone", invoice.clientPhone)
                DetailRow("Description", invoice.description)
                DetailRow("Amount", MpesaParser.formatAmount(invoice.amount))
                DetailRow("Date", sdf.format(Date(invoice.dateCreated)))
                DetailRow("Status", if (invoice.isPaid) "✅ Paid" else "⏳ Pending")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!invoice.isPaid) {
                    Button(onClick = onMarkPaid, colors = ButtonDefaults.buttonColors(containerColor = MpesaGreen)) {
                        Text("Mark Paid")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

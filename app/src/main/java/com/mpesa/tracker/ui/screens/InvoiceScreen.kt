package com.mpesa.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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
    var activeTab by remember { mutableStateOf(0) } // 0 = Pending, 1 = Paid

    val pending = invoices.filter { !it.isPaid }
    val paid = invoices.filter { it.isPaid }
    val displayed = if (activeTab == 0) pending else paid

    Box(Modifier.fillMaxSize().background(Color(0xFFF0F0F0))) {
        Column(Modifier.fillMaxSize()) {

            // ── Header ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Invoices", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextPrimary)
                    Text("${invoices.size} total", fontSize = 12.sp, color = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MpesaGreen)
                        .border(1.5.dp, MpesaGreenDark, RoundedCornerShape(6.dp))
                        .clickable { showCreate = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New Invoice", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Divider(color = Color(0xFF1A1A1A), thickness = 1.5.dp)

            // ── Pending / Paid Tabs ──
            Row(Modifier.fillMaxWidth().background(Color.White)) {
                listOf("PENDING" to pending.size, "PAID" to paid.size).forEachIndexed { index, (label, count) ->
                    val selected = activeTab == index
                    val accentColor = if (index == 0) Color(0xFFFF9800) else MpesaGreen
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = index }
                            .background(if (selected) Color.White else Color(0xFFF5F5F5))
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = if (selected) accentColor else Color(0xFFCCCCCC))
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = if (selected) TextSecondary else Color(0xFFCCCCCC), letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.height(2.dp).fillMaxWidth(0.4f)
                            .background(if (selected) accentColor else Color.Transparent))
                    }
                    if (index == 0) {
                        Box(Modifier.width(1.dp).height(60.dp).background(Color(0xFFE8E8E8)).align(Alignment.CenterVertically))
                    }
                }
            }

            Divider(color = Color(0xFFDDDDDD), thickness = 1.dp)

            // ── List ──
            if (displayed.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (activeTab == 0) "🧾" else "✅", fontSize = 48.sp)
                        Spacer(Modifier.height(14.dp))
                        Text(if (activeTab == 0) "No Pending Invoices" else "No Paid Invoices",
                            fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(if (activeTab == 0) "Tap New Invoice to create one" else "Paid invoices will appear here",
                            color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)) {
                    items(displayed, key = { it.id }) { invoice ->
                        InvoiceCard(invoice, onClick = { selectedInvoice = invoice })
                    }
                }
            }
        }

        if (showCreate) {
            CreateInvoiceSheet(onDismiss = { showCreate = false }, onCreate = { inv ->
                viewModel.createInvoice(inv)
                showCreate = false
            })
        }

        selectedInvoice?.let { inv ->
            InvoiceDetailSheet(
                invoice = inv,
                onDismiss = { selectedInvoice = null },
                onMarkPaid = { viewModel.updateInvoice(inv.copy(isPaid = true)); selectedInvoice = null },
                onDelete = { viewModel.deleteInvoice(inv); selectedInvoice = null }
            )
        }
    }
}

@Composable
fun InvoiceCard(invoice: Invoice, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val isPaid = invoice.isPaid

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .shadow(elevation = if (isPaid) 1.dp else 3.dp, shape = RoundedCornerShape(14.dp),
                spotColor = Color.Black.copy(if (isPaid) 0.04f else 0.08f))
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPaid) Color(0xFFF8F8F8) else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
            Text(invoice.clientName, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = if (isPaid) Color(0xFF999999) else TextPrimary)
            Text("#${invoice.invoiceNumber}  •  ${sdf.format(Date(invoice.dateCreated))}",
                fontSize = 12.sp, color = TextSecondary)
            if (invoice.description.isNotBlank()) {
                Text(invoice.description, fontSize = 12.sp, color = TextSecondary.copy(0.6f), maxLines = 1)
            }
        }

        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(MpesaParser.formatAmount(invoice.amount), fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = if (isPaid) Color(0xFF999999) else TextPrimary)
            Box(
                Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isPaid) Color(0xFFEEEEEE) else MpesaGreen.copy(0.1f))
                    .border(1.dp, if (isPaid) Color(0xFFCCCCCC) else MpesaGreen.copy(0.4f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(if (isPaid) "PAID" else "PENDING", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = if (isPaid) Color(0xFF999999) else MpesaGreen, letterSpacing = 0.6.sp)
            }
        }
    }
}

@Composable
fun CreateInvoiceSheet(onDismiss: () -> Unit, onCreate: (Invoice) -> Unit) {
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val invoiceNum = remember { "INV-${System.currentTimeMillis().toString().takeLast(6)}" }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Sheet takes most of screen so buttons are always visible above nav
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .clickable(enabled = false, onClick = {})
        ) {
            // Handle
            Box(
                Modifier.padding(top = 14.dp).width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Color(0xFFDDDDDD))
                    .align(Alignment.CenterHorizontally)
            )

            // Title
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("New Invoice", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
                    Text("#$invoiceNum", fontSize = 12.sp, color = TextSecondary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Divider(color = Color(0xFF1A1A1A), thickness = 2.dp)

            // Scrollable fields — takes remaining space above buttons
            Column(Modifier.weight(1f).verticalScroll(scrollState)) {
                BrutalistField(value = clientName, onValueChange = { clientName = it },
                    label = "CLIENT NAME", placeholder = "e.g. John Doe")
                Divider(color = Color(0xFFEEEEEE))
                BrutalistField(value = clientPhone, onValueChange = { clientPhone = it },
                    label = "PHONE NUMBER", placeholder = "e.g. 0712345678",
                    keyboardType = KeyboardType.Phone)
                Divider(color = Color(0xFFEEEEEE))
                BrutalistField(value = description, onValueChange = { description = it },
                    label = "FOR / DESCRIPTION", placeholder = "What is this invoice for?",
                    minLines = 2)
                Divider(color = Color(0xFFEEEEEE))
                BrutalistField(value = amount, onValueChange = { amount = it },
                    label = "AMOUNT (KSH)", placeholder = "0.00",
                    keyboardType = KeyboardType.Decimal)
            }

            // Buttons always pinned at bottom of sheet — never covered by nav
            Divider(color = Color(0xFF1A1A1A), thickness = 2.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    // 90dp accounts for the floating nav bar height + its 24dp margin
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: return@Button
                        onCreate(Invoice(
                            invoiceNumber = invoiceNum,
                            clientName = clientName.trim(),
                            clientPhone = clientPhone.trim(),
                            description = description.trim(),
                            amount = amt
                        ))
                    },
                    enabled = clientName.isNotBlank() && amount.isNotBlank(),
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MpesaGreen)
                ) {
                    Text("Create Invoice", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun BrutalistField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MpesaGreen, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, fontSize = 17.sp, color = Color(0xFFCCCCCC))
                    inner()
                }
            }
        )
    }
}

@Composable
fun InvoiceDetailSheet(invoice: Invoice, onDismiss: () -> Unit, onMarkPaid: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .clickable(enabled = false, onClick = {})
        ) {
            Box(
                Modifier.padding(top = 14.dp).width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Color(0xFFDDDDDD))
                    .align(Alignment.CenterHorizontally)
            )

            // Header
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(invoice.clientName, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                    Text("#${invoice.invoiceNumber}", fontSize = 12.sp, color = TextSecondary)
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (invoice.isPaid) Color(0xFFEEEEEE) else MpesaGreen.copy(0.1f))
                        .border(1.5.dp, if (invoice.isPaid) Color(0xFFCCCCCC) else MpesaGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (invoice.isPaid) "PAID" else "PENDING", fontWeight = FontWeight.Bold,
                        fontSize = 11.sp, color = if (invoice.isPaid) Color(0xFF999999) else MpesaGreen,
                        letterSpacing = 1.sp)
                }
            }

            Divider(color = Color(0xFF1A1A1A), thickness = 2.dp)

            // Amount strip
            Row(
                Modifier.fillMaxWidth().background(Color(0xFFF8FFF8)).padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                    Text(MpesaParser.formatAmount(invoice.amount), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                    Text(sdf.format(Date(invoice.dateCreated)), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }

            Divider(color = Color(0xFFE0E0E0))

            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (invoice.clientPhone.isNotBlank()) InvoiceDetailItem("Phone", invoice.clientPhone)
                if (invoice.description.isNotBlank()) InvoiceDetailItem("For", invoice.description)
            }

            Divider(color = Color(0xFF1A1A1A), thickness = 2.dp)

            // Action buttons — padded 100dp at bottom to clear floating nav bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                if (!invoice.isPaid) {
                    Button(
                        onClick = onMarkPaid,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpesaGreen)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mark as Paid", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE))
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Invoice?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete the invoice for ${invoice.clientName}.", color = TextSecondary) },
            confirmButton = {
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(6.dp)) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun InvoiceDetailItem(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

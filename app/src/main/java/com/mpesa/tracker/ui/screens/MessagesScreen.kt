package com.mpesa.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpesa.tracker.data.model.MpesaTransaction
import com.mpesa.tracker.ui.theme.*
import com.mpesa.tracker.utils.MpesaParser
import com.mpesa.tracker.viewmodel.MainViewModel

@Composable
fun MessagesScreen(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    var selectedTx by remember { mutableStateOf<MpesaTransaction?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(MpesaBackground)) {
        if (transactions.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Summary header
                    SummaryCard(transactions)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Recent Transactions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(transactions) { tx ->
                    TransactionCard(tx, onClick = { selectedTx = tx })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // Detail bottom sheet
        selectedTx?.let { tx ->
            TransactionDetailSheet(tx, onDismiss = { selectedTx = null })
        }
    }
}

@Composable
fun SummaryCard(transactions: List<MpesaTransaction>) {
    val total = transactions.sumOf { it.amount }
    val today = transactions.filter {
        val cal = java.util.Calendar.getInstance()
        val todayStr = "${cal.get(java.util.Calendar.MONTH)+1}/${cal.get(java.util.Calendar.DAY_OF_MONTH)}/${cal.get(java.util.Calendar.YEAR).toString().takeLast(2)}"
        it.date == todayStr
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MpesaGreen),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Total Received", color = Color.White.copy(0.8f), fontSize = 12.sp)
                Text(MpesaParser.formatAmount(total), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${transactions.size} transactions", color = Color.White.copy(0.7f), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Today", color = Color.White.copy(0.8f), fontSize = 12.sp)
                Text(MpesaParser.formatAmount(today.sumOf { it.amount }), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${today.size} transactions", color = Color.White.copy(0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TransactionCard(tx: MpesaTransaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MpesaGreen.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tx.senderName.split(" ").take(2).map { it.firstOrNull() ?: ' ' }.joinToString(""),
                    color = MpesaGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tx.senderName.split(" ").take(2).joinToString(" "),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    "${tx.date} • ${tx.time}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    "ID: ${tx.transactionId}",
                    fontSize = 11.sp,
                    color = TextSecondary.copy(0.7f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "+${MpesaParser.formatAmount(tx.amount)}",
                    color = MpesaGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (tx.simSlot >= 0) {
                    Text(
                        "SIM ${tx.simSlot + 1}",
                        fontSize = 10.sp,
                        color = TextSecondary.copy(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionDetailSheet(tx: MpesaTransaction, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var showRaw by remember { mutableStateOf(false) }
    var snackbarVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp)) {
                // Handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.LightGray)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(20.dp))

                // Amount hero
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MpesaGreen.copy(0.08f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Received", color = TextSecondary, fontSize = 13.sp)
                        Text(
                            MpesaParser.formatAmount(tx.amount),
                            color = MpesaGreen,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Details
                DetailRow("From", tx.senderName)
                DetailRow("Phone", tx.senderPhone)
                DetailRow("Date", "${tx.date}  •  ${tx.time}")
                // Transaction ID with copy
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transaction ID", color = TextSecondary, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tx.transactionId, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(tx.transactionId))
                                snackbarVisible = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MpesaGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                DetailRow("New Balance", MpesaParser.formatAmount(tx.newBalance))
                DetailRow("Transaction Cost", MpesaParser.formatAmount(tx.transactionCost))
                if (tx.simSlot >= 0) DetailRow("SIM", "SIM ${tx.simSlot + 1}")

                // Raw message dropdown
                Spacer(Modifier.height(12.dp))
                Divider(color = DividerColor)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showRaw = !showRaw }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Original Message", fontWeight = FontWeight.Medium, color = TextSecondary)
                    Icon(
                        if (showRaw) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
                AnimatedVisibility(showRaw, enter = expandVertically(), exit = shrinkVertically()) {
                    Text(
                        tx.rawMessage,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(12.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (snackbarVisible) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        snackbarVisible = false
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MpesaGreen)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Transaction ID copied!", color = Color.White, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextPrimary)
    }
    Divider(color = DividerColor, thickness = 0.5.dp)
}

@Composable
fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("No M-PESA Messages Yet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Messages from MPESA will\nappear here automatically.",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

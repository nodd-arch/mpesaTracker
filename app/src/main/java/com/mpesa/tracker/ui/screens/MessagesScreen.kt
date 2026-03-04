package com.mpesa.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpesa.tracker.data.model.MpesaTransaction
import com.mpesa.tracker.ui.theme.*
import com.mpesa.tracker.utils.MpesaParser
import com.mpesa.tracker.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun MessagesScreen(viewModel: MainViewModel) {
    val unconfirmed by viewModel.unconfirmedTransactions.collectAsState()
    val confirmed by viewModel.confirmedTransactions.collectAsState()
    var selectedTx by remember { mutableStateOf<MpesaTransaction?>(null) }
    var showReverseDialog by remember { mutableStateOf<MpesaTransaction?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(MpesaBackground)) {
        if (unconfirmed.isEmpty() && confirmed.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Pending / unconfirmed section ──
                if (unconfirmed.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recent Transactions",
                            subtitle = "${unconfirmed.size} awaiting confirmation",
                            color = TextPrimary
                        )
                    }
                    items(unconfirmed, key = { it.id }) { tx ->
                        UnconfirmedTransactionCard(
                            tx = tx,
                            onTap = { selectedTx = tx },
                            onConfirm = { viewModel.confirmTransaction(tx.id) }
                        )
                    }
                }

                // ── Confirmed section ──
                if (confirmed.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Divider(color = Color(0xFFDDDDDD), thickness = 1.dp)
                        Spacer(Modifier.height(4.dp))
                        SectionHeader(
                            title = "Confirmed Purchases",
                            subtitle = "${confirmed.size} handed over  •  Long press to reverse",
                            color = Color(0xFF999999)
                        )
                    }
                    items(confirmed, key = { it.id }) { tx ->
                        ConfirmedTransactionCard(
                            tx = tx,
                            onTap = { selectedTx = tx },
                            onLongPress = { showReverseDialog = tx }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // Transaction detail sheet
        selectedTx?.let { tx ->
            TransactionDetailSheet(tx, onDismiss = { selectedTx = null })
        }

        // Reverse confirmation dialog
        showReverseDialog?.let { tx ->
            ReverseConfirmDialog(
                tx = tx,
                onConfirm = {
                    viewModel.unconfirmTransaction(tx.id)
                    showReverseDialog = null
                },
                onDismiss = { showReverseDialog = null }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String, color: Color) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = color)
        Text(subtitle, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
fun UnconfirmedTransactionCard(
    tx: MpesaTransaction,
    onTap: () -> Unit,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Main content row — tap for detail
            Row(
                Modifier
                    .clickable(onClick = onTap)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MpesaGreen.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tx.senderName.split(" ").take(2).map { it.firstOrNull() ?: ' ' }.joinToString(""),
                        color = MpesaGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        tx.senderName.split(" ").take(2).joinToString(" "),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Text(
                        "${tx.date} • ${tx.time}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "+${MpesaParser.formatAmount(tx.amount)}",
                        color = MpesaGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (tx.simSlot >= 0) {
                        Text("SIM ${tx.simSlot + 1}", fontSize = 10.sp, color = TextSecondary.copy(0.6f))
                    }
                }
            }

            // Confirm button strip
            Divider(color = DividerColor, thickness = 0.8.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onConfirm)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Confirm",
                    tint = MpesaGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Confirm Purchase",
                    color = MpesaGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConfirmedTransactionCard(
    tx: MpesaTransaction,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F2)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD0D0D0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tx.senderName.split(" ").take(2).map { it.firstOrNull() ?: ' ' }.joinToString(""),
                    color = Color(0xFF888888),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tx.senderName.split(" ").take(2).joinToString(" "),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF999999)
                )
                Text(
                    "${tx.date} • ${tx.time}",
                    fontSize = 12.sp,
                    color = Color(0xFFBBBBBB)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "+${MpesaParser.formatAmount(tx.amount)}",
                    color = Color(0xFF999999),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "Done",
                        fontSize = 10.sp,
                        color = Color(0xFFAAAAAA),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ReverseConfirmDialog(
    tx: MpesaTransaction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reverse Confirmation?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Move ${tx.senderName.split(" ").take(2).joinToString(" ")}'s payment of " +
                "${MpesaParser.formatAmount(tx.amount)} back to pending?\n\nUse this if the purchase was not actually handed over.",
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) { Text("Yes, Reverse") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
            modifier = Modifier.fillMaxWidth().clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp)) {
                Box(
                    modifier = Modifier
                        .width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.LightGray)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(20.dp))

                // Status badge
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (tx.isConfirmed) MpesaGreen.copy(0.12f) else Color(0xFFFFF3E0))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (tx.isConfirmed) "✓ Confirmed & Handed Over" else "⏳ Awaiting Confirmation",
                        color = if (tx.isConfirmed) MpesaGreen else Color(0xFFE65100),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MpesaGreen.copy(0.08f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Received", color = TextSecondary, fontSize = 13.sp)
                        Text(MpesaParser.formatAmount(tx.amount), color = MpesaGreen, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))
                DetailRow("From", tx.senderName)
                DetailRow("Phone", tx.senderPhone)
                DetailRow("Date", "${tx.date}  •  ${tx.time}")

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
                Divider(color = DividerColor, thickness = 0.5.dp)

                DetailRow("New Balance", MpesaParser.formatAmount(tx.newBalance))
                DetailRow("Transaction Cost", MpesaParser.formatAmount(tx.transactionCost))
                if (tx.simSlot >= 0) DetailRow("SIM", "SIM ${tx.simSlot + 1}")

                Spacer(Modifier.height(12.dp))
                Divider(color = DividerColor)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { showRaw = !showRaw }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Original Message", fontWeight = FontWeight.Medium, color = TextSecondary)
                    Icon(
                        if (showRaw) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null, tint = TextSecondary
                    )
                }
                AnimatedVisibility(showRaw, enter = expandVertically(), exit = shrinkVertically()) {
                    Text(
                        tx.rawMessage,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(12.dp)
                    )
                }

                if (snackbarVisible) {
                    LaunchedEffect(Unit) {
                        delay(2000)
                        snackbarVisible = false
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MpesaGreen).padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Transaction ID copied!", color = Color.White, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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
                color = TextSecondary, fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

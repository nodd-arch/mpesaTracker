package com.mpesa.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F0F0))) {
        if (unconfirmed.isEmpty() && confirmed.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── PENDING SECTION ──
                if (unconfirmed.isNotEmpty()) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(3.dp).height(18.dp).background(MpesaGreen))
                            Spacer(Modifier.width(10.dp))
                            Text("RECENT TRANSACTIONS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary, letterSpacing = 1.2.sp)
                            Spacer(Modifier.weight(1f))
                            Text("${unconfirmed.size} pending", fontSize = 11.sp, color = MpesaGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    items(unconfirmed, key = { it.id }) { tx ->
                        UnconfirmedTransactionCard(
                            tx = tx,
                            onTap = { selectedTx = tx },
                            onConfirm = { viewModel.confirmTransaction(tx.id) }
                        )
                    }
                }

                // ── CONFIRMED SECTION ──
                if (confirmed.isNotEmpty()) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(3.dp).height(18.dp).background(Color(0xFFAAAAAA)))
                            Spacer(Modifier.width(10.dp))
                            Text("CONFIRMED PURCHASES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF888888), letterSpacing = 1.2.sp)
                            Spacer(Modifier.weight(1f))
                            Text("long press to reverse", fontSize = 10.sp, color = Color(0xFFBBBBBB))
                        }
                    }
                    items(confirmed, key = { it.id }) { tx ->
                        ConfirmedTransactionCard(
                            tx = tx,
                            onTap = { selectedTx = tx },
                            onLongPress = { showReverseDialog = tx }
                        )
                    }
                }
            }
        }

        selectedTx?.let { tx ->
            TransactionDetailSheet(tx, onDismiss = { selectedTx = null })
        }

        showReverseDialog?.let { tx ->
            ReverseConfirmDialog(
                tx = tx,
                onConfirm = { viewModel.unconfirmTransaction(tx.id); showReverseDialog = null },
                onDismiss = { showReverseDialog = null }
            )
        }
    }
}

// ── PENDING card — elevated white card, green accent ──
@Composable
fun UnconfirmedTransactionCard(tx: MpesaTransaction, onTap: () -> Unit, onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp), spotColor = Color.Black.copy(0.08f))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MpesaGreen.copy(0.1f))
                    .border(1.dp, MpesaGreen.copy(0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tx.senderName.split(" ").take(2).map { it.firstOrNull() ?: ' ' }.joinToString(""),
                    color = MpesaGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f).padding(vertical = 16.dp)) {
                Text(tx.senderName.split(" ").take(2).joinToString(" "), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text("${tx.date}  •  ${tx.time}", fontSize = 12.sp, color = TextSecondary)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("+${MpesaParser.formatAmount(tx.amount)}", color = MpesaGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (tx.simSlot >= 0) Text("SIM ${tx.simSlot + 1}", fontSize = 10.sp, color = TextSecondary.copy(0.5f))
            }
        }

        Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)

        // Confirm strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onConfirm)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MpesaGreen, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text("CONFIRM PURCHASE", color = MpesaGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.8.sp)
        }
    }
}

// ── CONFIRMED card — muted, lower elevation ──
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConfirmedTransactionCard(tx: MpesaTransaction, onTap: () -> Unit, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp), spotColor = Color.Black.copy(0.04f))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8F8F8))
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                tx.senderName.split(" ").take(2).map { it.firstOrNull() ?: ' ' }.joinToString(""),
                color = Color(0xFF999999), fontWeight = FontWeight.Bold, fontSize = 15.sp
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f).padding(vertical = 16.dp)) {
            Text(tx.senderName.split(" ").take(2).joinToString(" "), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF999999))
            Text("${tx.date}  •  ${tx.time}", fontSize = 12.sp, color = Color(0xFFBBBBBB))
        }

        Column(horizontalAlignment = Alignment.End) {
            Text("+${MpesaParser.formatAmount(tx.amount)}", color = Color(0xFF999999), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text("Done", fontSize = 10.sp, color = Color(0xFFBBBBBB), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ReverseConfirmDialog(tx: MpesaTransaction, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reverse Confirmation?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Move ${tx.senderName.split(" ").take(2).joinToString(" ")}'s payment of ${MpesaParser.formatAmount(tx.amount)} back to pending?",
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)), shape = RoundedCornerShape(6.dp)) {
                Text("Yes, Reverse", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
            .background(Color.Black.copy(alpha = 0.45f))
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
                    .clip(RoundedCornerShape(2.dp)).background(Color(0xFFDDDDDD)).align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            // Status badge
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (tx.isConfirmed) Color(0xFFEEEEEE) else MpesaGreen.copy(0.1f))
                    .border(1.dp, if (tx.isConfirmed) Color(0xFFCCCCCC) else MpesaGreen.copy(0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    if (tx.isConfirmed) "✓ CONFIRMED & HANDED OVER" else "⏳ AWAITING CONFIRMATION",
                    color = if (tx.isConfirmed) Color(0xFF999999) else MpesaGreen,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = Color(0xFF1A1A1A), thickness = 1.5.dp)

            // Amount strip
            Row(
                Modifier.fillMaxWidth().background(Color(0xFFF8FFF8)).padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("RECEIVED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                    Text(MpesaParser.formatAmount(tx.amount), color = MpesaGreen, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                    Text(MpesaParser.formatAmount(tx.newBalance), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            Divider(color = Color(0xFF1A1A1A), thickness = 1.5.dp)

            Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                DetailRow("From", tx.senderName)
                DetailRow("Phone", tx.senderPhone)
                DetailRow("Date", "${tx.date}  •  ${tx.time}")

                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Transaction ID", color = TextSecondary, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tx.transactionId, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MpesaGreen.copy(0.1f))
                                .border(1.dp, MpesaGreen.copy(0.3f), RoundedCornerShape(4.dp))
                                .clickable { clipboard.setText(AnnotatedString(tx.transactionId)); snackbarVisible = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("COPY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MpesaGreen, letterSpacing = 0.5.sp)
                        }
                    }
                }
                Divider(color = Color(0xFFEEEEEE))
                DetailRow("Tx Cost", MpesaParser.formatAmount(tx.transactionCost))
                if (tx.simSlot >= 0) DetailRow("SIM", "SIM ${tx.simSlot + 1}")
            }

            Divider(color = Color(0xFFDDDDDD))
            Row(
                Modifier.fillMaxWidth().clickable { showRaw = !showRaw }.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ORIGINAL MESSAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.8.sp)
                Icon(if (showRaw) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            AnimatedVisibility(showRaw, enter = expandVertically(), exit = shrinkVertically()) {
                Text(tx.rawMessage, fontSize = 12.sp, color = TextSecondary,
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(horizontal = 20.dp, vertical = 14.dp))
            }

            if (snackbarVisible) {
                LaunchedEffect(Unit) { delay(2000); snackbarVisible = false }
                Box(Modifier.fillMaxWidth().background(MpesaGreen).padding(14.dp), contentAlignment = Alignment.Center) {
                    Text("Transaction ID copied!", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
    }
    Divider(color = Color(0xFFEEEEEE))
}

@Composable
fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("No M-PESA Messages Yet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Messages from MPESA will appear here automatically.", color = TextSecondary, fontSize = 14.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
        }
    }
}

package com.mpesa.tracker.ui.screens

import android.Manifest
import android.os.Build
import android.telephony.SubscriptionManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mpesa.tracker.ui.theme.MpesaGreen
import com.mpesa.tracker.ui.theme.MpesaGreenDark
import com.mpesa.tracker.utils.PreferencesManager

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }

    var step by remember { mutableStateOf(0) } // 0=welcome, 1=permissions, 2=sim select
    var sim1Selected by remember { mutableStateOf(true) }
    var sim2Selected by remember { mutableStateOf(false) }

    // Detect available SIM cards
    val subscriptionManager = remember {
        context.getSystemService(SubscriptionManager::class.java)
    }

    val permissions = buildList {
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissions)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MpesaGreen, MpesaGreenDark)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(targetState = step) { currentStep ->
                when (currentStep) {
                    0 -> WelcomeStep { step = 1 }
                    1 -> PermissionsStep(
                        allGranted = permissionState.allPermissionsGranted,
                        onRequest = { permissionState.launchMultiplePermissionRequest() },
                        onNext = { step = 2 }
                    )
                    2 -> SimSelectionStep(
                        sim1Selected = sim1Selected,
                        sim2Selected = sim2Selected,
                        onSim1Toggle = { sim1Selected = it },
                        onSim2Toggle = { sim2Selected = it },
                        onDone = {
                            val simPref = when {
                                sim1Selected && sim2Selected -> -1
                                sim1Selected -> 0
                                sim2Selected -> 1
                                else -> 0
                            }
                            kotlinx.coroutines.MainScope().launch {
                                prefsManager.completeOnboarding(simPref, sim1Selected, sim2Selected)
                                onComplete()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("💚", fontSize = 48.sp)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "M-PESA Tracker",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Automatically capture, organize and\nexport your M-PESA transactions.",
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MpesaGreen),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Get Started", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun PermissionsStep(allGranted: Boolean, onRequest: () -> Unit, onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📋", fontSize = 48.sp)
        Spacer(Modifier.height(24.dp))
        Text("Permissions Needed", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text(
            "We need SMS permission to read M-PESA messages. Your data stays on your device — no internet required.",
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        listOf(
            "📩 Receive & Read SMS",
            "📱 Read Phone State (SIM info)",
            "🔔 Show Notifications"
        ).forEach { perm ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (allGranted) Color(0xFF90EE90) else Color.White.copy(0.5f))
                )
                Spacer(Modifier.width(12.dp))
                Text(perm, color = Color.White, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
        if (allGranted) {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MpesaGreen),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("✓ Permissions Granted — Continue", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MpesaGreen),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Grant Permissions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SimSelectionStep(
    sim1Selected: Boolean,
    sim2Selected: Boolean,
    onSim1Toggle: (Boolean) -> Unit,
    onSim2Toggle: (Boolean) -> Unit,
    onDone: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.SimCard, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(24.dp))
        Text("Select SIM Card(s)", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose which SIM card(s) receive M-PESA messages",
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        SimCard(
            label = "SIM 1",
            subtitle = "Primary SIM",
            selected = sim1Selected,
            onClick = { onSim1Toggle(!sim1Selected) }
        )
        Spacer(Modifier.height(16.dp))
        SimCard(
            label = "SIM 2",
            subtitle = "Secondary SIM",
            selected = sim2Selected,
            onClick = { onSim2Toggle(!sim2Selected) }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "You can change this later in Settings",
            color = Color.White.copy(0.6f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onDone,
            enabled = sim1Selected || sim2Selected,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MpesaGreen),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Start Tracking", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun SimCard(label: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.15f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.SimCard,
            contentDescription = null,
            tint = if (selected) MpesaGreen else Color.White,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, color = if (selected) MpesaGreen else Color.White, fontSize = 16.sp)
            Text(subtitle, fontSize = 13.sp, color = if (selected) MpesaGreen.copy(0.7f) else Color.White.copy(0.7f))
        }
        Checkbox(
            checked = selected,
            onCheckedChange = { onClick() },
            colors = CheckboxDefaults.colors(
                checkedColor = MpesaGreen,
                uncheckedColor = Color.White.copy(0.6f),
                checkmarkColor = Color.White
            )
        )
    }
}

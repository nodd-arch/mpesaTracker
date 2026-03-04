package com.mpesa.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpesa.tracker.ui.screens.*
import com.mpesa.tracker.ui.theme.*
import com.mpesa.tracker.utils.NotificationHelper
import com.mpesa.tracker.utils.PreferencesManager
import com.mpesa.tracker.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)

        val prefsManager = PreferencesManager(this)

        setContent {
            MpesaTrackerTheme {
                val isOnboardingDone by prefsManager.isOnboardingDone.collectAsState(initial = null)

                when (isOnboardingDone) {
                    null -> {
                        // Loading splash
                        Box(Modifier.fillMaxSize().background(MpesaGreen), contentAlignment = Alignment.Center) {
                            Text("💚", fontSize = 56.sp)
                        }
                    }
                    false -> {
                        OnboardingScreen(onComplete = {})
                    }
                    true -> {
                        MainApp(viewModel)
                    }
                }
            }
        }
    }
}

sealed class Tab(val label: String, val icon: ImageVector) {
    object Messages : Tab("Messages", Icons.Default.Message)
    object Invoice : Tab("Invoice", Icons.Default.Receipt)
    object Records : Tab("Records", Icons.Default.BarChart)
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val tabs = listOf(Tab.Messages, Tab.Invoice, Tab.Records)
    var currentTab by remember { mutableStateOf<Tab>(Tab.Messages) }

    Box(Modifier.fillMaxSize()) {
        // Screen content
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            }
        ) { tab ->
            when (tab) {
                Tab.Messages -> MessagesScreen(viewModel)
                Tab.Invoice -> InvoiceScreen(viewModel)
                Tab.Records -> RecordsScreen(viewModel)
            }
        }

        // Floating tab bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black.copy(0.15f))
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        FloatingTabItem(
                            tab = tab,
                            selected = currentTab == tab,
                            onClick = { currentTab = tab }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingTabItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) MpesaGreen else Color.Transparent
    val contentColor = if (selected) Color.White else TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 20.dp else 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(tab.icon, contentDescription = tab.label, tint = contentColor, modifier = Modifier.size(20.dp))
            AnimatedVisibility(visible = selected) {
                Row {
                    Spacer(Modifier.width(8.dp))
                    Text(tab.label, color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

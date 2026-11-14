package com.moviznow.admin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviznow.admin.data.model.AppSettings
import com.moviznow.admin.data.remote.ApiClient
import com.moviznow.admin.ui.theme.MovizColors
import com.moviznow.admin.ui.viewmodel.AdminUiState

@Composable
fun SyncAndSettingsScreen(
    state: AdminUiState,
    onSaveSettings: (AppSettings) -> Unit,
    onCheckSyncStatus: () -> Unit,
    onPushSync: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val cur = state.appSettings

    var apiBaseUrl by remember { mutableStateOf(ApiClient.getBaseUrl()) }
    var headerText by remember(cur) { mutableStateOf(cur.headerText) }
    var membershipFee by remember(cur) { mutableStateOf(cur.membershipFee.toInt().toString()) }
    var movieFee by remember(cur) { mutableStateOf(cur.movieFee.toInt().toString()) }
    var seasonFee by remember(cur) { mutableStateOf(cur.seasonFee.toInt().toString()) }
    var supportNumber by remember(cur) { mutableStateOf(cur.supportNumber) }
    var maintenanceMode by remember(cur) { mutableStateOf(cur.isMaintenanceModeEnabled) }
    var maintenanceMsg by remember(cur) { mutableStateOf(cur.maintenanceMessage) }
    var trialEnabled by remember(cur) { mutableStateOf(cur.isTrialEnabled) }
    var phoneLoginEnabled by remember(cur) { mutableStateOf(cur.isPhoneLoginEnabled) }
    var paymentEnabled by remember(cur) { mutableStateOf(cur.isPaymentEnabled) }
    var contactEnabled by remember(cur) { mutableStateOf(cur.isAdminContactEnabled) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MovizColors.BgSurface,
            contentColor = MovizColors.Emerald,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MovizColors.Emerald
                )
            }
        ) {
            listOf("App Settings", "DB Sync").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == idx) MovizColors.Emerald else MovizColors.TextSecondary
                        )
                    }
                )
            }
        }

        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = apiBaseUrl,
                    onValueChange = {
                        apiBaseUrl = it
                        ApiClient.setBaseUrl(it)
                    },
                    label = { Text("Backend Server URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = headerText, onValueChange = { headerText = it }, label = { Text("Site / App Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = membershipFee, onValueChange = { membershipFee = it }, label = { Text("VIP Fee") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = movieFee, onValueChange = { movieFee = it }, label = { Text("Movie Fee") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = seasonFee, onValueChange = { seasonFee = it }, label = { Text("Season Fee") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = supportNumber, onValueChange = { supportNumber = it }, label = { Text("Support WhatsApp Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                SettingToggleRow("Maintenance Mode", maintenanceMode) { maintenanceMode = it }
                if (maintenanceMode) {
                    OutlinedTextField(value = maintenanceMsg, onValueChange = { maintenanceMsg = it }, label = { Text("Maintenance Message") }, modifier = Modifier.fillMaxWidth())
                }
                SettingToggleRow("Enable Free Trial", trialEnabled) { trialEnabled = it }
                SettingToggleRow("Enable Phone Login", phoneLoginEnabled) { phoneLoginEnabled = it }
                SettingToggleRow("Enable Payments", paymentEnabled) { paymentEnabled = it }
                SettingToggleRow("Enable Admin Contact", contactEnabled) { contactEnabled = it }

                Button(
                    onClick = {
                        onSaveSettings(
                            cur.copy(
                                headerText = headerText.trim(),
                                membershipFee = membershipFee.toDoubleOrNull() ?: cur.membershipFee,
                                movieFee = movieFee.toDoubleOrNull() ?: cur.movieFee,
                                seasonFee = seasonFee.toDoubleOrNull() ?: cur.seasonFee,
                                supportNumber = supportNumber.trim(),
                                isMaintenanceModeEnabled = maintenanceMode,
                                maintenanceMessage = maintenanceMsg.trim(),
                                isTrialEnabled = trialEnabled,
                                isPhoneLoginEnabled = phoneLoginEnabled,
                                isPaymentEnabled = paymentEnabled,
                                isAdminContactEnabled = contactEnabled
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MovizColors.Emerald, contentColor = Color(0xFF022C22)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Settings to Firestore", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MovizColors.BgSurface,
                    border = BorderStroke(1.dp, MovizColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Multi-Database Content Sync", color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        val st = state.syncStatus
                        Text(
                            text = if (st == null) "Tap Check Status to verify service account keys."
                            else "Source: ${if (st.sourceConnected) "Connected" else "Missing"} • Target: ${if (st.targetConnected) "Connected" else "Missing"}",
                            color = MovizColors.TextSecondary,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onCheckSyncStatus, enabled = !state.isRunningSync) {
                                Text("Check Status", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onPushSync("changed") },
                                enabled = !state.isRunningSync,
                                colors = ButtonDefaults.buttonColors(containerColor = MovizColors.Emerald, contentColor = Color(0xFF022C22))
                            ) {
                                Text(if (state.isRunningSync) "Syncing..." else "Push Changed", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = { onPushSync("all") }, enabled = !state.isRunningSync) {
                                Text("Push All", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (state.syncLogs.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MovizColors.BgSurface,
                        border = BorderStroke(1.dp, MovizColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Sync Logs", color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            state.syncLogs.take(25).forEach { line ->
                                Text(line, color = MovizColors.TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MovizColors.TextPrimary, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = MovizColors.Emerald)
        )
    }
}

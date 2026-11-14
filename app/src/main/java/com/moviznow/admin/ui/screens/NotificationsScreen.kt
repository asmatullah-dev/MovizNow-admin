package com.moviznow.admin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviznow.admin.ui.theme.MovizColors
import com.moviznow.admin.ui.theme.StatusBadge
import com.moviznow.admin.ui.viewmodel.AdminUiState

@Composable
fun NotificationsScreen(
    state: AdminUiState,
    onSendNotification: (String, String, String, List<String>, String?, String?, Boolean, Boolean) -> Unit,
    onRunExpiryCheck: () -> Unit,
    onSaveTemplate: (String, String, String, String?, String?) -> Unit,
    onDeleteTemplate: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var targetType by remember { mutableStateOf("all") }
    var buttonLabel by remember { mutableStateOf("") }
    var buttonUrl by remember { mutableStateOf("") }
    var sendFcm by remember { mutableStateOf(true) }
    var sendEmail by remember { mutableStateOf(true) }
    var tplName by remember { mutableStateOf("") }

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
            listOf("Compose", "History (${state.notificationHistory.size})", "Templates").forEachIndexed { i, t ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = {
                        Text(
                            t,
                            fontSize = 12.sp,
                            color = if (selectedTab == i) MovizColors.Emerald else MovizColors.TextSecondary
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRunExpiryCheck,
                    colors = ButtonDefaults.buttonColors(containerColor = MovizColors.BgSurface, contentColor = MovizColors.Amber),
                    border = BorderStroke(1.dp, MovizColors.Amber.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Trigger Automated Expiry Check Now", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Text("Target Audience", color = MovizColors.TextSecondary, fontSize = 12.sp)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "All Devices", "registered" to "Registered Users", "guests" to "Guests")
                        .forEach { (key, label) ->
                            FilterChip(
                                selected = targetType == key,
                                onClick = { targetType = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                }

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Notification Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message Body") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = buttonLabel, onValueChange = { buttonLabel = it }, label = { Text("Button Label (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = buttonUrl, onValueChange = { buttonUrl = it }, label = { Text("Action URL / Path (e.g. /top-up)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Send Push (FCM)", color = MovizColors.TextPrimary, fontSize = 14.sp)
                    Switch(checked = sendFcm, onCheckedChange = { sendFcm = it }, colors = SwitchDefaults.colors(checkedThumbColor = MovizColors.Emerald))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Send Email", color = MovizColors.TextPrimary, fontSize = 14.sp)
                    Switch(checked = sendEmail, onCheckedChange = { sendEmail = it }, colors = SwitchDefaults.colors(checkedThumbColor = MovizColors.Emerald))
                }

                Button(
                    onClick = {
                        onSendNotification(
                            title,
                            body,
                            targetType,
                            emptyList(),
                            buttonLabel.ifBlank { null },
                            buttonUrl.ifBlank { null },
                            sendFcm,
                            sendEmail
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MovizColors.Emerald, contentColor = Color(0xFF022C22)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Send Notification", fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = tplName, onValueChange = { tplName = it }, label = { Text("Save as Template Name") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        if (tplName.isNotBlank() && title.isNotBlank() && body.isNotBlank()) {
                            onSaveTemplate(tplName.trim(), title, body, buttonLabel.ifBlank { null }, buttonUrl.ifBlank { null })
                            tplName = ""
                        }
                    }) { Text("Save") }
                }
            }

            1 -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.notificationHistory, key = { it.id }) { notif ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MovizColors.BgSurface,
                        border = BorderStroke(1.dp, MovizColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(notif.title, color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                StatusBadge(text = notif.targetAudience.uppercase(), color = MovizColors.Sky)
                            }
                            Text(notif.body, color = MovizColors.TextSecondary, fontSize = 12.sp)
                            Text(notif.createdAt.take(16).replace("T", " "), color = MovizColors.TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            2 -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.notificationTemplates, key = { it.id }) { tpl ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MovizColors.BgSurface,
                        border = BorderStroke(1.dp, MovizColors.Border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                title = tpl.title
                                body = tpl.body
                                buttonLabel = tpl.buttonLabel.orEmpty()
                                buttonUrl = tpl.buttonUrl.orEmpty()
                                selectedTab = 0
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tpl.name, color = MovizColors.Emerald, fontWeight = FontWeight.Bold)
                                Text(tpl.title, color = MovizColors.TextPrimary, fontSize = 13.sp)
                                Text(tpl.body, color = MovizColors.TextSecondary, fontSize = 12.sp, maxLines = 2)
                            }
                            IconButton(onClick = { onDeleteTemplate(tpl.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MovizColors.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.moviznow.admin.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.moviznow.admin.data.firebase.ChunkCodec
import com.moviznow.admin.ui.theme.MovizColors
import com.moviznow.admin.ui.theme.StatusBadge
import com.moviznow.admin.ui.viewmodel.AdminUiState
import kotlin.math.roundToInt

@Composable
fun AnalyticsAndIncomeScreen(
    state: AdminUiState,
    onAddIncome: (Double, String, String, String) -> Unit,
    onDeleteIncome: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isAddDialogOpen by remember { mutableStateOf(false) }

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
            listOf("Analytics", "Income Ledger").forEachIndexed { idx, label ->
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Most Clicked Content", color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                items(state.topClickedContent) { stat ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MovizColors.BgSurface,
                        border = BorderStroke(1.dp, MovizColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stat.title, color = MovizColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("ID: ${stat.id}", color = MovizColors.TextMuted, fontSize = 11.sp)
                            }
                            StatusBadge(text = "${stat.clicks} clicks", color = MovizColors.Emerald)
                        }
                    }
                }

                item {
                    Text(
                        "Most Active Users (Sessions)",
                        color = MovizColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.topUsersBySessions, key = { "sess_${it.uid}" }) { user ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MovizColors.BgSurface,
                        border = BorderStroke(1.dp, MovizColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.effectiveName, color = MovizColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(user.email, color = MovizColors.TextMuted, fontSize = 11.sp)
                            }
                            StatusBadge(text = "${user.sessionsCount} sessions", color = MovizColors.Sky)
                        }
                    }
                }

                item {
                    Text(
                        "Top Watch Time",
                        color = MovizColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.topUsersByTime, key = { "time_${it.uid}" }) { user ->
                    val mins = (user.timeSpent / 60).coerceAtLeast(1)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MovizColors.BgSurface,
                        border = BorderStroke(1.dp, MovizColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.effectiveName, color = MovizColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(user.email, color = MovizColors.TextMuted, fontSize = 11.sp)
                            }
                            StatusBadge(text = "${mins}m", color = MovizColors.Purple)
                        }
                    }
                }
            }
        } else {
            val totalIncome = state.incomeRecords.sumOf { it.amount }.roundToInt()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MovizColors.BgSurface,
                        border = BorderStroke(1.dp, MovizColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Recorded Revenue", color = MovizColors.TextSecondary, fontSize = 12.sp)
                                Text("Rs $totalIncome", color = MovizColors.Emerald, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            }
                            Button(
                                onClick = { isAddDialogOpen = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MovizColors.Emerald, contentColor = Color(0xFF022C22)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" Add Entry", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                items(state.incomeRecords, key = { it.id }) { rec ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MovizColors.BgSurface,
                        border = BorderStroke(1.dp, MovizColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.description, color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${rec.userName} • ${rec.date.take(10)}", color = MovizColors.TextSecondary, fontSize = 12.sp)
                            }
                            Text("Rs ${rec.amount.roundToInt()}", color = MovizColors.Emerald, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDeleteIncome(rec.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MovizColors.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddDialogOpen) {
        var amount by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var user by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { isAddDialogOpen = false },
            containerColor = MovizColors.BgSurface,
            title = { Text("Add Income Record", color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (Rs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, singleLine = true)
                    OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("User Name (Optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: return@Button
                        if (desc.isNotBlank()) {
                            onAddIncome(amt, desc.trim(), user.trim(), ChunkCodec.getNowUtcIso())
                            isAddDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MovizColors.Emerald, contentColor = Color(0xFF022C22))
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { isAddDialogOpen = false }) { Text("Cancel") }
            }
        )
    }
}

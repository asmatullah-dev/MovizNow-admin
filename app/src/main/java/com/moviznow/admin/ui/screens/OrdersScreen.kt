package com.moviznow.admin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.moviznow.admin.data.model.OrderRecord
import com.moviznow.admin.ui.theme.MovizColors
import com.moviznow.admin.ui.theme.StatusBadge
import com.moviznow.admin.ui.theme.statusColor
import com.moviznow.admin.ui.viewmodel.AdminUiState
import kotlin.math.roundToInt

@Composable
fun OrdersScreen(
    state: AdminUiState,
    onApproveOrder: (OrderRecord) -> Unit,
    onDeclineOrder: (OrderRecord) -> Unit,
    onAiVerifyOrder: (OrderRecord) -> Unit
) {
    var statusFilter by remember { mutableStateOf("pending") }
    var searchQuery by remember { mutableStateOf("") }
    var previewScreenshotUrl by remember { mutableStateOf<String?>(null) }

    val filtered = remember(state.orders, statusFilter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        state.orders.filter { o ->
            val matchStatus = statusFilter == "all" || o.status == statusFilter
            val matchQ = q.isEmpty() ||
                o.userName.lowercase().contains(q) ||
                o.userEmail.lowercase().contains(q) ||
                o.trxId.orEmpty().lowercase().contains(q) ||
                o.id.lowercase().contains(q)
            matchStatus && matchQ
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search user, email, Trx ID...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MovizColors.Emerald,
                unfocusedBorderColor = MovizColors.Border,
                focusedContainerColor = MovizColors.BgSurface,
                unfocusedContainerColor = MovizColors.BgSurface,
                focusedTextColor = MovizColors.TextPrimary,
                unfocusedTextColor = MovizColors.TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("pending" to "Pending", "approved" to "Approved", "declined" to "Declined", "all" to "All")
                .forEach { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MovizColors.Emerald.copy(alpha = 0.2f),
                            selectedLabelColor = MovizColors.Emerald,
                            containerColor = MovizColors.BgSurface,
                            labelColor = MovizColors.TextSecondary
                        )
                    )
                }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered, key = { "${it.userId}_${it.id}" }) { order ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MovizColors.BgSurface,
                    border = BorderStroke(1.dp, MovizColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = order.userName.ifBlank { order.userEmail.ifBlank { order.userId } },
                                    color = MovizColors.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = order.userEmail,
                                    color = MovizColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            StatusBadge(text = order.status.uppercase(), color = statusColor(order.status))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = order.planName ?: if (order.type == "content") "Selected Content (${order.items.size})" else "${order.months} Month(s) Membership",
                                color = MovizColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Rs ${order.amount.roundToInt()}",
                                color = MovizColors.Emerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        if (!order.trxId.isNullOrBlank() || !order.senderBank.isNullOrBlank()) {
                            Text(
                                text = listOfNotNull(
                                    order.senderBank?.takeIf { it.isNotBlank() }?.let { "Bank: $it" },
                                    order.trxId?.takeIf { it.isNotBlank() }?.let { "Trx: $it" },
                                    order.accountTitle?.takeIf { it.isNotBlank() }?.let { "Sender: $it" }
                                ).joinToString(" • "),
                                color = MovizColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        if (!order.aiVerificationReason.isNullOrBlank()) {
                            Text(
                                text = "AI Note: ${order.aiVerificationReason}",
                                color = MovizColors.Cyan,
                                fontSize = 11.sp
                            )
                        }

                        if (!order.paymentScreenshotUrl.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { previewScreenshotUrl = order.paymentScreenshotUrl },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" View Payment Receipt", fontSize = 12.sp)
                            }
                        }

                        if (order.status == "pending") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onApproveOrder(order) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MovizColors.Emerald,
                                        contentColor = Color(0xFF022C22)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" Approve", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onAiVerifyOrder(order) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MovizColors.Amber, modifier = Modifier.size(16.dp))
                                    Text(" AI", color = MovizColors.Amber, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onDeclineOrder(order) },
                                    border = BorderStroke(1.dp, MovizColors.Red.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = MovizColors.Red, modifier = Modifier.size(16.dp))
                                    Text(" Decline", color = MovizColors.Red, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    previewScreenshotUrl?.let { url ->
        Dialog(onDismissRequest = { previewScreenshotUrl = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MovizColors.BgSurface) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.End) {
                    IconButton(onClick = { previewScreenshotUrl = null }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MovizColors.TextPrimary)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Payment Receipt",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}

package com.moviznow.admin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.RequestPage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviznow.admin.ui.theme.MovizColors
import com.moviznow.admin.ui.theme.StatusBadge
import com.moviznow.admin.ui.theme.statusColor
import com.moviznow.admin.ui.viewmodel.AdminNavDestination
import com.moviznow.admin.ui.viewmodel.AdminUiState
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    state: AdminUiState,
    onNavigate: (AdminNavDestination) -> Unit,
    onSyncUsers: () -> Unit,
    onRunExpiryCheck: () -> Unit
) {
    val activeUsers = state.users.count { it.status == "active" }
    val vipUsers = state.users.count { it.role == "vip" && it.status == "active" }
    val moviesCount = state.contentList.count { it.type == "movie" }
    val seriesCount = state.contentList.count { it.type == "series" }
    val totalIncome = state.incomeRecords.sumOf { it.amount }.roundToInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.pendingChangesCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MovizColors.Amber.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MovizColors.Amber.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${state.pendingChangesCount} Pending User Change(s)",
                                color = MovizColors.Amber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Sync to commit queued user edits to Firestore",
                                color = MovizColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = onSyncUsers,
                            enabled = !state.isSyncingUsers,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MovizColors.Amber,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (state.isSyncingUsers) " Syncing..." else " Sync Now",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Operations Overview",
                color = MovizColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Users",
                    value = "${state.users.size}",
                    subtitle = "$activeUsers active • $vipUsers VIP",
                    icon = Icons.Default.People,
                    accent = MovizColors.Emerald,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AdminNavDestination.USERS) }
                )
                KpiCard(
                    title = "Catalog",
                    value = "${state.contentList.size}",
                    subtitle = "$moviesCount movies • $seriesCount series",
                    icon = Icons.Default.Movie,
                    accent = MovizColors.Sky,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AdminNavDestination.CONTENT) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Pending Orders",
                    value = "${state.pendingOrdersCount}",
                    subtitle = "${state.orders.size} total orders",
                    icon = Icons.Default.ReceiptLong,
                    accent = MovizColors.Amber,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AdminNavDestination.ORDERS) }
                )
                KpiCard(
                    title = "Income",
                    value = "Rs $totalIncome",
                    subtitle = "${state.incomeRecords.size} records",
                    icon = Icons.Default.AttachMoney,
                    accent = MovizColors.Purple,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AdminNavDestination.ANALYTICS_INCOME) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Movie Requests",
                    value = "${state.pendingRequestsCount}",
                    subtitle = "${state.movieRequests.size} grouped",
                    icon = Icons.Default.RequestPage,
                    accent = MovizColors.Cyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AdminNavDestination.REQUESTS_REPORTS) }
                )
                KpiCard(
                    title = "Broken Reports",
                    value = "${state.pendingReportedLinksCount}",
                    subtitle = "${state.reportedLinks.size} reported",
                    icon = Icons.Default.ReportProblem,
                    accent = MovizColors.Red,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AdminNavDestination.REQUESTS_REPORTS) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigate(AdminNavDestination.NOTIFICATIONS) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MovizColors.BgSurface,
                        contentColor = MovizColors.TextPrimary
                    ),
                    border = BorderStroke(1.dp, MovizColors.Border),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MovizColors.Emerald, modifier = Modifier.size(16.dp))
                    Text(" Send Alert", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onRunExpiryCheck,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MovizColors.BgSurface,
                        contentColor = MovizColors.TextPrimary
                    ),
                    border = BorderStroke(1.dp, MovizColors.Border),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Run Expiry Check", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Text(
                text = "Recent Pending Orders",
                color = MovizColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        val pendingOrders = state.orders.filter { it.status == "pending" }.take(5)
        if (pendingOrders.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MovizColors.BgSurface,
                    border = BorderStroke(1.dp, MovizColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No pending orders right now.",
                        color = MovizColors.TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(pendingOrders, key = { "${it.userId}_${it.id}" }) { order ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MovizColors.BgSurface,
                    border = BorderStroke(1.dp, MovizColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AdminNavDestination.ORDERS) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = order.userName.ifBlank { order.userEmail.ifBlank { order.userId } },
                                color = MovizColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${order.planName ?: order.type.uppercase()} • Rs ${order.amount.roundToInt()}",
                                color = MovizColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        StatusBadge(text = order.status.uppercase(), color = statusColor(order.status))
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MovizColors.BgSurface,
        border = BorderStroke(1.dp, MovizColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = MovizColors.TextSecondary, fontSize = 12.sp)
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(
                text = value,
                color = MovizColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = subtitle, color = MovizColors.TextMuted, fontSize = 11.sp, maxLines = 1)
        }
    }
}

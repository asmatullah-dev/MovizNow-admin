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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.moviznow.admin.data.model.MovieRequestRecord
import com.moviznow.admin.data.model.ReportedLinkRecord
import com.moviznow.admin.ui.theme.MovizColors
import com.moviznow.admin.ui.theme.StatusBadge
import com.moviznow.admin.ui.theme.statusColor
import com.moviznow.admin.ui.viewmodel.AdminUiState

@Composable
fun RequestsAndReportsScreen(
    state: AdminUiState,
    onUpdateRequestStatus: (MovieRequestRecord, String, String?) -> Unit,
    onResolveReport: (ReportedLinkRecord) -> Unit,
    onCheckLinkHealth: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var testUrl by remember { mutableStateOf("") }

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
            listOf(
                "Requests (${state.pendingRequestsCount})",
                "Reported Links (${state.pendingReportedLinksCount})"
            ).forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = label,
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
                items(state.movieRequests, key = { it.id }) { req ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
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
                                    Text(req.title, color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = "${req.type.uppercase()}${req.year?.let { " ($it)" } ?: ""} • Requested ${req.requestCount} time(s)",
                                        color = MovizColors.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                StatusBadge(text = req.status.uppercase(), color = statusColor(req.status))
                            }

                            if (!req.adminComment.isNullOrBlank()) {
                                Text("Note: ${req.adminComment}", color = MovizColors.Sky, fontSize = 12.sp)
                            }

                            if (req.status == "pending") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onUpdateRequestStatus(req, "added", "Added to catalog") },
                                        colors = ButtonDefaults.buttonColors(containerColor = MovizColors.Emerald, contentColor = Color(0xFF022C22)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Text(" Mark Added", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { onUpdateRequestStatus(req, "rejected", "Currently unavailable") },
                                        border = BorderStroke(1.dp, MovizColors.Red.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = MovizColors.Red, modifier = Modifier.size(15.dp))
                                        Text(" Reject", color = MovizColors.Red, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MovizColors.BgSurface,
                        border = BorderStroke(1.dp, MovizColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Link Health Checker", color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = testUrl,
                                    onValueChange = { testUrl = it },
                                    placeholder = { Text("Paste download URL to verify...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = { onCheckLinkHealth(testUrl) }) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (!state.linkCheckOutput.isNullOrBlank()) {
                                Text(state.linkCheckOutput, color = MovizColors.Emerald, fontSize = 12.sp)
                            }
                        }
                    }
                }

                items(state.reportedLinks, key = { "${it.userId}_${it.id}" }) { rep ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MovizColors.BgSurface,
                        border = BorderStroke(1.dp, MovizColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(rep.contentTitle, color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                StatusBadge(text = rep.status.uppercase(), color = statusColor(rep.status))
                            }
                            Text("Link: ${rep.linkName} • By ${rep.userName}", color = MovizColors.TextSecondary, fontSize = 12.sp)
                            Text(rep.linkUrl, color = MovizColors.TextMuted, fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onCheckLinkHealth(rep.linkUrl) }) {
                                    Text("Check Link", fontSize = 12.sp)
                                }
                                if (rep.status == "pending") {
                                    Button(
                                        onClick = { onResolveReport(rep) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MovizColors.Emerald, contentColor = Color(0xFF022C22))
                                    ) {
                                        Text("Mark Resolved", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

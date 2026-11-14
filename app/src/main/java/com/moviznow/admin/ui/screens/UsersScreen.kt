package com.moviznow.admin.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.moviznow.admin.data.firebase.ChunkCodec
import com.moviznow.admin.data.model.ContentItem
import com.moviznow.admin.data.model.FcmStatus
import com.moviznow.admin.data.model.UserProfile
import com.moviznow.admin.ui.theme.MovizColors
import com.moviznow.admin.ui.theme.StatusBadge
import com.moviznow.admin.ui.theme.roleColor
import com.moviznow.admin.ui.theme.roleLabel
import com.moviznow.admin.ui.theme.statusColor
import com.moviznow.admin.ui.viewmodel.AdminUiState

private enum class UserFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    PENDING("Pending"),
    EXPIRED("Expired"),
    VIP("VIP"),
    BASIC("Basic"),
    SELECTED("Selected"),
    WHITELISTED("Whitelisted"),
    CONSHARE("ConShare")
}

@Composable
fun UsersScreen(
    state: AdminUiState,
    onQueueUserUpdate: (String, Map<String, Any?>) -> Unit,
    onSyncPending: () -> Unit,
    onToggleWhitelist: (UserProfile) -> Unit,
    onDeleteUser: (String) -> Unit,
    onSendStatusEmail: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(UserFilter.ALL) }
    var editingUser by remember { mutableStateOf<UserProfile?>(null) }
    var assigningUser by remember { mutableStateOf<UserProfile?>(null) }
    var userToDelete by remember { mutableStateOf<UserProfile?>(null) }

    val filteredUsers = remember(state.users, searchQuery, activeFilter) {
        val q = searchQuery.trim().lowercase()
        state.users.filter { u ->
            val matchesFilter = when (activeFilter) {
                UserFilter.ALL -> u.status != "deleted"
                UserFilter.ACTIVE -> u.status == "active"
                UserFilter.PENDING -> u.status == "pending"
                UserFilter.EXPIRED -> u.status == "expired"
                UserFilter.VIP -> u.role == "vip"
                UserFilter.BASIC -> u.role == "basic"
                UserFilter.SELECTED -> u.role == "selected_content"
                UserFilter.WHITELISTED -> u.isWhitelisted
                UserFilter.CONSHARE -> u.conshare
            }
            val matchesQuery = q.isEmpty() ||
                u.effectiveName.lowercase().contains(q) ||
                u.email.lowercase().contains(q) ||
                u.phone.orEmpty().lowercase().contains(q) ||
                u.city.orEmpty().lowercase().contains(q) ||
                u.uid.lowercase().contains(q)
            matchesFilter && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.pendingChangesCount > 0) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MovizColors.Emerald.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, MovizColors.Emerald.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${state.pendingChangesCount} unsynced user edit(s)",
                        color = MovizColors.Emerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = onSyncPending,
                        enabled = !state.isSyncingUsers,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MovizColors.Emerald,
                            contentColor = Color(0xFF022C22)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(
                            text = if (state.isSyncingUsers) " Syncing..." else " Sync Pending",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search name, email, phone, city...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
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
            UserFilter.entries.forEach { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { activeFilter = filter },
                    label = { Text(filter.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MovizColors.Emerald.copy(alpha = 0.2f),
                        selectedLabelColor = MovizColors.Emerald,
                        containerColor = MovizColors.BgSurface,
                        labelColor = MovizColors.TextSecondary
                    )
                )
            }
        }

        Text(
            text = "${filteredUsers.size} user(s)",
            color = MovizColors.TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredUsers, key = { it.uid }) { user ->
                UserAdminCard(
                    user = user,
                    hasPending = state.pendingUserUpdates.containsKey(user.uid),
                    onClick = { editingUser = user },
                    onWhatsApp = {
                        val digits = ChunkCodec.standardizePhone(user.phone).removePrefix("+")
                        if (digits.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
                            context.startActivity(intent)
                        }
                    },
                    onSendEmail = { onSendStatusEmail(user) },
                    onAssignContent = { assigningUser = user },
                    onDelete = { userToDelete = user }
                )
            }
        }
    }

    editingUser?.let { target ->
        val freshUser = state.users.find { it.uid == target.uid } ?: target
        UserEditDialog(
            user = freshUser,
            onDismiss = { editingUser = null },
            onSaveFields = { fields ->
                onQueueUserUpdate(freshUser.uid, fields)
                editingUser = null
            },
            onToggleWhitelist = { onToggleWhitelist(freshUser) },
            onOpenAssignContent = {
                editingUser = null
                assigningUser = freshUser
            }
        )
    }

    assigningUser?.let { target ->
        val freshUser = state.users.find { it.uid == target.uid } ?: target
        AssignContentDialog(
            user = freshUser,
            contentList = state.contentList,
            onDismiss = { assigningUser = null },
            onSaveAssigned = { ids ->
                onQueueUserUpdate(freshUser.uid, mapOf("assignedContent" to ids))
                assigningUser = null
            }
        )
    }

    userToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            containerColor = MovizColors.BgSurface,
            title = { Text("Delete User?", color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Permanently delete ${target.effectiveName} (${target.email})?",
                    color = MovizColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(target.uid)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MovizColors.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel", color = MovizColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun UserAdminCard(
    user: UserProfile,
    hasPending: Boolean,
    onClick: () -> Unit,
    onWhatsApp: () -> Unit,
    onSendEmail: () -> Unit,
    onAssignContent: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MovizColors.BgSurface,
        border = BorderStroke(
            1.dp,
            if (hasPending) MovizColors.Amber.copy(alpha = 0.6f) else MovizColors.Border
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!user.photoURL.isNullOrBlank()) {
                    AsyncImage(
                        model = user.photoURL,
                        contentDescription = user.effectiveName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MovizColors.BgElevated,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user.effectiveName.firstOrNull()?.uppercase() ?: "?",
                                color = MovizColors.TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = user.effectiveName,
                            color = MovizColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!user.city.isNullOrBlank()) {
                            Text(text = "(${user.city})", color = MovizColors.TextMuted, fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = if (user.email.endsWith("@moviznow.com")) "${user.phone ?: ""} (Phone)" else user.email.ifBlank { "No Email" },
                        color = MovizColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MovizColors.TextMuted, modifier = Modifier.size(12.dp))
                        Text(
                            text = user.phone ?: "No phone",
                            color = MovizColors.TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MovizColors.TextMuted, modifier = Modifier.size(12.dp))
                        Text(
                            text = "Active: ${user.lastActive?.take(16)?.replace("T", " ") ?: "Never"}",
                            color = MovizColors.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(text = roleLabel(user.role), color = roleColor(user.role))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusBadge(text = user.status.uppercase(), color = statusColor(user.status))
                        Icon(
                            imageVector = if (user.fcmStatus == FcmStatus.ACTIVE) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = when (user.fcmStatus) {
                                FcmStatus.ACTIVE -> MovizColors.Emerald
                                FcmStatus.DISABLED_IN_SETTINGS -> MovizColors.Amber
                                else -> MovizColors.TextMuted
                            },
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (user.web) {
                        StatusBadge(text = "Web", color = MovizColors.Blue) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MovizColors.Blue, modifier = Modifier.size(10.dp))
                        }
                    }
                    if (user.isAndroidUser) {
                        StatusBadge(text = "Android", color = MovizColors.Emerald) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = MovizColors.Emerald, modifier = Modifier.size(10.dp))
                        }
                    }
                    if (user.isWhitelisted) {
                        StatusBadge(text = "Whitelisted", color = MovizColors.Emerald) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MovizColors.Emerald, modifier = Modifier.size(10.dp))
                        }
                    }
                    if (user.conshare) {
                        StatusBadge(text = "ConShare", color = MovizColors.Purple) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MovizColors.Purple, modifier = Modifier.size(10.dp))
                        }
                    }
                }

                Text(
                    text = user.expiryDate?.take(10) ?: "No Expiry",
                    color = if (user.status == "expired") MovizColors.Red else MovizColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(color = MovizColors.Border)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onWhatsApp, enabled = !user.phone.isNullOrBlank(), modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = MovizColors.Emerald, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onSendEmail, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Email, contentDescription = "Status Email", tint = MovizColors.Sky, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onAssignContent, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Movie, contentDescription = "Assign Content", tint = MovizColors.Pink, modifier = Modifier.size(18.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MovizColors.TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    if (user.role != "owner") {
                        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MovizColors.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserEditDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSaveFields: (Map<String, Any?>) -> Unit,
    onToggleWhitelist: () -> Unit,
    onOpenAssignContent: () -> Unit
) {
    var role by remember { mutableStateOf(user.role) }
    var status by remember { mutableStateOf(user.status) }
    var expiryDate by remember { mutableStateOf(user.expiryDate.orEmpty()) }
    var conshare by remember { mutableStateOf(user.conshare) }
    var phone by remember { mutableStateOf(user.phone.orEmpty()) }
    var city by remember { mutableStateOf(user.city.orEmpty()) }

    val roles = listOf("user", "basic", "vip", "selected_content", "trial", "user_manager", "content_manager", "manager", "admin")
    val statuses = listOf("pending", "active", "expired", "suspended")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            color = MovizColors.BgSurface,
            border = BorderStroke(1.dp, MovizColors.Border)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.effectiveName, color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(user.email.ifBlank { user.uid }, color = MovizColors.TextSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MovizColors.TextSecondary)
                    }
                }

                Text("Role", color = MovizColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(roleLabel(r), fontSize = 11.sp) }
                        )
                    }
                }

                Text("Status", color = MovizColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.uppercase(), fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry Date (ISO or Lifetime)") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf(1 to "+1M", 3 to "+3M", 6 to "+6M", 12 to "+1Y").forEach { (m, label) ->
                        OutlinedButton(
                            onClick = {
                                expiryDate = ChunkCodec.extendExpiryIso(expiryDate.ifBlank { null }, m)
                                status = "active"
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(label, fontSize = 11.sp, color = MovizColors.Emerald)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            expiryDate = "Lifetime"
                            status = "active"
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Lifetime", fontSize = 11.sp, color = MovizColors.Amber)
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ConShare Permission", color = MovizColors.TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = conshare,
                        onCheckedChange = { conshare = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MovizColors.Emerald)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onToggleWhitelist, modifier = Modifier.weight(1f)) {
                        Text(if (user.isWhitelisted) "Remove Whitelist" else "Whitelist Phone", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onOpenAssignContent, modifier = Modifier.weight(1f)) {
                        Text("Assigned (${user.assignedContent.size})", fontSize = 12.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MovizColors.TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val fields = mutableMapOf<String, Any?>(
                                "role" to role,
                                "status" to status,
                                "conshare" to conshare,
                                "phone" to phone.trim(),
                                "city" to city.trim(),
                                "expiryDate" to (expiryDate.trim().ifBlank { "__DELETE_FIELD__" })
                            )
                            onSaveFields(fields)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MovizColors.Emerald,
                            contentColor = Color(0xFF022C22)
                        )
                    ) {
                        Text("Stage Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignContentDialog(
    user: UserProfile,
    contentList: List<ContentItem>,
    onDismiss: () -> Unit,
    onSaveAssigned: (List<String>) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(user.assignedContent.toSet()) }

    val filtered = remember(contentList, search) {
        val q = search.trim().lowercase()
        contentList.filter { q.isEmpty() || it.title.lowercase().contains(q) }.take(80)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            color = MovizColors.BgSurface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Assign Content • ${user.effectiveName}",
                    color = MovizColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Filter movies & series...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.id }) { item ->
                        val checked = selectedIds.contains(item.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (checked) selectedIds - item.id else selectedIds + item.id
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedIds = if (it) selectedIds + item.id else selectedIds - item.id
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MovizColors.Emerald)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = MovizColors.TextPrimary, fontSize = 14.sp, maxLines = 1)
                                Text("${item.type.uppercase()} • ${item.year}", color = MovizColors.TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onSaveAssigned(selectedIds.toList()) },
                        colors = ButtonDefaults.buttonColors(containerColor = MovizColors.Emerald, contentColor = Color(0xFF022C22))
                    ) {
                        Text("Save (${selectedIds.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

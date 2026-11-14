package com.moviznow.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moviznow.admin.ui.screens.AnalyticsAndIncomeScreen
import com.moviznow.admin.ui.screens.ContentScreen
import com.moviznow.admin.ui.screens.DashboardScreen
import com.moviznow.admin.ui.screens.LoginScreen
import com.moviznow.admin.ui.screens.NotificationsScreen
import com.moviznow.admin.ui.screens.OrdersScreen
import com.moviznow.admin.ui.screens.RequestsAndReportsScreen
import com.moviznow.admin.ui.screens.SyncAndSettingsScreen
import com.moviznow.admin.ui.screens.UsersScreen
import com.moviznow.admin.ui.theme.MovizColors
import com.moviznow.admin.ui.theme.MovizNowAdminTheme
import com.moviznow.admin.ui.theme.roleLabel
import com.moviznow.admin.ui.viewmodel.AdminNavDestination
import com.moviznow.admin.ui.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovizNowAdminTheme {
                MovizNowAdminRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovizNowAdminRoot(vm: AdminViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentDest by remember { mutableStateOf(AdminNavDestination.DASHBOARD) }

    LaunchedEffect(state.toastMessage) {
        val msg = state.toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearToast()
        }
    }

    if (state.isCheckingAuth) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MovizColors.BgPrimary),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MovizColors.Emerald)
        }
        return
    }

    val admin = state.currentAdmin
    if (admin == null) {
        LoginScreen(
            isLoggingIn = state.isLoggingIn,
            authError = state.authError,
            onSignIn = vm::signIn
        )
        return
    }

    val allowed = vm.allowedDestinations()
    if (!allowed.contains(currentDest) && allowed.isNotEmpty()) {
        currentDest = allowed.first()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MovizColors.BgSurface) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = state.appSettings.headerText,
                        color = MovizColors.Emerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "${admin.effectiveName} • ${roleLabel(admin.role)}",
                        color = MovizColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                HorizontalDivider(color = MovizColors.Border)
                Spacer(modifier = Modifier.height(8.dp))

                allowed.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.label, fontSize = 14.sp) },
                        icon = { Icon(iconForDest(dest), contentDescription = null) },
                        selected = currentDest == dest,
                        onClick = {
                            currentDest = dest
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MovizColors.Emerald.copy(alpha = 0.14f),
                            selectedIconColor = MovizColors.Emerald,
                            selectedTextColor = MovizColors.Emerald,
                            unselectedIconColor = MovizColors.TextSecondary,
                            unselectedTextColor = MovizColors.TextSecondary
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = MovizColors.Border)
                NavigationDrawerItem(
                    label = { Text("Sign Out", color = MovizColors.Red) },
                    icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MovizColors.Red) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        vm.signOut()
                    },
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MovizColors.BgPrimary,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MovizColors.BgSurface,
                        titleContentColor = MovizColors.TextPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MovizColors.TextPrimary)
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = currentDest.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "${state.appSettings.headerText} Admin",
                                color = MovizColors.Emerald,
                                fontSize = 11.sp
                            )
                        }
                    },
                    actions = {
                        if (state.pendingChangesCount > 0) {
                            IconButton(onClick = vm::syncPendingUserChanges) {
                                BadgedBox(badge = { Badge { Text("${state.pendingChangesCount}") } }) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = "Sync Pending", tint = MovizColors.Amber)
                                }
                            }
                        }
                        IconButton(onClick = { vm.loadAllAdminData(isManualRefresh = true) }) {
                            if (state.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MovizColors.Emerald,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MovizColors.TextSecondary)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                val bottomItems = allowed.take(4)
                if (bottomItems.size > 1) {
                    NavigationBar(containerColor = MovizColors.BgSurface) {
                        bottomItems.forEach { dest ->
                            NavigationBarItem(
                                selected = currentDest == dest,
                                onClick = { currentDest = dest },
                                icon = {
                                    val count = when (dest) {
                                        AdminNavDestination.ORDERS -> state.pendingOrdersCount
                                        AdminNavDestination.USERS -> state.pendingChangesCount
                                        else -> 0
                                    }
                                    if (count > 0) {
                                        BadgedBox(badge = { Badge { Text("$count") } }) {
                                            Icon(iconForDest(dest), contentDescription = dest.label)
                                        }
                                    } else {
                                        Icon(iconForDest(dest), contentDescription = dest.label)
                                    }
                                },
                                label = { Text(dest.id, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MovizColors.Emerald,
                                    selectedTextColor = MovizColors.Emerald,
                                    indicatorColor = MovizColors.Emerald.copy(alpha = 0.14f),
                                    unselectedIconColor = MovizColors.TextMuted,
                                    unselectedTextColor = MovizColors.TextMuted
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = MovizColors.Emerald)
                            Text("Loading moviznow-app data...", color = MovizColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    when (currentDest) {
                        AdminNavDestination.DASHBOARD -> DashboardScreen(
                            state = state,
                            onNavigate = { currentDest = it },
                            onSyncUsers = vm::syncPendingUserChanges,
                            onRunExpiryCheck = vm::runExpiryCheck
                        )
                        AdminNavDestination.USERS -> UsersScreen(
                            state = state,
                            onQueueUserUpdate = vm::queueUserUpdate,
                            onSyncPending = vm::syncPendingUserChanges,
                            onToggleWhitelist = vm::toggleUserWhitelist,
                            onDeleteUser = vm::deleteUser,
                            onSendStatusEmail = vm::notifyUserMembershipEmail
                        )
                        AdminNavDestination.ORDERS -> OrdersScreen(
                            state = state,
                            onApproveOrder = vm::approveOrder,
                            onDeclineOrder = vm::declineOrder,
                            onAiVerifyOrder = vm::verifyOrderWithAi
                        )
                        AdminNavDestination.CONTENT -> ContentScreen(
                            state = state,
                            onSaveContent = vm::saveContent,
                            onDeleteContent = vm::deleteContent,
                            onSearchTmdb = vm::searchTmdb,
                            onClearTmdb = vm::clearTmdbResults,
                            onSaveGenre = vm::saveGenre,
                            onDeleteGenre = vm::deleteGenre,
                            onSaveLanguage = vm::saveLanguage,
                            onDeleteLanguage = vm::deleteLanguage,
                            onSaveQuality = vm::saveQuality,
                            onDeleteQuality = vm::deleteQuality,
                            onSaveCollection = vm::saveCollection,
                            onDeleteCollection = vm::deleteCollection
                        )
                        AdminNavDestination.REQUESTS_REPORTS -> RequestsAndReportsScreen(
                            state = state,
                            onUpdateRequestStatus = vm::updateMovieRequestStatus,
                            onResolveReport = vm::resolveReportedLink,
                            onCheckLinkHealth = vm::checkLinkHealth
                        )
                        AdminNavDestination.NOTIFICATIONS -> NotificationsScreen(
                            state = state,
                            onSendNotification = vm::sendNotification,
                            onRunExpiryCheck = vm::runExpiryCheck,
                            onSaveTemplate = vm::saveTemplate,
                            onDeleteTemplate = vm::deleteTemplate
                        )
                        AdminNavDestination.ANALYTICS_INCOME -> AnalyticsAndIncomeScreen(
                            state = state,
                            onAddIncome = vm::addIncomeRecord,
                            onDeleteIncome = vm::deleteIncomeRecord
                        )
                        AdminNavDestination.SETTINGS_SYNC -> SyncAndSettingsScreen(
                            state = state,
                            onSaveSettings = vm::saveAppSettings,
                            onCheckSyncStatus = vm::checkDatabaseSyncStatus,
                            onPushSync = vm::pushDatabaseSync
                        )
                    }
                }
            }
        }
    }
}

private fun iconForDest(dest: AdminNavDestination): ImageVector = when (dest) {
    AdminNavDestination.DASHBOARD -> Icons.Default.Dashboard
    AdminNavDestination.USERS -> Icons.Default.People
    AdminNavDestination.ORDERS -> Icons.Default.ReceiptLong
    AdminNavDestination.CONTENT -> Icons.Default.Movie
    AdminNavDestination.REQUESTS_REPORTS -> Icons.Default.ReportProblem
    AdminNavDestination.NOTIFICATIONS -> Icons.Default.Notifications
    AdminNavDestination.ANALYTICS_INCOME -> Icons.Default.Analytics
    AdminNavDestination.SETTINGS_SYNC -> Icons.Default.Settings
}

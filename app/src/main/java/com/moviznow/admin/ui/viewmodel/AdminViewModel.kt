package com.moviznow.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviznow.admin.data.firebase.ChunkCodec
import com.moviznow.admin.data.firebase.MovizNowFirebase
import com.moviznow.admin.data.model.AppCollection
import com.moviznow.admin.data.model.AppNotification
import com.moviznow.admin.data.model.AppSettings
import com.moviznow.admin.data.model.ContentItem
import com.moviznow.admin.data.model.Genre
import com.moviznow.admin.data.model.IncomeRecord
import com.moviznow.admin.data.model.Language
import com.moviznow.admin.data.model.MovieRequestRecord
import com.moviznow.admin.data.model.NotificationTemplate
import com.moviznow.admin.data.model.OrderRecord
import com.moviznow.admin.data.model.Quality
import com.moviznow.admin.data.model.ReportedLinkRecord
import com.moviznow.admin.data.model.SyncStatusResult
import com.moviznow.admin.data.model.TmdbSearchResult
import com.moviznow.admin.data.model.UserProfile
import com.moviznow.admin.data.remote.ApiClient
import com.moviznow.admin.data.repository.AdminRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

data class AnalyticsClickStat(
    val id: String,
    val title: String,
    val clicks: Int
)

data class AdminUiState(
    val isCheckingAuth: Boolean = true,
    val currentAdmin: UserProfile? = null,
    val authError: String? = null,
    val isLoggingIn: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val toastMessage: String? = null,

    val users: List<UserProfile> = emptyList(),
    val pendingUserUpdates: Map<String, Map<String, Any?>> = emptyMap(),
    val isSyncingUsers: Boolean = false,

    val contentList: List<ContentItem> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val languages: List<Language> = emptyList(),
    val qualities: List<Quality> = emptyList(),
    val collections: List<AppCollection> = emptyList(),

    val orders: List<OrderRecord> = emptyList(),
    val movieRequests: List<MovieRequestRecord> = emptyList(),
    val reportedLinks: List<ReportedLinkRecord> = emptyList(),
    val incomeRecords: List<IncomeRecord> = emptyList(),
    val notificationHistory: List<AppNotification> = emptyList(),
    val notificationTemplates: List<NotificationTemplate> = emptyList(),
    val appSettings: AppSettings = AppSettings(),
    val rawSettingsData: Map<String, Any?> = emptyMap(),

    val tmdbResults: List<TmdbSearchResult> = emptyList(),
    val isSearchingTmdb: Boolean = false,
    val syncStatus: SyncStatusResult? = null,
    val syncLogs: List<String> = emptyList(),
    val isRunningSync: Boolean = false,
    val linkCheckOutput: String? = null
) {
    val pendingChangesCount: Int get() = pendingUserUpdates.size
    val pendingOrdersCount: Int get() = orders.count { it.status == "pending" }
    val pendingRequestsCount: Int get() = movieRequests.count { it.status == "pending" }
    val pendingReportedLinksCount: Int get() = reportedLinks.count { it.status == "pending" }

    val topClickedContent: List<AnalyticsClickStat>
        get() {
            val counts = mutableMapOf<String, AnalyticsClickStat>()
            users.forEach { u ->
                u.clickHistory.forEach { entry ->
                    val existing = counts[entry.title]
                    if (existing == null) {
                        counts[entry.title] = AnalyticsClickStat(entry.id, entry.title, entry.clicks)
                    } else {
                        counts[entry.title] = existing.copy(
                            id = if (existing.id == "unknown" && entry.id != "unknown") entry.id else existing.id,
                            clicks = existing.clicks + entry.clicks
                        )
                    }
                }
            }
            return counts.values.sortedByDescending { it.clicks }.take(15)
        }

    val topUsersBySessions: List<UserProfile>
        get() = users.filter { it.sessionsCount > 0 }.sortedByDescending { it.sessionsCount }.take(10)

    val topUsersByTime: List<UserProfile>
        get() = users.filter { it.timeSpent > 0 }.sortedByDescending { it.timeSpent }.take(10)
}

enum class AdminNavDestination(val id: String, val label: String) {
    DASHBOARD("Dashboard", "Dashboard"),
    USERS("Users", "Users & Members"),
    ORDERS("Orders", "Orders"),
    CONTENT("Content", "Movies & Series"),
    REQUESTS_REPORTS("Requests", "Requests & Reports"),
    NOTIFICATIONS("Notifications", "Notifications"),
    ANALYTICS_INCOME("Analytics", "Analytics & Income"),
    SETTINGS_SYNC("Settings", "Settings & Sync")
}

class AdminViewModel(
    private val repo: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            MovizNowFirebase.verifyConnection()
            val current = repo.getCurrentFirebaseUser()
            if (current == null) {
                _uiState.update { it.copy(isCheckingAuth = false, currentAdmin = null) }
                return@launch
            }
            val profile = repo.fetchOrBuildAdminProfile(
                current.uid,
                current.email.orEmpty(),
                current.displayName.orEmpty()
            )
            if (profile != null) {
                _uiState.update { it.copy(isCheckingAuth = false, currentAdmin = profile) }
                loadAllAdminData()
            } else {
                repo.signOut()
                _uiState.update {
                    it.copy(
                        isCheckingAuth = false,
                        currentAdmin = null,
                        authError = "Account lacks admin access."
                    )
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authError = "Enter email and password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, authError = null) }
            val res = repo.signInWithEmail(email, password)
            res.onSuccess { adminProfile ->
                _uiState.update {
                    it.copy(isLoggingIn = false, currentAdmin = adminProfile, authError = null)
                }
                loadAllAdminData()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isLoggingIn = false, authError = err.message ?: "Sign in failed")
                }
            }
        }
    }

    fun signOut() {
        repo.signOut()
        _uiState.value = AdminUiState(isCheckingAuth = false)
    }

    fun allowedDestinations(): List<AdminNavDestination> {
        val role = _uiState.value.currentAdmin?.role ?: return emptyList()
        return when (role) {
            "content_manager" -> listOf(AdminNavDestination.DASHBOARD, AdminNavDestination.CONTENT)
            "user_manager" -> listOf(AdminNavDestination.DASHBOARD, AdminNavDestination.USERS)
            "manager" -> listOf(
                AdminNavDestination.DASHBOARD,
                AdminNavDestination.USERS,
                AdminNavDestination.CONTENT
            )
            "owner" -> AdminNavDestination.entries
            else -> AdminNavDestination.entries.filter { it != AdminNavDestination.SETTINGS_SYNC }
        }
    }

    fun loadAllAdminData(isManualRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (isManualRefresh) it.copy(isRefreshing = true) else it.copy(isLoading = true)
            }
            try {
                val usersDef = async { runCatching { repo.fetchAllUsers() }.getOrDefault(emptyList()) }
                val contentDef = async { runCatching { repo.fetchAllContent() }.getOrDefault(emptyList()) }
                val genresDef = async { runCatching { repo.fetchGenres() }.getOrDefault(emptyList()) }
                val langsDef = async { runCatching { repo.fetchLanguages() }.getOrDefault(emptyList()) }
                val qualsDef = async { runCatching { repo.fetchQualities() }.getOrDefault(emptyList()) }
                val collsDef = async { runCatching { repo.fetchCollections() }.getOrDefault(emptyList()) }
                val incomeDef = async { runCatching { repo.fetchIncomeRecords() }.getOrDefault(emptyList()) }
                val notifHistDef = async { runCatching { repo.fetchNotificationHistory() }.getOrDefault(emptyList()) }
                val notifTplDef = async { runCatching { repo.fetchNotificationTemplates() }.getOrDefault(emptyList()) }
                val settingsDef = async { runCatching { repo.fetchAppSettings() }.getOrNull() }

                val users = usersDef.await()
                val content = contentDef.await()
                val genres = genresDef.await()
                val langs = langsDef.await()
                val quals = qualsDef.await()
                val colls = collsDef.await()
                val incomes = incomeDef.await()
                val notifHistory = notifHistDef.await()
                val notifTemplates = notifTplDef.await()
                val settingsPair = settingsDef.await()

                val derivedOrders = users.flatMap { it.orders }
                    .sortedByDescending { it.createdAt }
                val derivedRequests = aggregateMovieRequests(users)
                val derivedReports = users.flatMap { u ->
                    u.reportedLinks.map { r ->
                        val matchedContent = content.find { it.id == r.contentId }
                        if (matchedContent != null) r.copy(contentTitle = matchedContent.title) else r
                    }
                }.sortedByDescending { it.createdAt }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        users = users,
                        contentList = content,
                        genres = genres,
                        languages = langs,
                        qualities = quals,
                        collections = colls,
                        orders = derivedOrders,
                        movieRequests = derivedRequests,
                        reportedLinks = derivedReports,
                        incomeRecords = incomes,
                        notificationHistory = notifHistory,
                        notificationTemplates = notifTemplates,
                        appSettings = settingsPair?.first ?: state.appSettings,
                        rawSettingsData = settingsPair?.second ?: state.rawSettingsData
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                showToast("Refresh error: ${e.message}")
            }
        }
    }

    private fun aggregateMovieRequests(users: List<UserProfile>): List<MovieRequestRecord> {
        val map = linkedMapOf<String, MovieRequestRecord>()
        users.forEach { u ->
            u.movieRequests.forEach { req ->
                val key = "${req.title.lowercase(Locale.US).trim()}-${req.type}"
                val existing = map[key]
                if (existing == null) {
                    map[key] = req.copy(id = key, requestedBy = listOf(u.uid), requestCount = 1)
                } else {
                    map[key] = existing.copy(
                        requestedBy = (existing.requestedBy + u.uid).distinct(),
                        requestCount = existing.requestCount + 1
                    )
                }
            }
        }
        return map.values.sortedByDescending { it.requestCount }
    }

    // ==================== USER MANAGEMENT ====================

    fun queueUserUpdate(uid: String, fields: Map<String, Any?>) {
        _uiState.update { state ->
            val updatedUsers = state.users.map { u ->
                if (u.uid != uid) u else applyLocalFields(u, fields)
            }
            val currentPending = state.pendingUserUpdates.toMutableMap()
            val merged = (currentPending[uid] ?: emptyMap()).toMutableMap()
            merged.putAll(fields)
            currentPending[uid] = merged
            state.copy(users = updatedUsers, pendingUserUpdates = currentPending)
        }
        showToast("Staged change for sync")
    }

    private fun applyLocalFields(u: UserProfile, fields: Map<String, Any?>): UserProfile {
        @Suppress("UNCHECKED_CAST")
        val updated = u.copy(
            role = fields["role"]?.toString() ?: u.role,
            status = fields["status"]?.toString() ?: u.status,
            expiryDate = if (fields.containsKey("expiryDate")) {
                val raw = fields["expiryDate"]?.toString()
                if (raw == "__DELETE_FIELD__") null else raw
            } else u.expiryDate,
            conshare = (fields["conshare"] as? Boolean) ?: u.conshare,
            displayName = fields["displayName"]?.toString() ?: u.displayName,
            phone = if (fields.containsKey("phone")) fields["phone"]?.toString() else u.phone,
            city = if (fields.containsKey("city")) fields["city"]?.toString() else u.city,
            assignedContent = (fields["assignedContent"] as? List<String>) ?: u.assignedContent,
            isUserManager = (fields["isUserManager"] as? Boolean) ?: u.isUserManager
        )
        return ChunkCodec.normalizeUser(updated)
    }

    fun syncPendingUserChanges() {
        val pending = _uiState.value.pendingUserUpdates
        if (pending.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingUsers = true) }
            try {
                repo.commitPendingUserUpdates(pending)
                _uiState.update { it.copy(isSyncingUsers = false, pendingUserUpdates = emptyMap()) }
                showToast("Synced ${pending.size} user update(s) to Firestore")
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncingUsers = false) }
                showToast("Sync failed: ${e.message}")
            }
        }
    }

    fun toggleUserWhitelist(user: UserProfile) {
        val phone = user.phone
        if (phone.isNullOrBlank()) {
            showToast("User has no phone number to whitelist")
            return
        }
        val targetState = !user.isWhitelisted
        viewModelScope.launch {
            try {
                repo.toggleWhitelistPhone(phone, user.uid, targetState)
                _uiState.update { state ->
                    state.copy(
                        users = state.users.map {
                            if (it.uid == user.uid) it.copy(isWhitelisted = targetState) else it
                        }
                    )
                }
                showToast(if (targetState) "Phone whitelisted" else "Removed from whitelist")
            } catch (e: Exception) {
                showToast("Whitelist update failed: ${e.message}")
            }
        }
    }

    fun deleteUser(uid: String) {
        viewModelScope.launch {
            try {
                repo.deleteUserImmediate(uid)
                _uiState.update { state ->
                    val nextPending = state.pendingUserUpdates.toMutableMap().apply { remove(uid) }
                    state.copy(
                        users = state.users.filterNot { it.uid == uid },
                        pendingUserUpdates = nextPending
                    )
                }
                showToast("User deleted")
            } catch (e: Exception) {
                showToast("Delete failed: ${e.message}")
            }
        }
    }

    fun notifyUserMembershipEmail(user: UserProfile) {
        if (user.email.isBlank() || user.email.endsWith("@moviznow.com")) {
            showToast("No valid Gmail/email on this account")
            return
        }
        viewModelScope.launch {
            val res = ApiClient.sendUserStatusEmail(
                userId = user.uid,
                email = user.email,
                displayName = user.effectiveName,
                status = user.status,
                role = user.role,
                expiryDate = user.expiryDate
            )
            res.onSuccess {
                showToast("Status email sent to ${user.email}")
            }.onFailure {
                showToast("Email failed: ${it.message}")
            }
        }
    }

    // ==================== ORDERS ====================

    fun approveOrder(order: OrderRecord) {
        viewModelScope.launch {
            try {
                repo.updateOrderStatusOnUser(order.userId, order.id, "approved", "Android Admin")
                showToast("Order #${order.id.takeLast(6)} approved")
                loadAllAdminData(isManualRefresh = true)
            } catch (e: Exception) {
                showToast("Approve failed: ${e.message}")
            }
        }
    }

    fun declineOrder(order: OrderRecord) {
        viewModelScope.launch {
            try {
                repo.updateOrderStatusOnUser(order.userId, order.id, "declined", "Android Admin")
                showToast("Order #${order.id.takeLast(6)} declined")
                loadAllAdminData(isManualRefresh = true)
            } catch (e: Exception) {
                showToast("Decline failed: ${e.message}")
            }
        }
    }

    fun verifyOrderWithAi(order: OrderRecord) {
        viewModelScope.launch {
            showToast("Running Gemini AI verification...")
            val res = ApiClient.verifyOrderWithAi(order.userId, order.id)
            res.onSuccess { json ->
                val verified = json.optBoolean("verified", false) || json.optBoolean("approved", false)
                val reason = json.optString("reason").ifBlank { json.optString("message") }
                showToast(if (verified) "AI Approved: $reason" else "AI Result: $reason")
                loadAllAdminData(isManualRefresh = true)
            }.onFailure {
                showToast("AI Verification error: ${it.message}")
            }
        }
    }

    // ==================== CONTENT & TAXONOMY ====================

    fun saveContent(item: ContentItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val saved = repo.saveContentItem(item)
                _uiState.update { state ->
                    val exists = state.contentList.any { it.id == saved.id }
                    val updated = if (exists) {
                        state.contentList.map { if (it.id == saved.id) saved else it }
                    } else {
                        listOf(saved) + state.contentList
                    }
                    state.copy(contentList = updated)
                }
                showToast("${saved.title} saved to Firestore")
                onDone()
            } catch (e: Exception) {
                showToast("Save content error: ${e.message}")
            }
        }
    }

    fun deleteContent(item: ContentItem) {
        viewModelScope.launch {
            try {
                repo.deleteContentItem(item)
                _uiState.update { state ->
                    state.copy(contentList = state.contentList.filterNot { it.id == item.id })
                }
                showToast("Deleted ${item.title}")
            } catch (e: Exception) {
                showToast("Delete failed: ${e.message}")
            }
        }
    }

    fun searchTmdb(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingTmdb = true) }
            val res = ApiClient.searchTmdb(query)
            res.onSuccess { list ->
                _uiState.update { it.copy(isSearchingTmdb = false, tmdbResults = list) }
            }.onFailure { err ->
                _uiState.update { it.copy(isSearchingTmdb = false) }
                showToast("TMDB search error: ${err.message}")
            }
        }
    }

    fun clearTmdbResults() {
        _uiState.update { it.copy(tmdbResults = emptyList()) }
    }

    fun saveGenre(id: String?, name: String) {
        viewModelScope.launch {
            try {
                val order = _uiState.value.genres.size + 1
                repo.saveGenre(id, name, order)
                _uiState.update { it.copy(genres = repo.fetchGenres()) }
                showToast("Genre saved")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    fun deleteGenre(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteGenre(id)
                _uiState.update { it.copy(genres = it.genres.filterNot { g -> g.id == id }) }
                showToast("Genre deleted")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    fun saveLanguage(id: String?, name: String) {
        viewModelScope.launch {
            try {
                val order = _uiState.value.languages.size + 1
                repo.saveLanguage(id, name, order)
                _uiState.update { it.copy(languages = repo.fetchLanguages()) }
                showToast("Language saved")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    fun deleteLanguage(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteLanguage(id)
                _uiState.update { it.copy(languages = it.languages.filterNot { l -> l.id == id }) }
                showToast("Language deleted")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    fun saveQuality(id: String?, name: String, color: String?) {
        viewModelScope.launch {
            try {
                val order = _uiState.value.qualities.size + 1
                repo.saveQuality(id, name, color, order)
                _uiState.update { it.copy(qualities = repo.fetchQualities()) }
                showToast("Quality saved")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    fun deleteQuality(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteQuality(id)
                _uiState.update { it.copy(qualities = it.qualities.filterNot { q -> q.id == id }) }
                showToast("Quality deleted")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    fun saveCollection(col: AppCollection) {
        viewModelScope.launch {
            try {
                repo.saveCollection(col)
                _uiState.update { it.copy(collections = repo.fetchCollections()) }
                showToast("Collection saved")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    fun deleteCollection(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteCollection(id)
                _uiState.update { it.copy(collections = it.collections.filterNot { c -> c.id == id }) }
                showToast("Collection deleted")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    // ==================== REQUESTS & REPORTED LINKS ====================

    fun updateMovieRequestStatus(request: MovieRequestRecord, newStatus: String, adminComment: String? = null) {
        viewModelScope.launch {
            try {
                val db = MovizNowFirebase.db
                val targetTitle = request.title.lowercase(Locale.US).trim()
                val batch = db.batch()
                request.requestedBy.forEach { uid ->
                    val u = _uiState.value.users.find { it.uid == uid } ?: return@forEach
                    val updatedList = u.movieRequests.map { r ->
                        if (r.title.lowercase(Locale.US).trim() == targetTitle && r.type == request.type) {
                            buildMap<String, Any> {
                                put("title", r.title)
                                put("type", r.type)
                                put("status", newStatus)
                                put("createdAt", r.createdAt)
                                r.year?.let { put("year", it) }
                                adminComment?.let { put("adminComment", it) }
                            }
                        } else {
                            buildMap<String, Any> {
                                put("title", r.title)
                                put("type", r.type)
                                put("status", r.status)
                                put("createdAt", r.createdAt)
                                r.year?.let { put("year", it) }
                                r.adminComment?.let { put("adminComment", it) }
                            }
                        }
                    }
                    batch.update(db.collection("users").document(uid), "movieRequests", updatedList)
                }
                batch.commit().await()
                showToast("Request marked $newStatus")
                loadAllAdminData(isManualRefresh = true)
            } catch (e: Exception) {
                showToast("Update request failed: ${e.message}")
            }
        }
    }

    fun resolveReportedLink(report: ReportedLinkRecord) {
        viewModelScope.launch {
            try {
                val db = MovizNowFirebase.db
                val user = _uiState.value.users.find { it.uid == report.userId } ?: return@launch
                val updated = user.reportedLinks.map { r ->
                    buildMap<String, Any> {
                        put("id", r.id)
                        put("contentId", r.contentId)
                        put("contentTitle", r.contentTitle)
                        put("contentType", r.contentType)
                        put("linkId", r.linkId)
                        put("linkName", r.linkName)
                        put("linkUrl", r.linkUrl)
                        put("status", if (r.id == report.id) "resolved" else r.status)
                        put("createdAt", r.createdAt)
                    }
                }
                db.collection("users").document(user.uid).update("reported_links", updated).await()
                showToast("Report marked resolved")
                loadAllAdminData(isManualRefresh = true)
            } catch (e: Exception) {
                showToast("Failed: ${e.message}")
            }
        }
    }

    fun checkLinkHealth(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(linkCheckOutput = "Checking $url...") }
            val res = ApiClient.checkLinkHealth(url)
            res.onSuccess { json ->
                val ok = json.optBoolean("ok", false) || json.optBoolean("working", false)
                val status = json.optInt("status", 0)
                val msg = json.optString("message").ifBlank { json.optString("detail") }
                _uiState.update {
                    it.copy(linkCheckOutput = "${if (ok) "WORKING" else "ERROR"} (HTTP $status) $msg")
                }
            }.onFailure { err ->
                _uiState.update { it.copy(linkCheckOutput = "Check failed: ${err.message}") }
            }
        }
    }

    // ==================== NOTIFICATIONS ====================

    fun sendNotification(
        title: String,
        body: String,
        targetType: String,
        targetUserIds: List<String>,
        buttonLabel: String?,
        buttonUrl: String?,
        sendFcm: Boolean,
        sendEmail: Boolean
    ) {
        if (title.isBlank() || body.isBlank()) {
            showToast("Title and message body are required")
            return
        }
        viewModelScope.launch {
            val res = ApiClient.sendNotification(
                title = title,
                body = body,
                targetType = targetType,
                targetUserIds = targetUserIds,
                buttonLabel = buttonLabel,
                buttonUrl = buttonUrl,
                sendFcm = sendFcm,
                sendEmail = sendEmail
            )
            res.onSuccess {
                showToast("Notification dispatched")
                loadAllAdminData(isManualRefresh = true)
            }.onFailure { err ->
                showToast("Send failed: ${err.message}")
            }
        }
    }

    fun runExpiryCheck() {
        viewModelScope.launch {
            showToast("Running automated expiry check...")
            val res = ApiClient.runExpiryCheck()
            res.onSuccess { json ->
                val sent = json.optInt("sentCount", json.optInt("notified", 0))
                showToast("Expiry check complete ($sent notified)")
            }.onFailure { err ->
                showToast("Expiry check failed: ${err.message}")
            }
        }
    }

    fun saveTemplate(name: String, title: String, body: String, buttonLabel: String?, buttonUrl: String?) {
        viewModelScope.launch {
            try {
                repo.saveNotificationTemplate(
                    NotificationTemplate(
                        id = "tpl_${System.currentTimeMillis()}",
                        name = name,
                        title = title,
                        body = body,
                        buttonLabel = buttonLabel,
                        buttonUrl = buttonUrl,
                        createdAt = ChunkCodec.getNowUtcIso()
                    )
                )
                _uiState.update { it.copy(notificationTemplates = repo.fetchNotificationTemplates()) }
                showToast("Template saved")
            } catch (e: Exception) {
                showToast("Template error: ${e.message}")
            }
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteNotificationTemplate(id)
                _uiState.update { state ->
                    state.copy(notificationTemplates = state.notificationTemplates.filterNot { it.id == id })
                }
                showToast("Template removed")
            } catch (e: Exception) {
                showToast("Delete failed: ${e.message}")
            }
        }
    }

    // ==================== INCOME ====================

    fun addIncomeRecord(amount: Double, description: String, userName: String, dateIso: String) {
        viewModelScope.launch {
            try {
                val newRec = IncomeRecord(
                    id = "inc_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                    amount = amount,
                    description = description,
                    userName = userName.ifBlank { "Anonymous" },
                    date = dateIso
                )
                val updated = (listOf(newRec) + _uiState.value.incomeRecords).sortedByDescending { it.date }
                repo.saveIncomeRecords(updated)
                _uiState.update { it.copy(incomeRecords = updated) }
                showToast("Income recorded")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    fun deleteIncomeRecord(id: String) {
        viewModelScope.launch {
            try {
                val updated = _uiState.value.incomeRecords.filterNot { it.id == id }
                repo.saveIncomeRecords(updated)
                _uiState.update { it.copy(incomeRecords = updated) }
                showToast("Income entry deleted")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    // ==================== SETTINGS & DB SYNC ====================

    fun saveAppSettings(updated: AppSettings) {
        viewModelScope.launch {
            try {
                repo.saveAppSettings(updated)
                _uiState.update { it.copy(appSettings = updated) }
                showToast("App settings saved to Firestore")
            } catch (e: Exception) {
                showToast("Save settings failed: ${e.message}")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun checkDatabaseSyncStatus(targetIndex: Int = 0) {
        val serviceAccounts = _uiState.value.rawSettingsData["serviceAccounts"] as? Map<String, Any?> ?: emptyMap()
        val sourceKey = serviceAccounts["sourceKey"]?.toString().orEmpty()
        val targets = serviceAccounts["targets"] as? List<Map<String, Any?>> ?: emptyList()
        val target = targets.getOrNull(targetIndex)
        val targetKey = target?.get("key")?.toString().orEmpty()
        val targetDbId = target?.get("databaseId")?.toString().orEmpty()

        viewModelScope.launch {
            _uiState.update { it.copy(isRunningSync = true) }
            val res = ApiClient.checkSyncStatus(sourceKey, targetKey, targetDbId)
            res.onSuccess { status ->
                _uiState.update {
                    it.copy(
                        isRunningSync = false,
                        syncStatus = status,
                        syncLogs = listOf("Status: src=${status.sourceConnected}, tgt=${status.targetConnected}") + it.syncLogs
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isRunningSync = false,
                        syncLogs = listOf("Status check failed: ${err.message}") + it.syncLogs
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun pushDatabaseSync(mode: String = "changed", targetIndex: Int = 0) {
        val serviceAccounts = _uiState.value.rawSettingsData["serviceAccounts"] as? Map<String, Any?> ?: emptyMap()
        val sourceKey = serviceAccounts["sourceKey"]?.toString().orEmpty()
        val targets = serviceAccounts["targets"] as? List<Map<String, Any?>> ?: emptyList()
        val target = targets.getOrNull(targetIndex) ?: run {
            showToast("No target DB configured in Admin Settings")
            return
        }
        val targetKey = target["key"]?.toString().orEmpty()
        val targetDbId = target["databaseId"]?.toString().orEmpty()

        viewModelScope.launch {
            _uiState.update { it.copy(isRunningSync = true) }
            val res = ApiClient.pushSync(sourceKey, targetKey, targetDbId, mode)
            res.onSuccess { json ->
                val logsArr = json.optJSONArray("logs")
                val serverLogs = mutableListOf<String>()
                if (logsArr != null) {
                    for (i in 0 until logsArr.length()) {
                        serverLogs.add(logsArr.optString(i))
                    }
                }
                _uiState.update {
                    it.copy(
                        isRunningSync = false,
                        syncLogs = (serverLogs.ifEmpty { listOf("Push ($mode) completed") }) + it.syncLogs
                    )
                }
                showToast("Database push ($mode) finished")
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isRunningSync = false,
                        syncLogs = listOf("Push error: ${err.message}") + it.syncLogs
                    )
                }
                showToast("Sync push failed: ${err.message}")
            }
        }
    }
}

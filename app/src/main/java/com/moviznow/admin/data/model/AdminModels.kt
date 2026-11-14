package com.moviznow.admin.data.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoURL: String? = null,
    val role: String = "user",
    val status: String = "pending",
    val phone: String? = null,
    val city: String? = null,
    val expiryDate: String? = null,
    val assignedContent: List<String> = emptyList(),
    val watchLater: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val clickHistory: List<ClickHistoryEntry> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String? = null,
    val lastActive: String? = null,
    val sessionsCount: Int = 0,
    val timeSpent: Long = 0L,
    val fcmToken: String? = null,
    val notification: String? = null,
    val emailNotificationsEnabled: Boolean = true,
    val isFcmDisabled: Boolean = false,
    val web: Boolean = false,
    val android: Boolean = false,
    val conshare: Boolean = false,
    val isUserManager: Boolean = false,
    val managedBy: String? = null,
    val deviceOs: String? = null,
    val deviceModel: String? = null,
    val orders: List<OrderRecord> = emptyList(),
    val movieRequests: List<MovieRequestRecord> = emptyList(),
    val reportedLinks: List<ReportedLinkRecord> = emptyList(),
    val isWhitelisted: Boolean = false
) {
    val effectiveName: String
        get() = displayName.takeIf { it.isNotBlank() && it != "No Name" && it != "null" }
            ?: email.takeIf { it.isNotBlank() && !it.endsWith("@moviznow.com") }
            ?: phone?.takeIf { it.isNotBlank() }
            ?: "User"

    val isAndroidUser: Boolean
        get() = android || deviceOs.equals("android", ignoreCase = true)

    val fcmStatus: FcmStatus
        get() = when {
            !fcmToken.isNullOrBlank() && notification != "no" && !isFcmDisabled -> FcmStatus.ACTIVE
            !fcmToken.isNullOrBlank() -> FcmStatus.DISABLED_IN_SETTINGS
            notification == "no" -> FcmStatus.OPTED_OUT
            else -> FcmStatus.NOT_REGISTERED
        }
}

enum class FcmStatus {
    ACTIVE,
    DISABLED_IN_SETTINGS,
    OPTED_OUT,
    NOT_REGISTERED
}

data class ClickHistoryEntry(
    val id: String = "unknown",
    val label: String = "",
    val title: String = "",
    val clicks: Int = 1
)

data class CartItem(
    val contentId: String = "",
    val title: String = "",
    val type: String = "movie",
    val seasonId: String? = null,
    val seasonNumber: Int? = null,
    val price: Double = 0.0
)

data class OrderRecord(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userRole: String = "user",
    val type: String = "membership", // membership | content
    val amount: Double = 0.0,
    val status: String = "pending", // pending | approved | declined | cancelled
    val createdAt: String = "",
    val months: Int = 1,
    val planName: String? = null,
    val planRole: String? = null,
    val items: List<CartItem> = emptyList(),
    val trxId: String? = null,
    val accountTitle: String? = null,
    val accountNumberLast4: String? = null,
    val paymentDateTime: String? = null,
    val paymentScreenshotUrl: String? = null,
    val senderBank: String? = null,
    val paymentMethodId: String? = null,
    val paymentMethodName: String? = null,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val aiVerificationReason: String? = null,
    val aiConfidence: String? = null,
    val allowAutoApproval: Boolean = true
)

data class MovieRequestRecord(
    val id: String = "",
    val title: String = "",
    val type: String = "movie", // movie | series
    val year: String? = null,
    val status: String = "pending", // pending | added | rejected
    val createdAt: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val requestedBy: List<String> = emptyList(),
    val requestCount: Int = 1,
    val linkedContentId: String? = null,
    val adminComment: String? = null
)

data class ReportedLinkRecord(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val contentId: String = "",
    val contentTitle: String = "",
    val contentType: String = "movie",
    val linkId: String = "",
    val linkName: String = "",
    val linkUrl: String = "",
    val reason: String? = null,
    val status: String = "pending", // pending | resolved
    val createdAt: String = ""
)

data class LinkDef(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val size: String = "",
    val unit: String = "MB"
)

data class Episode(
    val id: String = "",
    val episodeNumber: Int = 1,
    val title: String = "",
    val description: String = "",
    val duration: String = "",
    val links: List<LinkDef> = emptyList()
)

data class Season(
    val id: String = "",
    val seasonNumber: Int = 1,
    val title: String = "",
    val year: Int? = null,
    val trailerUrl: String = "",
    val isFullSeasonMkv: Boolean = false,
    val folderLink: String = "",
    val zipLinks: List<LinkDef> = emptyList(),
    val mkvLinks: List<LinkDef> = emptyList(),
    val episodes: List<Episode> = emptyList()
)

data class ContentItem(
    val id: String = "",
    val type: String = "movie", // movie | series
    val title: String = "",
    val secondTitle: String? = null,
    val description: String = "",
    val posterUrl: String = "",
    val backdropUrl: String? = null,
    val trailerUrl: String = "",
    val imdbLink: String? = null,
    val imdbRating: String? = null,
    val genreIds: List<String> = emptyList(),
    val languageIds: List<String> = emptyList(),
    val qualityId: String? = null,
    val cast: List<String> = emptyList(),
    val year: Int = 2026,
    val releaseDate: String? = null,
    val runtime: String? = null,
    val country: String? = null,
    val ottPlatform: String? = null,
    val subtitles: Boolean = false,
    val status: String = "published", // published | draft | selected_content
    val isUpcoming: Boolean = false,
    val order: Int = 0,
    val chunkId: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val addedBy: String? = null,
    val movieLinks: List<LinkDef> = emptyList(),
    val seasons: List<Season> = emptyList()
)

data class Genre(
    val id: String = "",
    val name: String = "",
    val order: Int = 0
)

data class Language(
    val id: String = "",
    val name: String = "",
    val order: Int = 0
)

data class Quality(
    val id: String = "",
    val name: String = "",
    val color: String? = null,
    val order: Int = 0
)

data class AppCollection(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val contentIds: List<String> = emptyList(),
    val order: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class IncomeRecord(
    val id: String = "",
    val userId: String? = null,
    val userName: String = "Anonymous",
    val amount: Double = 0.0,
    val description: String = "",
    val date: String = ""
)

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val contentId: String? = null,
    val posterUrl: String? = null,
    val type: String = "custom",
    val targetAudience: String = "all",
    val targetUserIds: List<String> = emptyList(),
    val targetUserNames: List<String> = emptyList(),
    val buttonLabel: String? = null,
    val buttonUrl: String? = null,
    val createdAt: String = "",
    val createdBy: String = ""
)

data class NotificationTemplate(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val body: String = "",
    val buttonLabel: String? = null,
    val buttonUrl: String? = null,
    val createdAt: String = ""
)

data class BankAccount(
    val id: String = "",
    val name: String = "",
    val accountNumber: String = "",
    val accountTitle: String = "",
    val color: String = "#10B981",
    val allowAutoApproval: Boolean = true
)

data class AppSettings(
    val headerText: String = "MovizNow",
    val membershipFee: Double = 200.0,
    val movieFee: Double = 50.0,
    val seasonFee: Double = 100.0,
    val supportNumber: String = "3416286423",
    val accountTitle: String = "Asmat Ullah",
    val accountNumber: String = "03416286423",
    val defaultAppImage: String = "",
    val paymentDetails: String = "",
    val itemsPerPage: Int = 20,
    val isTrialEnabled: Boolean = true,
    val isPhoneLoginEnabled: Boolean = true,
    val isAdminContactEnabled: Boolean = true,
    val isPaymentEnabled: Boolean = true,
    val isMaintenanceModeEnabled: Boolean = false,
    val maintenanceMessage: String = "App is currently under maintenance. Please try again later.",
    val whatsappChannelLink: String = "",
    val whatsappClipsLink: String = "",
    val bankAccounts: List<BankAccount> = emptyList(),
    val hiddenAdminTabs: List<String> = emptyList()
)

data class TmdbSearchResult(
    val id: Int,
    val title: String,
    val type: String,
    val year: Int,
    val overview: String,
    val posterUrl: String,
    val voteAverage: String,
    val releaseDate: String
)

data class SyncStatusResult(
    val sourceConnected: Boolean = false,
    val targetConnected: Boolean = false,
    val sourceKeyExists: Boolean = false,
    val targetKeyExists: Boolean = false,
    val message: String = ""
)

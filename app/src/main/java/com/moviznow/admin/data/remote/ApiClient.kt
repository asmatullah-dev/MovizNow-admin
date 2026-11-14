package com.moviznow.admin.data.remote

import com.moviznow.admin.BuildConfig
import com.moviznow.admin.data.model.SyncStatusResult
import com.moviznow.admin.data.model.TmdbSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ApiClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')

    fun setBaseUrl(url: String) {
        if (url.isNotBlank()) {
            baseUrl = url.trim().trimEnd('/')
        }
    }

    fun getBaseUrl(): String = baseUrl

    suspend fun postJson(path: String, body: JSONObject): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = if (path.startsWith("http")) path else "$baseUrl$path"
            val req = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody(jsonMediaType))
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                val json = try {
                    if (raw.trim().startsWith("{")) JSONObject(raw)
                    else JSONObject().put("raw", raw)
                } catch (_: Exception) {
                    JSONObject().put("raw", raw)
                }
                if (!res.isSuccessful) {
                    val errMsg = json.optString("error").ifBlank { "HTTP ${res.code}" }
                    Result.failure(Exception(errMsg))
                } else {
                    Result.success(json)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getJson(path: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = if (path.startsWith("http")) path else "$baseUrl$path"
            val req = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                val json = try {
                    if (raw.trim().startsWith("{")) JSONObject(raw)
                    else JSONObject().put("raw", raw)
                } catch (_: Exception) {
                    JSONObject().put("raw", raw)
                }
                if (!res.isSuccessful) {
                    val errMsg = json.optString("error").ifBlank { "HTTP ${res.code}" }
                    Result.failure(Exception(errMsg))
                } else {
                    Result.success(json)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendNotification(
        title: String,
        body: String,
        targetType: String,
        targetUserIds: List<String>,
        buttonLabel: String?,
        buttonUrl: String?,
        sendFcm: Boolean,
        sendEmail: Boolean
    ): Result<JSONObject> {
        val payload = JSONObject().apply {
            put("title", title)
            put("body", body)
            put("targetType", targetType)
            if (targetUserIds.isNotEmpty()) {
                put("targetUserIds", JSONArray(targetUserIds))
            }
            if (!buttonLabel.isNullOrBlank()) put("buttonLabel", buttonLabel)
            if (!buttonUrl.isNullOrBlank()) put("buttonUrl", buttonUrl)
            put("sendFcm", sendFcm)
            put("sendEmail", sendEmail)
        }
        return postJson("/api/notifications/send", payload)
    }

    suspend fun runExpiryCheck(): Result<JSONObject> =
        postJson("/api/notifications/check-expiry", JSONObject())

    suspend fun notifyOrderApproved(
        userId: String,
        orderId: String,
        planName: String?,
        amount: Double
    ): Result<JSONObject> {
        val payload = JSONObject().apply {
            put("userId", userId)
            put("orderId", orderId)
            if (!planName.isNullOrBlank()) put("planName", planName)
            put("amount", amount)
        }
        return postJson("/api/notifications/notify-order-approved", payload)
    }

    suspend fun notifyMembershipUpdate(
        userId: String,
        role: String,
        status: String,
        expiryDate: String?
    ): Result<JSONObject> {
        val payload = JSONObject().apply {
            put("userId", userId)
            put("role", role)
            put("status", status)
            if (!expiryDate.isNullOrBlank()) put("expiryDate", expiryDate)
        }
        return postJson("/api/notifications/notify-membership-update", payload)
    }

    suspend fun sendUserStatusEmail(
        userId: String,
        email: String,
        displayName: String,
        status: String,
        role: String,
        expiryDate: String?
    ): Result<JSONObject> {
        val payload = JSONObject().apply {
            put("userId", userId)
            put("email", email)
            put("displayName", displayName)
            put("status", status)
            put("role", role)
            if (!expiryDate.isNullOrBlank()) put("expiryDate", expiryDate)
        }
        return postJson("/api/notifications/send-user-status-email", payload)
    }

    suspend fun verifyOrderWithAi(userId: String, orderId: String): Result<JSONObject> {
        val payload = JSONObject().apply {
            put("userId", userId)
            put("orderId", orderId)
        }
        return postJson("/api/orders/admin-verify-order", payload)
    }

    suspend fun deleteUserFromServer(uid: String): Result<JSONObject> {
        val payload = JSONObject().apply { put("uid", uid) }
        return postJson("/api/admin/users/delete", payload)
    }

    suspend fun checkLinkHealth(url: String): Result<JSONObject> {
        val payload = JSONObject().apply { put("url", url) }
        return postJson("/api/check-link", payload)
    }

    suspend fun searchTmdb(query: String): Result<List<TmdbSearchResult>> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        return getJson("/api/tmdb/search/multi?query=$encoded&include_adult=false").map { json ->
            val arr = json.optJSONArray("results") ?: JSONArray()
            val out = mutableListOf<TmdbSearchResult>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val mediaType = item.optString("media_type")
                if (mediaType != "movie" && mediaType != "tv") continue
                val title = if (mediaType == "movie") item.optString("title") else item.optString("name")
                val date = if (mediaType == "movie") item.optString("release_date") else item.optString("first_air_date")
                val year = date.take(4).toIntOrNull() ?: 2026
                val posterPath = item.optString("poster_path")
                val posterUrl = if (posterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else ""
                val rating = String.format("%.1f", item.optDouble("vote_average", 0.0))
                out.add(
                    TmdbSearchResult(
                        id = item.optInt("id"),
                        title = title,
                        type = if (mediaType == "tv") "series" else "movie",
                        year = year,
                        overview = item.optString("overview"),
                        posterUrl = posterUrl,
                        voteAverage = rating,
                        releaseDate = date
                    )
                )
            }
            out
        }
    }

    suspend fun checkSyncStatus(sourceKey: String, targetKey: String, targetDbId: String): Result<SyncStatusResult> {
        val payload = JSONObject().apply {
            put("sourceKey", sourceKey)
            put("targetKey", targetKey)
            put("targetDbId", targetDbId)
        }
        return postJson("/api/sync/status", payload).map { json ->
            SyncStatusResult(
                sourceConnected = json.optBoolean("sourceConnected", false),
                targetConnected = json.optBoolean("targetConnected", false),
                sourceKeyExists = json.optBoolean("sourceKeyExists", false),
                targetKeyExists = json.optBoolean("targetKeyExists", false),
                message = json.optString("message", "")
            )
        }
    }

    suspend fun compareDatabases(sourceKey: String, targetKey: String, targetDbId: String): Result<JSONObject> {
        val payload = JSONObject().apply {
            put("sourceKey", sourceKey)
            put("targetKey", targetKey)
            put("targetDbId", targetDbId)
        }
        return postJson("/api/sync/compare", payload)
    }

    suspend fun pushSync(sourceKey: String, targetKey: String, targetDbId: String, mode: String): Result<JSONObject> {
        val payload = JSONObject().apply {
            put("sourceKey", sourceKey)
            put("targetKey", targetKey)
            put("targetDbId", targetDbId)
            put("mode", mode)
        }
        return postJson("/api/sync/push", payload)
    }
}

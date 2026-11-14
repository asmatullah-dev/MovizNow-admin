package com.moviznow.admin.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.moviznow.admin.data.firebase.ChunkCodec
import com.moviznow.admin.data.firebase.MovizNowFirebase
import com.moviznow.admin.data.model.AppCollection
import com.moviznow.admin.data.model.AppNotification
import com.moviznow.admin.data.model.AppSettings
import com.moviznow.admin.data.model.BankAccount
import com.moviznow.admin.data.model.CartItem
import com.moviznow.admin.data.model.ClickHistoryEntry
import com.moviznow.admin.data.model.ContentItem
import com.moviznow.admin.data.model.Genre
import com.moviznow.admin.data.model.IncomeRecord
import com.moviznow.admin.data.model.Language
import com.moviznow.admin.data.model.MovieRequestRecord
import com.moviznow.admin.data.model.NotificationTemplate
import com.moviznow.admin.data.model.OrderRecord
import com.moviznow.admin.data.model.Quality
import com.moviznow.admin.data.model.ReportedLinkRecord
import com.moviznow.admin.data.model.UserProfile
import com.moviznow.admin.data.remote.ApiClient
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

class AdminRepository {

    private val auth get() = MovizNowFirebase.auth
    private val db get() = MovizNowFirebase.db

    fun getCurrentFirebaseUser() = auth.currentUser

    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val fbUser = result.user ?: return Result.failure(Exception("Authentication failed"))
            val profile = fetchOrBuildAdminProfile(fbUser.uid, fbUser.email.orEmpty(), fbUser.displayName.orEmpty())
                ?: return Result.failure(Exception("Access Denied: Your account does not have admin privileges."))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun fetchOrBuildAdminProfile(uid: String, email: String, fallbackName: String): UserProfile? {
        val emailLower = email.lowercase(Locale.US)
        val snap = try {
            db.collection("users").document(uid).get().await()
        } catch (_: Exception) {
            null
        }
        val docUser = snap?.data?.let { parseUserMap(uid, it, emptySet()) }
        val resolved = docUser ?: UserProfile(
            uid = uid,
            email = email,
            displayName = fallbackName.ifBlank { email.substringBefore("@") },
            role = when {
                emailLower == MovizNowFirebase.OWNER_EMAIL -> "owner"
                MovizNowFirebase.ADMIN_EMAILS.contains(emailLower) -> "admin"
                else -> "user"
            },
            status = "active",
            expiryDate = "Lifetime"
        )
        val normalized = ChunkCodec.normalizeUser(resolved)
        val allowedRoles = setOf("owner", "admin", "manager", "user_manager", "content_manager")
        return if (allowedRoles.contains(normalized.role) || MovizNowFirebase.ADMIN_EMAILS.contains(emailLower)) {
            normalized
        } else {
            null
        }
    }

    suspend fun fetchWhitelistedPhones(): Set<String> {
        val snap = db.collection("settings").document("whitelisted_phones").get().await()
        val rawNumbers = snap.get("numbers") as? List<*> ?: return emptySet()
        return rawNumbers.mapNotNull { entry ->
            val str = when (entry) {
                is Map<*, *> -> (entry["number"] ?: entry["phone"])?.toString()
                else -> entry?.toString()
            }
            ChunkCodec.standardizePhone(str).takeIf { it.isNotBlank() }
        }.toSet()
    }

    suspend fun fetchAllUsers(): List<UserProfile> {
        val whitelistedPhones = try { fetchWhitelistedPhones() } catch (_: Exception) { emptySet() }
        val snap = db.collection("users").get().await()
        return snap.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            parseUserMap(doc.id, data, whitelistedPhones)
        }.sortedByDescending { it.lastActive ?: it.createdAt }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseUserMap(uid: String, data: Map<String, Any?>, whitelistedPhones: Set<String>): UserProfile {
        val rawPhone = (data["phone"] ?: data["phoneNumber"] ?: data["whatsapp"] ?: data["whatsappNumber"])?.toString()
        val stdPhone = ChunkCodec.standardizePhone(rawPhone)
        val deviceMap = data["device"] as? Map<*, *>

        val ordersList = (data["orders"] as? List<*>)?.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val rawAmt = m["amt"] ?: m["amount"]
            val rawItems = (m["items"] as? List<*>)?.mapNotNull { itm ->
                val cm = itm as? Map<*, *> ?: return@mapNotNull null
                CartItem(
                    contentId = (cm["contentId"] ?: cm["id"])?.toString().orEmpty(),
                    title = cm["title"]?.toString().orEmpty(),
                    type = cm["type"]?.toString() ?: "movie",
                    seasonId = cm["seasonId"]?.toString(),
                    seasonNumber = (cm["seasonNumber"] as? Number)?.toInt(),
                    price = (cm["price"] as? Number)?.toDouble() ?: 0.0
                )
            } ?: emptyList()

            OrderRecord(
                id = m["id"]?.toString().orEmpty(),
                userId = uid,
                userName = data["displayName"]?.toString().orEmpty(),
                userEmail = data["email"]?.toString().orEmpty(),
                userRole = data["role"]?.toString() ?: "user",
                type = m["type"]?.toString() ?: "membership",
                amount = (rawAmt as? Number)?.toDouble() ?: rawAmt?.toString()?.toDoubleOrNull() ?: 0.0,
                status = (m["status"] ?: m["st"])?.toString() ?: "pending",
                createdAt = (m["cat"] ?: m["createdAt"])?.toString().orEmpty(),
                months = ((m["m"] ?: m["months"]) as? Number)?.toInt() ?: 1,
                planName = (m["plan"] ?: m["planName"])?.toString(),
                planRole = (m["role"] ?: m["planRole"])?.toString(),
                items = rawItems,
                trxId = (m["trx"] ?: m["trxId"])?.toString(),
                accountTitle = (m["acc"] ?: m["accountTitle"])?.toString(),
                accountNumberLast4 = (m["acc4"] ?: m["accountNumberLast4"])?.toString(),
                paymentDateTime = (m["dt"] ?: m["pdt"] ?: m["paymentDateTime"])?.toString(),
                paymentScreenshotUrl = m["paymentScreenshotUrl"]?.toString(),
                senderBank = (m["bank"] ?: m["senderBank"])?.toString(),
                paymentMethodId = m["paymentMethodId"]?.toString(),
                paymentMethodName = m["paymentMethodName"]?.toString(),
                verifiedBy = (m["vb"] ?: m["verifiedBy"])?.toString(),
                verifiedAt = m["verifiedAt"]?.toString(),
                aiVerificationReason = m["aiVerificationReason"]?.toString(),
                aiConfidence = (m["aic"] ?: m["aiConfidence"])?.toString(),
                allowAutoApproval = m["allowAutoApproval"] != false
            )
        } ?: emptyList()

        val reqList = (data["movieRequests"] as? List<*>)?.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val title = m["title"]?.toString().orEmpty()
            val type = m["type"]?.toString() ?: "movie"
            MovieRequestRecord(
                id = "${title.lowercase(Locale.US).trim()}-$type",
                title = title,
                type = type,
                year = m["year"]?.toString(),
                status = m["status"]?.toString() ?: "pending",
                createdAt = m["createdAt"]?.toString().orEmpty(),
                userId = uid,
                userName = data["displayName"]?.toString() ?: "User",
                userEmail = data["email"]?.toString().orEmpty(),
                requestedBy = listOf(uid),
                requestCount = 1,
                linkedContentId = m["linkedContentId"]?.toString(),
                adminComment = m["adminComment"]?.toString()
            )
        } ?: emptyList()

        val reportedList = (data["reported_links"] as? List<*>)?.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            ReportedLinkRecord(
                id = m["id"]?.toString() ?: UUID.randomUUID().toString(),
                userId = uid,
                userName = data["displayName"]?.toString() ?: "User",
                contentId = m["contentId"]?.toString().orEmpty(),
                contentTitle = m["contentTitle"]?.toString().orEmpty(),
                contentType = m["contentType"]?.toString() ?: "movie",
                linkId = m["linkId"]?.toString().orEmpty(),
                linkName = m["linkName"]?.toString().orEmpty(),
                linkUrl = m["linkUrl"]?.toString().orEmpty(),
                reason = m["reason"]?.toString(),
                status = m["status"]?.toString() ?: "pending",
                createdAt = m["createdAt"]?.toString().orEmpty()
            )
        } ?: emptyList()

        val clickList = (data["clickHistory"] as? List<*>)?.mapNotNull { item ->
            when (item) {
                is String -> parseClickEntry("unknown", item)
                is Map<*, *> -> parseClickEntry(item["id"]?.toString() ?: "unknown", item["label"]?.toString().orEmpty())
                else -> null
            }
        } ?: emptyList()

        val rawUser = UserProfile(
            uid = data["uid"]?.toString()?.ifBlank { uid } ?: uid,
            email = data["email"]?.toString().orEmpty(),
            displayName = data["displayName"]?.toString().orEmpty(),
            photoURL = data["photoURL"]?.toString(),
            role = data["role"]?.toString() ?: "user",
            status = data["status"]?.toString() ?: "pending",
            phone = rawPhone,
            city = data["city"]?.toString(),
            expiryDate = data["expiryDate"]?.toString(),
            assignedContent = (data["assignedContent"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
            watchLater = (data["watchLater"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
            favorites = (data["favorites"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
            clickHistory = clickList,
            createdAt = data["createdAt"]?.toString().orEmpty(),
            updatedAt = data["updatedAt"]?.toString(),
            lastActive = data["lastActive"]?.toString(),
            sessionsCount = (data["sessionsCount"] as? Number)?.toInt() ?: 0,
            timeSpent = (data["timeSpent"] as? Number)?.toLong() ?: 0L,
            fcmToken = data["fcmToken"]?.toString(),
            notification = data["notification"]?.toString(),
            emailNotificationsEnabled = data["emailNotificationsEnabled"] != false && data["emailNotificationsDisabled"] != true,
            isFcmDisabled = data["isFcmDisabled"] == true,
            web = data["web"] == true,
            android = data["android"] == true,
            conshare = data["conshare"] == true,
            isUserManager = data["isUserManager"] == true,
            managedBy = data["managedBy"]?.toString(),
            deviceOs = deviceMap?.get("os")?.toString(),
            deviceModel = deviceMap?.get("model")?.toString(),
            orders = ordersList,
            movieRequests = reqList,
            reportedLinks = reportedList,
            isWhitelisted = stdPhone.isNotBlank() && whitelistedPhones.contains(stdPhone)
        )
        return ChunkCodec.normalizeUser(rawUser)
    }

    private fun parseClickEntry(id: String, label: String): ClickHistoryEntry? {
        if (label.isBlank()) return null
        val title = label.substringBefore(" - ").ifBlank { "Unknown" }
        val match = Regex("(\\d+)\\s*time").find(label)
        val count = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
        return ClickHistoryEntry(id = id, label = label, title = title, clicks = count)
    }

    suspend fun commitPendingUserUpdates(pending: Map<String, Map<String, Any?>>) {
        if (pending.isEmpty()) return
        val nowIso = ChunkCodec.getNowUtcIso()
        val metaUsersUpdate = mutableMapOf<String, Any>()
        var batch = db.batch()
        var opCount = 0

        for ((uid, rawFields) in pending) {
            if (opCount >= 450) {
                batch.commit().await()
                batch = db.batch()
                opCount = 0
            }
            if (rawFields["__DELETE_USER__"] == true) {
                batch.delete(db.collection("users").document(uid))
                metaUsersUpdate[uid] = -1
                opCount++
                continue
            }
            val writeMap = rawFields.toMutableMap()
            writeMap["uid"] = uid
            writeMap["updatedAt"] = nowIso
            writeMap["lastActive"] = writeMap["lastActive"] ?: nowIso
            for ((k, v) in rawFields) {
                if (v == "__DELETE_FIELD__") {
                    writeMap[k] = FieldValue.delete()
                }
            }
            batch.set(db.collection("users").document(uid), writeMap, SetOptions.merge())
            metaUsersUpdate[uid] = nowIso
            opCount++
        }

        batch.set(
            db.collection("chunk_meta").document("versions"),
            mapOf("users" to metaUsersUpdate, "users_version" to System.currentTimeMillis()),
            SetOptions.merge()
        )
        batch.commit().await()
    }

    suspend fun toggleWhitelistPhone(phone: String, uid: String?, whitelist: Boolean): Boolean {
        val std = ChunkCodec.standardizePhone(phone)
        if (std.isBlank()) return false
        val docRef = db.collection("settings").document("whitelisted_phones")
        val snap = docRef.get().await()
        val existing = (snap.get("numbers") as? List<*>)?.mapNotNull { it?.toString() }?.toMutableList() ?: mutableListOf()

        val filtered = existing.filterNot { entry ->
            val entryPhone = ChunkCodec.standardizePhone(entry)
            entryPhone == std
        }.toMutableList()

        if (whitelist) {
            val formatted = if (!uid.isNullOrBlank()) "$std:$uid" else std
            filtered.add(formatted)
        }

        docRef.set(
            mapOf("numbers" to filtered, "updatedAt" to ChunkCodec.getNowUtcIso()),
            SetOptions.merge()
        ).await()
        return whitelist
    }

    suspend fun deleteUserImmediate(uid: String) {
        val res = ApiClient.deleteUserFromServer(uid)
        if (res.isFailure) {
            db.collection("users").document(uid).delete().await()
        }
        db.collection("chunk_meta").document("versions").set(
            mapOf("users" to mapOf(uid to -1), "users_version" to System.currentTimeMillis()),
            SetOptions.merge()
        ).await()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun updateOrderStatusOnUser(
        userId: String,
        orderId: String,
        newStatus: String,
        verifiedBy: String = "Admin"
    ) {
        val userRef = db.collection("users").document(userId)
        val snap = userRef.get().await()
        val data = snap.data ?: throw Exception("User not found")
        val rawOrders = (data["orders"] as? List<*>)?.mapNotNull { (it as? Map<String, Any?>)?.toMutableMap() } ?: emptyList()

        var targetOrder: Map<String, Any?>? = null
        val updatedOrders = rawOrders.map { o ->
            if (o["id"]?.toString() == orderId) {
                o["status"] = newStatus
                o["st"] = newStatus
                if (newStatus == "approved") {
                    o["vb"] = verifiedBy
                    o["verifiedBy"] = verifiedBy
                    o["verifiedAt"] = ChunkCodec.getNowUtcIso()
                }
                targetOrder = o
                o
            } else o
        }

        val updates = mutableMapOf<String, Any>(
            "orders" to updatedOrders,
            "updatedAt" to ChunkCodec.getNowUtcIso()
        )

        if (newStatus == "approved" && targetOrder != null) {
            val orderType = targetOrder!!["type"]?.toString() ?: "membership"
            if (orderType == "membership") {
                val months = ((targetOrder!!["m"] ?: targetOrder!!["months"]) as? Number)?.toInt() ?: 1
                val curExpiry = data["expiryDate"]?.toString()
                val newExpiry = ChunkCodec.extendExpiryIso(curExpiry, months)
                val targetRole = (targetOrder!!["role"] ?: targetOrder!!["planRole"])?.toString()
                    ?.takeIf { it == "vip" || it == "basic" }
                    ?: data["role"]?.toString()?.takeIf { it == "vip" || it == "basic" }
                    ?: "vip"

                updates["expiryDate"] = newExpiry
                updates["status"] = "active"
                updates["role"] = targetRole
                updates["expiryNoticeSent"] = false
            } else if (orderType == "content") {
                val existingAssigned = (data["assignedContent"] as? List<*>)?.mapNotNull { it?.toString() }?.toMutableSet() ?: mutableSetOf()
                val items = targetOrder!!["items"] as? List<*> ?: emptyList<Any>()
                items.forEach { itm ->
                    val cm = itm as? Map<*, *>
                    val cid = (cm?.get("contentId") ?: cm?.get("id"))?.toString()
                    if (!cid.isNullOrBlank()) existingAssigned.add(cid)
                }
                updates["assignedContent"] = existingAssigned.toList()
            }
        }

        userRef.set(updates, SetOptions.merge()).await()
        db.collection("chunk_meta").document("versions").set(
            mapOf("users" to mapOf(userId to ChunkCodec.getNowUtcIso()), "users_version" to System.currentTimeMillis()),
            SetOptions.merge()
        ).await()

        if (newStatus == "approved") {
            val amt = ((targetOrder?.get("amt") ?: targetOrder?.get("amount")) as? Number)?.toDouble() ?: 0.0
            val plan = (targetOrder?.get("plan") ?: targetOrder?.get("planName"))?.toString()
            ApiClient.notifyOrderApproved(userId, orderId, plan, amt)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun fetchAllContent(): List<ContentItem> {
        val snap = db.collection("content_chunks").get().await()
        val list = mutableListOf<ContentItem>()
        for (doc in snap.documents) {
            val itemsMap = doc.data?.get("items") as? Map<String, Any?> ?: continue
            for ((itemId, rawVal) in itemsMap) {
                val itemMap = rawVal as? Map<String, Any?> ?: continue
                list.add(ChunkCodec.expandContent(itemId, itemMap, doc.id))
            }
        }
        return list.sortedByDescending { it.order.takeIf { o -> o > 0 }?.toString() ?: it.createdAt }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun saveContentItem(item: ContentItem): ContentItem {
        val finalId = item.id.ifBlank {
            "${if (item.type == "series") "ser" else "mov"}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        }
        val targetChunk = if (!item.chunkId.isNullOrBlank()) {
            if (item.chunkId.startsWith("content_chunk_")) item.chunkId else "content_chunk_${item.chunkId}"
        } else {
            // Find an existing chunk for type with room or create chunk_0
            val chunks = db.collection("content_chunks").get().await()
            val prefix = if (item.type == "series") "content_chunk_series_" else "content_chunk_movie_"
            val matching = chunks.documents.filter { it.id.startsWith(prefix) }.sortedBy { it.id }
            matching.lastOrNull { doc ->
                val count = (doc.get("items") as? Map<*, *>)?.size ?: 0
                val max = if (item.type == "series") 200 else 500
                count < max
            }?.id ?: "${prefix}${matching.size}"
        }

        val updatedItem = item.copy(
            id = finalId,
            chunkId = targetChunk.removePrefix("content_chunk_"),
            createdAt = item.createdAt.ifBlank { ChunkCodec.getNowUtcIso() },
            updatedAt = ChunkCodec.getNowUtcIso()
        )
        val minified = ChunkCodec.minifyContent(updatedItem)

        db.collection("content_chunks").document(targetChunk).set(
            mapOf("items" to mapOf(finalId to minified)),
            SetOptions.merge()
        ).await()

        db.collection("chunk_meta").document("versions").set(
            mapOf("content_chunks" to mapOf(targetChunk to System.currentTimeMillis()), "content_version" to System.currentTimeMillis()),
            SetOptions.merge()
        ).await()

        return updatedItem
    }

    suspend fun deleteContentItem(item: ContentItem) {
        val chunkDocId = if (!item.chunkId.isNullOrBlank()) {
            if (item.chunkId.startsWith("content_chunk_")) item.chunkId else "content_chunk_${item.chunkId}"
        } else return

        db.collection("content_chunks").document(chunkDocId)
            .update("items.${item.id}", FieldValue.delete())
            .await()

        db.collection("chunk_meta").document("versions").set(
            mapOf("content_chunks" to mapOf(chunkDocId to System.currentTimeMillis()), "content_version" to System.currentTimeMillis()),
            SetOptions.merge()
        ).await()
    }

    suspend fun fetchGenres(): List<Genre> {
        val snap = db.collection("genres").get().await()
        return snap.documents.map { doc ->
            Genre(
                id = doc.id,
                name = doc.getString("name").orEmpty(),
                order = (doc.get("order") as? Number)?.toInt() ?: 999
            )
        }.sortedBy { it.order }
    }

    suspend fun saveGenre(id: String?, name: String, order: Int) {
        val docId = id?.ifBlank { null } ?: "gen_${System.currentTimeMillis()}"
        db.collection("genres").document(docId).set(mapOf("id" to docId, "name" to name, "order" to order)).await()
    }

    suspend fun deleteGenre(id: String) {
        db.collection("genres").document(id).delete().await()
    }

    suspend fun fetchLanguages(): List<Language> {
        val snap = db.collection("languages").get().await()
        return snap.documents.map { doc ->
            Language(
                id = doc.id,
                name = doc.getString("name").orEmpty(),
                order = (doc.get("order") as? Number)?.toInt() ?: 999
            )
        }.sortedBy { it.order }
    }

    suspend fun saveLanguage(id: String?, name: String, order: Int) {
        val docId = id?.ifBlank { null } ?: "lang_${System.currentTimeMillis()}"
        db.collection("languages").document(docId).set(mapOf("id" to docId, "name" to name, "order" to order)).await()
    }

    suspend fun deleteLanguage(id: String) {
        db.collection("languages").document(id).delete().await()
    }

    suspend fun fetchQualities(): List<Quality> {
        val snap = db.collection("qualities").get().await()
        return snap.documents.map { doc ->
            Quality(
                id = doc.id,
                name = doc.getString("name").orEmpty(),
                color = doc.getString("color"),
                order = (doc.get("order") as? Number)?.toInt() ?: 999
            )
        }.sortedBy { it.order }
    }

    suspend fun saveQuality(id: String?, name: String, color: String?, order: Int) {
        val docId = id?.ifBlank { null } ?: "qual_${System.currentTimeMillis()}"
        val data = mutableMapOf<String, Any>("id" to docId, "name" to name, "order" to order)
        if (!color.isNullOrBlank()) data["color"] = color
        db.collection("qualities").document(docId).set(data).await()
    }

    suspend fun deleteQuality(id: String) {
        db.collection("qualities").document(id).delete().await()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun fetchCollections(): List<AppCollection> {
        val snap = db.collection("collection_chunks").get().await()
        val list = mutableListOf<AppCollection>()
        for (doc in snap.documents) {
            val items = doc.data?.get("items") as? Map<String, Any?> ?: continue
            for ((cid, raw) in items) {
                val m = raw as? Map<String, Any?> ?: continue
                list.add(
                    AppCollection(
                        id = m["id"]?.toString()?.ifBlank { cid } ?: cid,
                        title = m["title"]?.toString().orEmpty(),
                        description = m["description"]?.toString(),
                        contentIds = (m["contentIds"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        order = (m["order"] as? Number)?.toInt() ?: 0,
                        createdAt = m["createdAt"]?.toString().orEmpty(),
                        updatedAt = m["updatedAt"]?.toString().orEmpty()
                    )
                )
            }
        }
        return list.sortedBy { it.order }
    }

    suspend fun saveCollection(col: AppCollection) {
        val id = col.id.ifBlank { "col_${System.currentTimeMillis()}" }
        val payload = mapOf(
            "id" to id,
            "title" to col.title,
            "description" to (col.description ?: ""),
            "contentIds" to col.contentIds,
            "order" to col.order,
            "createdAt" to col.createdAt.ifBlank { ChunkCodec.getNowUtcIso() },
            "updatedAt" to ChunkCodec.getNowUtcIso()
        )
        db.collection("collection_chunks").document("collections_0").set(
            mapOf("items" to mapOf(id to payload), "updatedAt" to ChunkCodec.getNowUtcIso()),
            SetOptions.merge()
        ).await()
    }

    suspend fun deleteCollection(id: String) {
        val snap = db.collection("collection_chunks").get().await()
        for (doc in snap.documents) {
            val items = doc.data?.get("items") as? Map<*, *> ?: continue
            if (items.containsKey(id)) {
                db.collection("collection_chunks").document(doc.id).update("items.$id", FieldValue.delete()).await()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun fetchIncomeRecords(): List<IncomeRecord> {
        val snap = db.collection("income").document("data").get().await()
        val raw = snap.get("records") as? List<*> ?: return emptyList()
        return raw.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            IncomeRecord(
                id = m["id"]?.toString().orEmpty(),
                userId = m["userId"]?.toString(),
                userName = m["userName"]?.toString() ?: "Anonymous",
                amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                description = m["description"]?.toString().orEmpty(),
                date = m["date"]?.toString().orEmpty()
            )
        }.sortedByDescending { it.date }
    }

    suspend fun saveIncomeRecords(records: List<IncomeRecord>) {
        val serialized = records.map { r ->
            buildMap<String, Any> {
                put("id", r.id)
                put("amount", r.amount)
                put("description", r.description)
                put("date", r.date)
                put("userName", r.userName)
                r.userId?.let { put("userId", it) }
            }
        }
        db.collection("income").document("data").set(
            mapOf("records" to serialized, "updatedAt" to ChunkCodec.getNowUtcIso())
        ).await()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun fetchNotificationHistory(): List<AppNotification> {
        val snap = db.collection("notification_chunks").get().await()
        val list = mutableListOf<AppNotification>()
        for (doc in snap.documents) {
            val items = doc.data?.get("items") as? Map<String, Any?> ?: continue
            for ((nid, raw) in items) {
                val m = raw as? Map<String, Any?> ?: continue
                list.add(
                    AppNotification(
                        id = m["id"]?.toString()?.ifBlank { nid } ?: nid,
                        title = m["title"]?.toString().orEmpty(),
                        body = m["body"]?.toString().orEmpty(),
                        contentId = m["contentId"]?.toString(),
                        posterUrl = m["posterUrl"]?.toString(),
                        type = m["type"]?.toString() ?: "custom",
                        targetAudience = m["targetAudience"]?.toString() ?: "all",
                        targetUserIds = (m["targetUserIds"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        targetUserNames = (m["targetUserNames"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        buttonLabel = m["buttonLabel"]?.toString(),
                        buttonUrl = m["buttonUrl"]?.toString(),
                        createdAt = m["createdAt"]?.toString().orEmpty(),
                        createdBy = m["createdBy"]?.toString().orEmpty()
                    )
                )
            }
        }
        return list.sortedByDescending { it.createdAt }
    }

    suspend fun fetchNotificationTemplates(): List<NotificationTemplate> {
        val snap = db.collection("notification_templates").get().await()
        return snap.documents.map { doc ->
            NotificationTemplate(
                id = doc.id,
                name = doc.getString("name").orEmpty(),
                title = doc.getString("title").orEmpty(),
                body = doc.getString("body").orEmpty(),
                buttonLabel = doc.getString("buttonLabel"),
                buttonUrl = doc.getString("buttonUrl"),
                createdAt = doc.getString("createdAt").orEmpty()
            )
        }.sortedByDescending { it.createdAt }
    }

    suspend fun saveNotificationTemplate(tpl: NotificationTemplate) {
        val id = tpl.id.ifBlank { "tpl_${System.currentTimeMillis()}" }
        val payload = buildMap<String, Any> {
            put("id", id)
            put("name", tpl.name)
            put("title", tpl.title)
            put("body", tpl.body)
            tpl.buttonLabel?.let { put("buttonLabel", it) }
            tpl.buttonUrl?.let { put("buttonUrl", it) }
            put("createdAt", tpl.createdAt.ifBlank { ChunkCodec.getNowUtcIso() })
        }
        db.collection("notification_templates").document(id).set(payload).await()
    }

    suspend fun deleteNotificationTemplate(id: String) {
        db.collection("notification_templates").document(id).delete().await()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun fetchAppSettings(): Pair<AppSettings, Map<String, Any?>> {
        val snap = db.collection("settings").document("app_settings").get().await()
        val data = snap.data ?: emptyMap()
        val banks = (data["bankAccounts"] as? List<*>)?.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            BankAccount(
                id = m["id"]?.toString().orEmpty(),
                name = m["name"]?.toString().orEmpty(),
                accountNumber = m["accountNumber"]?.toString().orEmpty(),
                accountTitle = m["accountTitle"]?.toString().orEmpty(),
                color = m["color"]?.toString() ?: "#10B981",
                allowAutoApproval = m["allowAutoApproval"] != false
            )
        } ?: emptyList()

        val settings = AppSettings(
            headerText = data["headerText"]?.toString() ?: "MovizNow",
            membershipFee = (data["membershipFee"] as? Number)?.toDouble() ?: 200.0,
            movieFee = (data["movieFee"] as? Number)?.toDouble() ?: 50.0,
            seasonFee = (data["seasonFee"] as? Number)?.toDouble() ?: 100.0,
            supportNumber = data["supportNumber"]?.toString() ?: "3416286423",
            accountTitle = data["accountTitle"]?.toString() ?: "Asmat Ullah",
            accountNumber = data["accountNumber"]?.toString() ?: "03416286423",
            defaultAppImage = data["defaultAppImage"]?.toString().orEmpty(),
            paymentDetails = data["paymentDetails"]?.toString().orEmpty(),
            itemsPerPage = (data["itemsPerPage"] as? Number)?.toInt() ?: 20,
            isTrialEnabled = data["isTrialEnabled"] != false,
            isPhoneLoginEnabled = data["isPhoneLoginEnabled"] != false,
            isAdminContactEnabled = data["isAdminContactEnabled"] != false,
            isPaymentEnabled = data["isPaymentEnabled"] != false,
            isMaintenanceModeEnabled = data["isMaintenanceModeEnabled"] == true,
            maintenanceMessage = data["maintenanceMessage"]?.toString() ?: "App is currently under maintenance. Please try again later.",
            whatsappChannelLink = data["whatsappChannelLink"]?.toString().orEmpty(),
            whatsappClipsLink = data["whatsappClipsLink"]?.toString().orEmpty(),
            bankAccounts = banks,
            hiddenAdminTabs = (data["hiddenAdminTabs"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        )
        return settings to data
    }

    suspend fun saveAppSettings(settings: AppSettings) {
        val updates = mapOf(
            "headerText" to settings.headerText,
            "membershipFee" to settings.membershipFee,
            "movieFee" to settings.movieFee,
            "seasonFee" to settings.seasonFee,
            "supportNumber" to settings.supportNumber,
            "accountTitle" to settings.accountTitle,
            "accountNumber" to settings.accountNumber,
            "defaultAppImage" to settings.defaultAppImage,
            "paymentDetails" to settings.paymentDetails,
            "itemsPerPage" to settings.itemsPerPage,
            "isTrialEnabled" to settings.isTrialEnabled,
            "isPhoneLoginEnabled" to settings.isPhoneLoginEnabled,
            "isAdminContactEnabled" to settings.isAdminContactEnabled,
            "isPaymentEnabled" to settings.isPaymentEnabled,
            "isMaintenanceModeEnabled" to settings.isMaintenanceModeEnabled,
            "maintenanceMessage" to settings.maintenanceMessage,
            "whatsappChannelLink" to settings.whatsappChannelLink,
            "whatsappClipsLink" to settings.whatsappClipsLink,
            "updatedAt" to ChunkCodec.getNowUtcIso()
        )
        db.collection("settings").document("app_settings").set(updates, SetOptions.merge()).await()
    }
}

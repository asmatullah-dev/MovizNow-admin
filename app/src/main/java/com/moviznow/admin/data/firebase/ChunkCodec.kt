package com.moviznow.admin.data.firebase

import com.moviznow.admin.data.model.ContentItem
import com.moviznow.admin.data.model.Episode
import com.moviznow.admin.data.model.LinkDef
import com.moviznow.admin.data.model.Season
import com.moviznow.admin.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object ChunkCodec {

    private val isoUtcFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private val pktOffsetFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+05:00'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+05:00")
        }

    fun getNowUtcIso(): String = isoUtcFormat.format(Date())

    fun getNowPktIso(): String = pktOffsetFormat.format(Date())

    fun standardizePhone(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        var cleaned = phone.trim()
        if (cleaned.contains(":")) cleaned = cleaned.substringBefore(":").trim()
        if (cleaned.contains("|")) cleaned = cleaned.substringBefore("|").trim()

        val digits = cleaned.filter { it.isDigit() }
        if (digits.isEmpty()) return ""

        var base = digits
        if (base.startsWith("92") && base.length > 10) {
            base = base.substring(2)
        }
        if (base.startsWith("0") && base.length == 11) {
            base = base.substring(1)
        }
        if (base.length == 10 && base.startsWith("3")) {
            return "+92$base"
        }
        if (cleaned.startsWith("+")) return "+$digits"
        if (digits.length == 12 && digits.startsWith("923")) return "+$digits"
        return digits
    }

    fun isUserExpired(expiryDate: String?): Boolean {
        if (expiryDate.isNullOrBlank() || expiryDate == "Lifetime" || expiryDate == "null") {
            return false
        }
        val cleanDate = expiryDate.substringBefore("T")
        val parts = cleanDate.split("-")
        if (parts.size != 3) return false
        val year = parts[0].toIntOrNull() ?: return false
        val month = (parts[1].toIntOrNull() ?: return false) - 1
        val day = parts[2].toIntOrNull() ?: return false

        val isMidnightBoundary = expiryDate.contains("T00:00:00") && !expiryDate.contains("23:59")
        val boundaryDay = if (isMidnightBoundary) day - 1 else day

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, boundaryDay)
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return System.currentTimeMillis() >= cal.timeInMillis
    }

    fun extendExpiryIso(currentExpiry: String?, months: Int): String {
        val baseCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+05:00"))
        if (!currentExpiry.isNullOrBlank() && currentExpiry != "Lifetime" && !isUserExpired(currentExpiry)) {
            val parts = currentExpiry.substringBefore("T").split("-")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                val d = parts[2].toIntOrNull()
                if (y != null && m != null && d != null) {
                    baseCal.set(y, m - 1, d, 23, 59, 59)
                }
            }
        }
        baseCal.add(Calendar.MONTH, months)
        val y = baseCal.get(Calendar.YEAR)
        val m = baseCal.get(Calendar.MONTH) + 1
        val d = baseCal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02dT23:59:59.000+05:00", y, m, d)
    }

    fun normalizeUser(u: UserProfile): UserProfile {
        val emailLower = u.email.lowercase(Locale.US)
        if (emailLower == MovizNowFirebase.OWNER_EMAIL) {
            return u.copy(role = "owner", status = "active", expiryDate = "Lifetime")
        }
        if (MovizNowFirebase.ADMIN_EMAILS.contains(emailLower)) {
            return u.copy(role = "admin", status = "active", expiryDate = u.expiryDate ?: "Lifetime")
        }
        if (u.status == "deleted") return u
        if (u.role == "owner" || u.role == "admin") {
            return u.copy(status = "active", expiryDate = u.expiryDate ?: "Lifetime")
        }
        if (u.expiryDate == "Lifetime") {
            return if (u.status != "suspended") u.copy(status = "active") else u
        }
        if (u.expiryDate.isNullOrBlank() || u.expiryDate == "null") {
            return if (u.status != "suspended" && u.status != "pending") {
                u.copy(status = "pending", expiryDate = null)
            } else u
        }
        return if (isUserExpired(u.expiryDate)) {
            if (u.status != "suspended") u.copy(status = "expired") else u
        } else {
            if (u.status != "suspended" && u.status != "pending") u.copy(status = "active") else u
        }
    }

    private fun parseLinkList(raw: Any?): List<LinkDef> {
        val result = mutableListOf<LinkDef>()
        when (raw) {
            is List<*> -> {
                raw.forEachIndexed { idx, item ->
                    val map = item as? Map<*, *> ?: return@forEachIndexed
                    val url = (map["ur"] ?: map["url"])?.toString().orEmpty()
                    if (url.isNotBlank()) {
                        result.add(
                            LinkDef(
                                id = map["id"]?.toString()?.takeIf { it.isNotBlank() } ?: "lnk_${idx}_${UUID.randomUUID().toString().take(5)}",
                                name = (map["nm"] ?: map["name"])?.toString().orEmpty(),
                                url = url,
                                size = (map["sz"] ?: map["size"])?.toString().orEmpty(),
                                unit = (map["un"] ?: map["unit"])?.toString()?.ifBlank { "MB" } ?: "MB"
                            )
                        )
                    }
                }
            }
            is String -> {
                if (raw.isNotBlank() && raw.trim().startsWith("[")) {
                    try {
                        val arr = JSONArray(raw)
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val url = obj.optString("ur").ifBlank { obj.optString("url") }
                            if (url.isNotBlank()) {
                                result.add(
                                    LinkDef(
                                        id = obj.optString("id").ifBlank { "lnk_$i" },
                                        name = obj.optString("nm").ifBlank { obj.optString("name") },
                                        url = url,
                                        size = obj.optString("sz").ifBlank { obj.optString("size") },
                                        unit = obj.optString("un").ifBlank { obj.optString("unit").ifBlank { "MB" } }
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return result
    }

    private fun serializeMinLinks(links: List<LinkDef>): List<Map<String, Any>> {
        return links.filter { it.url.isNotBlank() }.map { l ->
            buildMap {
                put("ur", l.url)
                if (l.name.isNotBlank()) put("nm", l.name)
                if (l.size.isNotBlank()) put("sz", l.size)
                if (l.unit.isNotBlank()) put("un", l.unit)
            }
        }
    }

    private fun parseCommaOrList(raw: Any?): List<String> {
        return when (raw) {
            is List<*> -> raw.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
            is String -> raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun expandContent(id: String, map: Map<String, Any?>, chunkId: String? = null): ContentItem {
        val normalizedChunk = chunkId?.removePrefix("content_chunk_")
        val rawSeasons = map["sea"] ?: map["seasons"]
        val seasons = parseSeasons(rawSeasons)
        val rawType = map["type"]?.toString()
        val resolvedType = when {
            normalizedChunk?.startsWith("movie_") == true -> "movie"
            normalizedChunk?.startsWith("series_") == true -> "series"
            rawType == "series" || rawType == "movie" -> rawType
            seasons.isNotEmpty() -> "series"
            else -> "movie"
        }

        val rawStatus = (map["sta"] ?: map["status"])?.toString() ?: "p"
        val resolvedStatus = when (rawStatus) {
            "p" -> "published"
            "d" -> "draft"
            else -> rawStatus
        }

        val rawSub = map["sub"] ?: map["subtitles"]
        val hasSubtitles = rawSub == true || rawSub?.toString() == "yes" || rawSub?.toString() == "true"

        return ContentItem(
            id = map["id"]?.toString()?.ifBlank { id } ?: id,
            type = resolvedType,
            title = (map["tit"] ?: map["title"])?.toString().orEmpty(),
            secondTitle = map["secondTitle"]?.toString(),
            description = (map["dsc"] ?: map["description"])?.toString().orEmpty(),
            posterUrl = (map["pos"] ?: map["posterUrl"])?.toString().orEmpty(),
            backdropUrl = map["backdropUrl"]?.toString(),
            trailerUrl = (map["trai"] ?: map["trailerUrl"])?.toString().orEmpty(),
            imdbLink = (map["imdb"] ?: map["imdbLink"])?.toString(),
            imdbRating = map["imdbRating"]?.toString(),
            genreIds = parseCommaOrList(map["gen"] ?: map["genreIds"]),
            languageIds = parseCommaOrList(map["lan"] ?: map["languageIds"]),
            qualityId = (map["qua"] ?: map["qualityId"])?.toString(),
            cast = parseCommaOrList(map["cst"] ?: map["cast"]),
            year = ((map["yea"] ?: map["year"]) as? Number)?.toInt()
                ?: (map["yea"] ?: map["year"])?.toString()?.toIntOrNull()
                ?: 2026,
            releaseDate = (map["rdte"] ?: map["releaseDate"])?.toString(),
            runtime = (map["run"] ?: map["runtime"])?.toString(),
            country = (map["cnt"] ?: map["country"])?.toString(),
            ottPlatform = (map["ott"] ?: map["ottPlatform"])?.toString(),
            subtitles = hasSubtitles,
            status = resolvedStatus,
            isUpcoming = map["isUpcoming"] == true,
            order = ((map["ord"] ?: map["order"]) as? Number)?.toInt() ?: 0,
            chunkId = normalizedChunk,
            createdAt = (map["cre"] ?: map["createdAt"])?.toString().orEmpty(),
            updatedAt = (map["upd"] ?: map["updatedAt"])?.toString().orEmpty(),
            addedBy = (map["uid"] ?: map["addedBy"])?.toString(),
            movieLinks = parseLinkList(map["lik"] ?: map["movieLinks"]),
            seasons = seasons
        )
    }

    private fun parseSeasons(raw: Any?): List<Season> {
        val list = mutableListOf<Season>()
        val sourceList: List<Map<String, Any?>> = when (raw) {
            is List<*> -> raw.mapNotNull { it as? Map<String, Any?> }
            is String -> {
                if (raw.trim().startsWith("[")) {
                    try {
                        val arr = JSONArray(raw)
                        (0 until arr.length()).mapNotNull { idx ->
                            arr.optJSONObject(idx)?.toMap()
                        }
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else emptyList()
            }
            else -> emptyList()
        }

        sourceList.forEach { ms ->
            val sn = ((ms["sn"] ?: ms["seasonNumber"]) as? Number)?.toInt()
                ?: (ms["sn"] ?: ms["seasonNumber"])?.toString()?.toIntOrNull()
                ?: 1
            val epsRaw = (ms["eps"] ?: ms["episodes"]) as? List<*>
            val episodes = epsRaw?.mapNotNull { item ->
                val me = item as? Map<*, *> ?: return@mapNotNull null
                val en = ((me["en"] ?: me["episodeNumber"]) as? Number)?.toInt()
                    ?: (me["en"] ?: me["episodeNumber"])?.toString()?.toIntOrNull()
                    ?: 1
                Episode(
                    id = "e$en",
                    episodeNumber = en,
                    title = (me["ti"] ?: me["title"])?.toString().orEmpty(),
                    description = (me["de"] ?: me["description"])?.toString().orEmpty(),
                    duration = (me["du"] ?: me["duration"])?.toString().orEmpty(),
                    links = parseLinkList(me["lks"] ?: me["links"])
                )
            } ?: emptyList()

            list.add(
                Season(
                    id = "s$sn",
                    seasonNumber = sn,
                    title = (ms["ti"] ?: ms["title"])?.toString().orEmpty(),
                    year = ((ms["yea"] ?: ms["year"]) as? Number)?.toInt(),
                    trailerUrl = (ms["trai"] ?: ms["trailerUrl"])?.toString().orEmpty(),
                    isFullSeasonMkv = ms["fsm"] != null || ms["isFullSeasonMKV"] == true,
                    folderLink = (ms["fl"] ?: ms["folderLink"])?.toString().orEmpty(),
                    zipLinks = parseLinkList(ms["zl"] ?: ms["zipLinks"]),
                    mkvLinks = parseLinkList(ms["ml"] ?: ms["mkvLinks"]),
                    episodes = episodes
                )
            )
        }
        return list
    }

    fun minifyContent(item: ContentItem): Map<String, Any> {
        val out = mutableMapOf<String, Any>()
        out["id"] = item.id
        out["tit"] = item.title
        out["yea"] = item.year
        out["pos"] = item.posterUrl
        out["dsc"] = item.description
        out["sta"] = when (item.status) {
            "published" -> "p"
            "draft" -> "d"
            else -> item.status
        }
        out["cre"] = item.createdAt.ifBlank { getNowUtcIso() }
        out["upd"] = getNowUtcIso()
        out["ord"] = item.order

        if (!item.secondTitle.isNullOrBlank()) out["secondTitle"] = item.secondTitle
        if (!item.backdropUrl.isNullOrBlank()) out["backdropUrl"] = item.backdropUrl
        if (item.trailerUrl.isNotBlank()) out["trai"] = item.trailerUrl
        if (!item.imdbLink.isNullOrBlank()) out["imdb"] = item.imdbLink
        if (!item.imdbRating.isNullOrBlank()) out["imdbRating"] = item.imdbRating
        if (item.genreIds.isNotEmpty()) out["gen"] = item.genreIds.joinToString(",")
        if (item.languageIds.isNotEmpty()) out["lan"] = item.languageIds.joinToString(",")
        if (!item.qualityId.isNullOrBlank()) out["qua"] = item.qualityId
        if (item.cast.isNotEmpty()) out["cst"] = item.cast.joinToString(",")
        if (!item.releaseDate.isNullOrBlank()) out["rdte"] = item.releaseDate
        if (!item.runtime.isNullOrBlank()) out["run"] = item.runtime
        if (!item.country.isNullOrBlank()) out["cnt"] = item.country
        if (!item.ottPlatform.isNullOrBlank()) out["ott"] = item.ottPlatform
        if (item.subtitles) out["sub"] = "yes"
        if (item.isUpcoming) out["isUpcoming"] = true
        if (!item.addedBy.isNullOrBlank()) out["uid"] = item.addedBy

        if (item.type == "movie" && item.movieLinks.isNotEmpty()) {
            out["lik"] = serializeMinLinks(item.movieLinks)
        }

        if (item.type == "series" && item.seasons.isNotEmpty()) {
            out["sea"] = item.seasons.map { s ->
                buildMap<String, Any> {
                    put("sn", s.seasonNumber)
                    if (s.title.isNotBlank()) put("ti", s.title)
                    s.year?.let { put("yea", it) }
                    if (s.trailerUrl.isNotBlank()) put("trai", s.trailerUrl)
                    if (s.isFullSeasonMkv) put("fsm", 1)
                    if (s.folderLink.isNotBlank()) put("fl", s.folderLink)
                    if (s.zipLinks.isNotEmpty()) put("zl", serializeMinLinks(s.zipLinks))
                    if (s.mkvLinks.isNotEmpty()) put("ml", serializeMinLinks(s.mkvLinks))
                    if (s.episodes.isNotEmpty()) {
                        put("eps", s.episodes.map { ep ->
                            buildMap<String, Any> {
                                put("en", ep.episodeNumber)
                                if (ep.title.isNotBlank()) put("ti", ep.title)
                                if (ep.description.isNotBlank()) put("de", ep.description)
                                if (ep.duration.isNotBlank()) put("du", ep.duration)
                                if (ep.links.isNotEmpty()) put("lks", serializeMinLinks(ep.links))
                            }
                        })
                    }
                }
            }
        }
        return out
    }

    private fun JSONObject.toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = opt(k)
            map[k] = when (v) {
                is JSONObject -> v.toMap()
                is JSONArray -> (0 until v.length()).map { idx ->
                    val item = v.opt(idx)
                    if (item is JSONObject) item.toMap() else item
                }
                JSONObject.NULL -> null
                else -> v
            }
        }
        return map
    }
}

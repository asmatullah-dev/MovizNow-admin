package com.moviznow.admin.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.moviznow.admin.BuildConfig
import kotlinx.coroutines.tasks.await

object MovizNowFirebase {
    private const val TAG = "MovizNowFirebase"

    val ADMIN_EMAILS = setOf(
        "asmatn628@gmail.com",
        "asmatullah9327@gmail.com",
        "kabirahmaddev@gmail.com",
        "wamoviesstation@gmail.com"
    )

    val OWNER_EMAIL = "asmatn628@gmail.com"

    val ADMIN_PHONES = setOf(
        "+923009327327",
        "+923006286280",
        "+923416286423"
    )

    @Volatile
    private var initializedApp: FirebaseApp? = null

    fun ensureInitialized(context: Context): FirebaseApp {
        initializedApp?.let { return it }
        synchronized(this) {
            initializedApp?.let { return it }
            val existing = FirebaseApp.getApps(context).firstOrNull()
            val app = existing ?: run {
                val options = FirebaseOptions.Builder()
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
                    .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            initializedApp = app
            return app
        }
    }

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val db: FirebaseFirestore
        get() {
            val app = initializedApp ?: FirebaseApp.getInstance()
            return FirebaseFirestore.getInstance(app, BuildConfig.FIRESTORE_DATABASE_ID)
        }

    suspend fun verifyConnection(): Result<Unit> {
        return try {
            db.collection("test").document("connection").get(Source.SERVER).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Permission-denied on /test/connection still proves server reachability;
            // offline / unavailable errors indicate configuration or connectivity issues.
            val msg = e.message.orEmpty()
            if (msg.contains("offline", ignoreCase = true) || msg.contains("UNAVAILABLE", ignoreCase = true)) {
                Log.e(TAG, "Firestore connection check failed: $msg", e)
                Result.failure(e)
            } else {
                Result.success(Unit)
            }
        }
    }
}

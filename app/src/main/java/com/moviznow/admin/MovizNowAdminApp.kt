package com.moviznow.admin

import android.app.Application
import com.moviznow.admin.data.firebase.MovizNowFirebase

class MovizNowAdminApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MovizNowFirebase.ensureInitialized(this)
    }
}

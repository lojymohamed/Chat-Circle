package com.example.chatcircle.notification

import android.content.Context
import com.example.chatcircle.R
import com.google.firebase.auth.FirebaseAuth
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OneSignalHelper(private val context: Context) {
    private var initialized = false

    fun initialize() {
        if (initialized) return
        val appId = context.getString(R.string.onesignal_app_id).trim()
        if (appId.isEmpty()) return

        initialized = true
        OneSignal.Debug.logLevel = LogLevel.WARN
        OneSignal.initWithContext(context, appId)
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            auth.currentUser?.uid?.let(OneSignal::login) ?: OneSignal.logout()
        }
        CoroutineScope(Dispatchers.Main).launch {
            OneSignal.Notifications.requestPermission(true)
        }
    }
}

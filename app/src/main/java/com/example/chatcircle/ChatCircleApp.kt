package com.example.chatcircle

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.chatcircle.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class ChatCircleApp : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UserRepositoryEntryPoint {
        fun userRepository(): UserRepository
    }

    override fun onCreate() {
        super.onCreate()

        // Listen to auth state changes to update presence
        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User signed in - set online
                updatePresence(user.uid, true)
            } else {
                // User signed out - set offline for the previous user
                // Note: We can't get the UID here since user is null
                // The signOut in ProfileViewModel handles this
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // App came to foreground - set online
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    updatePresence(uid, true)
                }
            }
            override fun onStop(owner: LifecycleOwner) {
                // App went to background - set offline
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    updatePresence(uid, false)
                }
            }
        })
    }

    private fun updatePresence(uid: String, isOnline: Boolean) {
        val entryPoint = EntryPoints.get(applicationContext, UserRepositoryEntryPoint::class.java)
        val userRepository = entryPoint.userRepository()
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.updatePresence(uid, isOnline)
        }
    }
}
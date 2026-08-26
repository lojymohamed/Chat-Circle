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
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                updatePresence(true)
            }
            override fun onStop(owner: LifecycleOwner) {
                updatePresence(false)
            }
        })
    }

    private fun updatePresence(isOnline: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val entryPoint = EntryPoints.get(applicationContext, UserRepositoryEntryPoint::class.java)
        val userRepository = entryPoint.userRepository()
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.updatePresence(uid, isOnline)
        }
    }
}
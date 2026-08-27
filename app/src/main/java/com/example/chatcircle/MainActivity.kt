package com.example.chatcircle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import com.example.chatcircle.notification.OneSignalHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @javax.inject.Inject
    lateinit var oneSignalHelper: OneSignalHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        oneSignalHelper.initialize()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)

        // Firebase Auth persists the signed-in user between app launches. Start at the
        // room list when that restored session is available instead of showing login.
        if (FirebaseAuth.getInstance().currentUser != null) {
            graph.setStartDestination(R.id.chatRoomFragment)
        }
        navController.graph = graph

        showNotificationDestinationIfPresent(navController)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        showNotificationDestinationIfPresent(navHostFragment.navController)
    }

    private fun showNotificationDestinationIfPresent(
        navController: androidx.navigation.NavController
    ) {
        val roomId = intent.getStringExtra(EXTRA_ROOM_ID) ?: return
        val roomName = intent.getStringExtra(EXTRA_ROOM_NAME) ?: "Chat room"
        intent.removeExtra(EXTRA_ROOM_ID)
        intent.removeExtra(EXTRA_ROOM_NAME)
        navController.navigate(
            R.id.chatFragment,
            bundleOf("roomId" to roomId, "roomName" to roomName)
        )
    }

    companion object {
        const val EXTRA_ROOM_ID = "roomId"
        const val EXTRA_ROOM_NAME = "roomName"
    }
}

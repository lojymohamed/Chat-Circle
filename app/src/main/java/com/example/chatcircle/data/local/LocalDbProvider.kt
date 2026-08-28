package com.example.chatcircle.data.local

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDbProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var db: ChatCircleDatabase? = null
    private var currentUid: String? = null

    fun open(uid: String): ChatCircleDatabase {
        if (uid == currentUid && db != null) return db!!
        close()
        db = Room.databaseBuilder(
            context,
            ChatCircleDatabase::class.java,
            "chatcircle_$uid.db"
        ).fallbackToDestructiveMigration()
            .build()
        currentUid = uid
        return db!!
    }

    fun close() {
        db?.close()
        db = null
        currentUid = null
    }
}
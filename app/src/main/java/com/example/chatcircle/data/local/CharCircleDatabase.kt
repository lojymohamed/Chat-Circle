package com.example.chatcircle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.chatcircle.data.local.dao.MessageDao
import com.example.chatcircle.data.local.entity.MessageEntity

@Database(
    entities = [MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChatCircleDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
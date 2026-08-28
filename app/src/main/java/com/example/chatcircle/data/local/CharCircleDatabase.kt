package com.example.chatcircle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.chatcircle.data.local.dao.ChatRoomDao
import com.example.chatcircle.data.local.dao.MessageDao
import com.example.chatcircle.data.local.entity.ChatRoomEntity
import com.example.chatcircle.data.local.entity.MessageEntity

@Database(
    entities = [MessageEntity::class, ChatRoomEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChatCircleDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun chatRoomDao(): ChatRoomDao
}
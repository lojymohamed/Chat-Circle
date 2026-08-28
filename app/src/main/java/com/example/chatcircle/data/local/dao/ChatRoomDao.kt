package com.example.chatcircle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chatcircle.data.local.entity.ChatRoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatRoomDao {

    // No WHERE clause needed — the DB itself is already scoped to one user
    // (LocalDbProvider opens chatcircle_<uid>.db), and observeUserRooms only
    // syncs rooms Firestore already filtered via whereArrayContains("memberIds", uid).
    @Query("SELECT * FROM chat_rooms ORDER BY lastMessageTimestamp DESC")
    fun observeRooms(): Flow<List<ChatRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rooms: List<ChatRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(room: ChatRoomEntity)
}
package com.example.chatcircle.ui.rooms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chatcircle.databinding.ItemChatRoomBinding
import com.example.chatcircle.domain.model.ChatRoom

class ChatRoomAdapter(
    private val onRoomClick: (ChatRoom) -> Unit
) : ListAdapter<ChatRoom, ChatRoomAdapter.ChatRoomViewHolder>(DiffCallback) {

    private var unreadCounts: Map<String, Int> = emptyMap()

    fun updateUnreadCounts(counts: Map<String, Int>) {
        unreadCounts = counts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatRoomViewHolder(binding, onRoomClick)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        val room = getItem(position)
        holder.bind(room, unreadCounts[room.id] ?: 0)
    }

    class ChatRoomViewHolder(
        private val binding: ItemChatRoomBinding,
        private val onRoomClick: (ChatRoom) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(room: ChatRoom, unreadCount: Int) {
            binding.roomNameText.text = room.name
            if (!room.lastMessage.isNullOrEmpty()) {
                binding.lastMessageText.visibility = View.VISIBLE
                binding.lastMessageText.text = room.lastMessage
            } else {
                binding.lastMessageText.visibility = View.VISIBLE
                binding.lastMessageText.text = "No messages yet"
            }

            if (unreadCount > 0) {
                binding.unreadBadge.visibility = View.VISIBLE
                binding.unreadBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
            } else {
                binding.unreadBadge.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onRoomClick(room)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ChatRoom>() {
        override fun areItemsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean =
            oldItem == newItem
    }
}
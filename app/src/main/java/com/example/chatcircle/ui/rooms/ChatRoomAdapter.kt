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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatRoomViewHolder(binding, onRoomClick)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatRoomViewHolder(
        private val binding: ItemChatRoomBinding,
        private val onRoomClick: (ChatRoom) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(room: ChatRoom) {
            binding.roomNameText.text = room.name
            if (!room.lastMessage.isNullOrEmpty()) {
                binding.lastMessageText.visibility = View.VISIBLE
                binding.lastMessageText.text = room.lastMessage
            } else {
                binding.lastMessageText.visibility = View.VISIBLE
                binding.lastMessageText.text = "No messages yet"
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

package com.example.chatcircle.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.chatcircle.databinding.ItemMessageReceivedBinding
import com.example.chatcircle.databinding.ItemMessageSentBinding
import com.example.chatcircle.domain.model.Message

private const val VIEW_TYPE_SENT = 1
private const val VIEW_TYPE_RECEIVED = 2

class MessageAdapter(
    private val currentUserId: String
) : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            SentViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val message = getItem(position)

        when (holder) {
            is SentViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
        }
    }

    class SentViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            if (!message.imageUrl.isNullOrEmpty()) {
                binding.messageImage.visibility = View.VISIBLE
                binding.messageText.visibility = View.GONE
                binding.messageImage.load(message.imageUrl) {
                    crossfade(true)
                }
            } else {
                binding.messageImage.visibility = View.GONE
                binding.messageText.visibility = View.VISIBLE
                binding.messageText.text = message.text
            }
        }
    }

    class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.senderName.text = message.senderName
            if (!message.imageUrl.isNullOrEmpty()) {
                binding.messageImage.visibility = View.VISIBLE
                binding.messageText.visibility = View.GONE
                binding.messageImage.load(message.imageUrl) {
                    crossfade(true)
                }
            } else {
                binding.messageImage.visibility = View.GONE
                binding.messageText.visibility = View.VISIBLE
                binding.messageText.text = message.text
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
    }
}
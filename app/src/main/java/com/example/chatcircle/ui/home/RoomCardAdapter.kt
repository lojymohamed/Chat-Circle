package com.example.chatcircle.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chatcircle.R
import com.example.chatcircle.databinding.ItemRoomCardBinding
import com.example.chatcircle.domain.model.ChatRoom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "CC_RoomCardAdapter"

/**
 * Rooms as wide cards, for the accent band on Home.
 *
 * Separate from ChatRoomAdapter on purpose: that one renders a dense list row
 * and this one renders a card in a scrolling strip. Sharing a single adapter
 * across both would mean branching on view type inside every bind.
 */
class RoomCardAdapter(
    private val onRoomClick: (ChatRoom) -> Unit
) : ListAdapter<ChatRoom, RoomCardAdapter.RoomCardViewHolder>(DiffCallback) {

    private var unreadCounts: Map<String, Int> = emptyMap()

    fun updateUnreadCounts(counts: Map<String, Int>) {
        Log.d(TAG, "updateUnreadCounts() called: rooms=${counts.size}")
        unreadCounts = counts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomCardViewHolder {
        Log.d(TAG, "onCreateViewHolder() called")
        val binding = ItemRoomCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        // Sized here rather than in XML so the card is a fraction of the screen
        // instead of a fixed dp width - the same layout then reads correctly on
        // a small phone and a tablet, and the leftover sliver is what makes the
        // next card peek.
        val screenWidth = parent.resources.displayMetrics.widthPixels
        val cardWidth = (screenWidth * CARD_WIDTH_FRACTION).toInt()

        // Modifying the existing params keeps the margin the XML root declares;
        // assigning fresh LayoutParams would drop it.
        binding.root.layoutParams = binding.root.layoutParams.apply {
            width = cardWidth
        }
        // Set outright rather than as a minimum: the band above sizes itself to
        // this card, so the card has to declare a real height or the band
        // collapses to whatever the text happens to need.
        binding.cardRoot.layoutParams = binding.cardRoot.layoutParams.apply {
            height = (cardWidth * CARD_HEIGHT_RATIO).toInt()
        }

        Log.d(TAG, "onCreateViewHolder() success: cardWidth=$cardWidth px")
        return RoomCardViewHolder(binding, onRoomClick)
    }

    override fun onBindViewHolder(holder: RoomCardViewHolder, position: Int) {
        val room = getItem(position)
        holder.bind(room, unreadCounts[room.id] ?: 0)
    }

    class RoomCardViewHolder(
        private val binding: ItemRoomCardBinding,
        private val onRoomClick: (ChatRoom) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(room: ChatRoom, unread: Int) {
            Log.d(TAG, "bind() called: roomId=${room.id}, unread=$unread")

            val context = binding.root.context
            // Capitalised here rather than on write: existing rooms were
            // created before this rule, and rewriting stored names would be a
            // migration for a purely cosmetic change.
            binding.cardTitle.text = room.name.replaceFirstChar { it.uppercase() }

            // Who is in the room and when it last stirred, rather than the last
            // message body - a brand new room would otherwise read "No messages
            // yet", which says nothing useful about it.
            val members = context.resources.getQuantityString(
                R.plurals.room_members,
                room.memberIds.size,
                room.memberIds.size
            )
            val lastActive = room.lastMessageTimestamp?.let { DATE_FORMAT.format(Date(it)) }

            binding.cardMeta.text = if (lastActive != null) {
                context.getString(R.string.home_card_meta, members, lastActive)
            } else {
                members
            }

            binding.cardUnread.visibility = if (unread > 0) View.VISIBLE else View.GONE
            binding.cardUnread.text = if (unread > 99) "99+" else unread.toString()

            binding.cardRoot.setOnClickListener { onRoomClick(room) }
        }
    }

    private companion object {
        /** Short, no year - these are recent rooms, not an archive. */
        val DATE_FORMAT = SimpleDateFormat("d MMM", Locale.getDefault())

        /** Share of the screen one card takes. The rest is the next card peeking. */
        const val CARD_WIDTH_FRACTION = 0.76f

        /** Height as a share of width, matching the reference proportions. */
        const val CARD_HEIGHT_RATIO = 0.62f
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatRoom>() {
        override fun areItemsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean =
            oldItem == newItem
    }
}

package com.example.chatcircle.ui.directory

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.chatcircle.R
import com.example.chatcircle.ui.common.InitialsAvatar
import com.example.chatcircle.databinding.ItemPersonRowBinding
import com.example.chatcircle.domain.model.User

private const val TAG = "CC_PersonRowAdapter"

/**
 * People as full-width rows, for the Directory.
 *
 * Separate from PeopleAdapter, which renders the compact avatar used in the
 * horizontal strip on Home - same data, different density.
 */
class PersonRowAdapter(
    private val onPersonClick: (User) -> Unit
) : ListAdapter<User, PersonRowAdapter.PersonRowViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonRowViewHolder {
        Log.d(TAG, "onCreateViewHolder() called")
        val binding = ItemPersonRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PersonRowViewHolder(binding, onPersonClick)
    }

    override fun onBindViewHolder(holder: PersonRowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PersonRowViewHolder(
        private val binding: ItemPersonRowBinding,
        private val onPersonClick: (User) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            Log.d(TAG, "bind() called: uid=${user.uid}")

            binding.rowName.text = user.displayName.ifBlank {
                user.email.substringBefore('@').ifBlank { "?" }
            }

            // Presence is the more useful second line than the email, which is
            // often just the name again.
            binding.rowSubtitle.setText(
                if (user.isOnline) R.string.person_online else R.string.person_offline
            )

            binding.rowPresence.setBackgroundResource(
                if (user.isOnline) R.drawable.bg_presence_online
                else R.drawable.bg_presence_offline
            )

            val initials = InitialsAvatar.forUser(binding.root.context, user)
            binding.rowAvatar.load(user.photoUrl) {
                placeholder(initials)
                error(initials)
                fallback(initials)
            }

            binding.root.setOnClickListener { onPersonClick(user) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem == newItem
    }
}

package com.example.chatcircle.ui.rooms

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.chatcircle.R
import com.example.chatcircle.ui.common.InitialsAvatar
import com.example.chatcircle.databinding.ItemPersonBinding
import com.example.chatcircle.domain.model.User

private const val TAG = "CC_PeopleAdapter"

/**
 * The horizontal strip of people on the home screen.
 *
 * Sorting is the repository's job, not this adapter's - it renders whatever
 * order it is given.
 */
class PeopleAdapter(
    private val onPersonClick: (User) -> Unit
) : ListAdapter<User, PeopleAdapter.PersonViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        Log.d(TAG, "onCreateViewHolder() called")
        val binding = ItemPersonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PersonViewHolder(binding, onPersonClick)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PersonViewHolder(
        private val binding: ItemPersonBinding,
        private val onPersonClick: (User) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            Log.d(TAG, "bind() called: uid=${user.uid}, online=${user.isOnline}")

            // Fall back to the email prefix so a user who never set a name is
            // still identifiable rather than showing as a blank avatar.
            binding.personName.text = user.displayName.ifBlank {
                user.email.substringBefore('@').ifBlank { "?" }
            }

            // fallback() matters as much as error(): Coil uses it when the
            // data itself is null, which is the common case here.
            val initials = InitialsAvatar.forUser(binding.root.context, user)
            binding.personAvatar.load(user.photoUrl) {
                placeholder(initials)
                error(initials)
                fallback(initials)
            }

            binding.personPresence.setBackgroundResource(
                if (user.isOnline) R.drawable.bg_presence_online
                else R.drawable.bg_presence_offline
            )

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

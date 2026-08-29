package com.example.chatcircle.ui.home

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.chatcircle.R
import com.example.chatcircle.ui.common.InitialsAvatar
import com.example.chatcircle.databinding.FragmentHomeBinding
import com.example.chatcircle.databinding.SheetNotificationsBinding
import com.example.chatcircle.databinding.SheetRoomActionsBinding
import com.example.chatcircle.domain.model.ChatRoom
import com.example.chatcircle.ui.rooms.ChatRoomAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.chatcircle.ui.rooms.ChatRoomViewModel
import com.example.chatcircle.ui.rooms.PeopleAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private const val TAG = "CC_Home"

/**
 * The landing tab: a dashboard over the same rooms and people the other tabs
 * list in full.
 *
 * It reuses [ChatRoomViewModel] rather than owning a view model of its own.
 * Home and the Rooms tab show the same two streams, and a second view model
 * would mean a second Firestore listener on each of them for no benefit.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatRoomViewModel by viewModels()

    private lateinit var roomCardAdapter: RoomCardAdapter
    private lateinit var peopleAdapter: PeopleAdapter

    /** Drift animators for the contour pattern behind the band. */
    private val patternAnimators = mutableListOf<Animator>()

    /** Latest rooms and unread counts, so the bell sheet can be built on demand. */
    private var latestRooms: List<ChatRoom> = emptyList()
    private var latestUnread: Map<String, Int> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView() called")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated() called")

        setupHeader()
        setupLists()
        setupActions()
        observeViewModel()
    }

    private fun setupHeader() {
        val user = viewModel.currentUser()
        Log.d(TAG, "setupHeader() called: hasUser=${user != null}")

        binding.headerName.text = user?.displayName?.ifBlank {
            user.email.substringBefore('@')
        } ?: getString(R.string.home_greeting)

        // Brand blue, because this row is always you - the palette is for
        // telling other people apart.
        val initials = InitialsAvatar.forCurrentUser(requireContext(), user)

        binding.headerAvatar.load(user?.photoUrl) {
            placeholder(initials)
            error(initials)
            fallback(initials)
        }
    }

    private fun setupLists() {
        Log.d(TAG, "setupLists() called")

        roomCardAdapter = RoomCardAdapter { room ->
            Log.i(TAG, "setupLists(): opening room ${room.id}")
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToChatFragment(
                    roomId = room.id,
                    roomName = room.name
                )
            )
        }
        binding.roomCardsRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.roomCardsRecycler.adapter = roomCardAdapter

        peopleAdapter = PeopleAdapter { person ->
            Toast.makeText(
                requireContext(),
                getString(R.string.home_direct_chat_soon, person.displayName),
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.peopleRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.peopleRecycler.adapter = peopleAdapter
    }

    private fun setupActions() {
        Log.d(TAG, "setupActions() called")

        binding.fabRoomActions.setOnClickListener { showRoomActions() }

        // Both "see all" links land on the Directory, which lists rooms and
        // people in full behind its two tabs.
        binding.bandSeeAll.setOnClickListener {
            findNavController().navigate(R.id.directoryFragment)
        }
        binding.peopleSeeAll.setOnClickListener {
            findNavController().navigate(R.id.directoryFragment)
        }
        binding.headerBell.setOnClickListener { showNotifications() }
    }

    /**
     * Opens the create-or-join sheet.
     *
     * Creating is handled inline because it only needs a name. Joining is not:
     * it needs a code and a camera, so it hands off to the join screen and the
     * sheet closes first, or it would still be sitting there on the way back.
     */
    private fun showRoomActions() {
        Log.d(TAG, "showRoomActions() called")

        val sheetBinding = SheetRoomActionsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        sheetBinding.sheetCreate.setOnClickListener {
            val name = sheetBinding.sheetRoomName.text?.toString()?.trim().orEmpty()
            Log.i(TAG, "showRoomActions(): create requested, nameLength=${name.length}")

            if (name.isEmpty()) {
                sheetBinding.sheetRoomNameLayout.error =
                    getString(R.string.sheet_name_required)
                return@setOnClickListener
            }
            sheetBinding.sheetRoomNameLayout.error = null
            viewModel.createRoom(name)
            dialog.dismiss()
        }

        sheetBinding.sheetJoin.setOnClickListener {
            Log.i(TAG, "showRoomActions(): join requested")
            dialog.dismiss()
            findNavController().navigate(R.id.action_homeFragment_to_joinRoomFragment)
        }

        dialog.show()
    }


    /**
     * Lists the rooms with unread messages.
     *
     * There is no notifications collection in the backend, so the sheet is
     * built from the unread counts the room list already tracks. That keeps the
     * bell truthful rather than showing a permanently empty screen.
     */
    private fun showNotifications() {
        Log.d(TAG, "showNotifications() called: rooms=${latestRooms.size}")

        val unreadRooms = latestRooms.filter { (latestUnread[it.id] ?: 0) > 0 }
        Log.d(TAG, "showNotifications(): unreadRooms=${unreadRooms.size}")

        val sheetBinding = SheetNotificationsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        val adapter = ChatRoomAdapter { room ->
            dialog.dismiss()
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToChatFragment(
                    roomId = room.id,
                    roomName = room.name
                )
            )
        }
        sheetBinding.notificationsList.layoutManager = LinearLayoutManager(requireContext())
        sheetBinding.notificationsList.adapter = adapter
        adapter.updateUnreadCounts(latestUnread)
        adapter.submitList(unreadRooms)

        sheetBinding.notificationsList.isVisible = unreadRooms.isNotEmpty()
        sheetBinding.notificationsEmpty.isVisible = unreadRooms.isEmpty()

        dialog.show()
    }

    /**
     * Drifts the pattern behind the band.
     *
     * Slow and short - the image is scaled to 1.12 in the layout purely to give
     * this room to move without ever revealing an edge, so the travel here must
     * stay well inside that margin.
     */
    private fun startPatternDrift() {
        Log.d(TAG, "startPatternDrift() called")
        stopPatternDrift()

        val animator = ObjectAnimator.ofPropertyValuesHolder(
            binding.bandPattern,
            PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -PATTERN_TRAVEL_DP.toPx(), PATTERN_TRAVEL_DP.toPx()),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, PATTERN_TRAVEL_DP.toPx() / 2f, -PATTERN_TRAVEL_DP.toPx() / 2f)
        ).apply {
            duration = PATTERN_DRIFT_MS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        patternAnimators += animator
    }

    private fun stopPatternDrift() {
        Log.d(TAG, "stopPatternDrift() called: count=${patternAnimators.size}")
        patternAnimators.forEach { it.cancel() }
        patternAnimators.clear()
    }

    private fun Float.toPx(): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        resources.displayMetrics
    )

    private fun observeViewModel() {
        Log.d(TAG, "observeViewModel() called")

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.userRooms.collect { rooms ->
                        Log.d(TAG, "observeViewModel(): rooms=${rooms.size}")
                        latestRooms = rooms
                        roomCardAdapter.submitList(rooms)
                        updateBellDot()
                        binding.roomCardsRecycler.visibility =
                            if (rooms.isEmpty()) View.GONE else View.VISIBLE
                        binding.bandEmpty.visibility =
                            if (rooms.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.unreadCounts.collect { counts ->
                        latestUnread = counts
                        roomCardAdapter.updateUnreadCounts(counts)
                        updateBellDot()
                    }
                }

                launch {
                    viewModel.people.collect { people ->
                        Log.d(TAG, "observeViewModel(): people=${people.size}")
                        peopleAdapter.submitList(people)
                        binding.peopleRecycler.visibility =
                            if (people.isEmpty()) View.GONE else View.VISIBLE
                        binding.peopleEmpty.visibility =
                            if (people.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    /** Shows the bell dot only when something is actually waiting. */
    private fun updateBellDot() {
        val hasUnread = latestRooms.any { (latestUnread[it.id] ?: 0) > 0 }
        binding.headerBellDot.isVisible = hasUnread
    }

    override fun onResume() {
        super.onResume()
        startPatternDrift()
        // The name or photo may have changed on the profile tab since this
        // screen was last shown.
        setupHeader()
    }

    override fun onPause() {
        stopPatternDrift()
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView() called")
        stopPatternDrift()
        binding.roomCardsRecycler.adapter = null
        binding.peopleRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        /**
         * How far the band pattern travels each way.
         *
         * The image is scaled to 1.12 in the layout purely to leave room for
         * this, so the travel has to stay comfortably inside that margin or an
         * edge of the image slides into view.
         */
        const val PATTERN_TRAVEL_DP = 14f

        /** Slow enough to notice only if you look for it. */
        const val PATTERN_DRIFT_MS = 16_000L
    }
}

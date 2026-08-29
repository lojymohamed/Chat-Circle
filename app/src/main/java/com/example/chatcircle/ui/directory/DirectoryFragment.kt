package com.example.chatcircle.ui.directory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.chatcircle.R
import com.example.chatcircle.databinding.FragmentDirectoryBinding
import com.example.chatcircle.domain.model.ChatRoom
import com.example.chatcircle.domain.model.User
import com.example.chatcircle.ui.rooms.ChatRoomAdapter
import com.example.chatcircle.ui.rooms.ChatRoomViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private const val TAG = "CC_Directory"

/**
 * Rooms and people in one searchable list, switched by two tabs.
 *
 * Both tabs share a single RecyclerView and the adapter is swapped on
 * selection - see fragment_directory.xml for why this is not a pager.
 *
 * Filtering happens here rather than in the view model: the query is pure view
 * state that should not survive rotation into a stale result, and both source
 * lists are already in memory.
 */
@AndroidEntryPoint
class DirectoryFragment : Fragment() {

    private var _binding: FragmentDirectoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatRoomViewModel by viewModels()

    private lateinit var roomAdapter: ChatRoomAdapter
    private lateinit var personAdapter: PersonRowAdapter

    /** Latest values from the view model, kept so search can re-filter. */
    private var allRooms: List<ChatRoom> = emptyList()
    private var allPeople: List<User> = emptyList()

    private var showingRooms = true
    private var query: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView() called")
        _binding = FragmentDirectoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated() called")

        setupAdapters()
        setupTabs()
        setupSearch()
        observeViewModel()
    }

    private fun setupAdapters() {
        Log.d(TAG, "setupAdapters() called")

        roomAdapter = ChatRoomAdapter { room ->
            Log.i(TAG, "setupAdapters(): opening room ${room.id}")
            findNavController().navigate(
                DirectoryFragmentDirections.actionDirectoryFragmentToChatFragment(
                    roomId = room.id,
                    roomName = room.name
                )
            )
        }

        personAdapter = PersonRowAdapter { person ->
            Toast.makeText(
                requireContext(),
                getString(R.string.home_direct_chat_soon, person.displayName),
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun setupTabs() {
        Log.d(TAG, "setupTabs() called")

        binding.tabRooms.setOnClickListener { selectTab(showRooms = true) }
        binding.tabPeople.setOnClickListener { selectTab(showRooms = false) }

        selectTab(showRooms = true)
    }

    /**
     * Moves the selected pill and swaps the adapter under the list.
     *
     * Selection is expressed by swapping each tab background rather than by
     * animating a separate indicator view - with only two fixed segments, the
     * indicator would be more state to keep in sync than it is worth.
     */
    private fun selectTab(showRooms: Boolean) {
        Log.d(TAG, "selectTab() called: showRooms=$showRooms")

        if (showingRooms == showRooms && binding.directoryList.adapter != null) {
            // Already here - re-selecting should not rebuild the list.
            return
        }
        showingRooms = showRooms

        val selected = if (showRooms) binding.tabRooms else binding.tabPeople
        val unselected = if (showRooms) binding.tabPeople else binding.tabRooms

        selected.setBackgroundResource(R.drawable.bg_tab_selected)
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        selected.typeface = ResourcesCompat.getFont(requireContext(), R.font.poppinssemibold)

        unselected.background = null
        unselected.setTextColor(ContextCompat.getColor(requireContext(), R.color.konecta_muted))
        unselected.typeface = ResourcesCompat.getFont(requireContext(), R.font.poppinsmedium)

        binding.directoryList.adapter = if (showRooms) roomAdapter else personAdapter
        applyFilter()
    }

    private fun setupSearch() {
        Log.d(TAG, "setupSearch() called")

        binding.searchInput.doAfterTextChanged { text ->
            query = text?.toString()?.trim().orEmpty()
            Log.d(TAG, "setupSearch(): queryLength=${query.length}")
            applyFilter()
        }
    }

    /** Re-filters whichever list is on screen and toggles the empty label. */
    private fun applyFilter() {
        val needle = query.lowercase()

        val visibleCount = if (showingRooms) {
            val rooms = allRooms.filter {
                needle.isEmpty() ||
                        it.name.lowercase().contains(needle) ||
                        it.id.lowercase().contains(needle)
            }
            roomAdapter.submitList(rooms)
            rooms.size
        } else {
            val people = allPeople.filter {
                needle.isEmpty() ||
                        it.displayName.lowercase().contains(needle) ||
                        it.email.lowercase().contains(needle)
            }
            personAdapter.submitList(people)
            people.size
        }

        Log.d(TAG, "applyFilter() called: showingRooms=$showingRooms, visible=$visibleCount")
        binding.directoryEmpty.visibility = if (visibleCount == 0) View.VISIBLE else View.GONE
    }

    private fun observeViewModel() {
        Log.d(TAG, "observeViewModel() called")

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.userRooms.collect { rooms ->
                        allRooms = rooms
                        applyFilter()
                    }
                }

                launch {
                    viewModel.unreadCounts.collect { counts ->
                        roomAdapter.updateUnreadCounts(counts)
                    }
                }

                launch {
                    viewModel.people.collect { people ->
                        allPeople = people
                        applyFilter()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView() called")
        binding.directoryList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}

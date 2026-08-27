package com.example.chatcircle.ui.rooms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatcircle.R
import com.example.chatcircle.databinding.FragmentChatRoomsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatRoomFragment : Fragment() {

    private var _binding: FragmentChatRoomsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatRoomViewModel by viewModels()
    private lateinit var roomAdapter: ChatRoomAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatRoomsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    findNavController().navigate(
                        ChatRoomFragmentDirections.actionChatRoomFragmentToProfileFragment()
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        roomAdapter = ChatRoomAdapter { room ->
            val action = ChatRoomFragmentDirections.actionChatRoomFragmentToChatFragment(
                roomId = room.id,
                roomName = room.name
            )
            findNavController().navigate(action)
        }
        binding.roomsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.roomsRecyclerView.adapter = roomAdapter
    }

    private fun setupButtons() {
        binding.createRoomButton.setOnClickListener {
            val roomName = binding.roomNameInput.text.toString()
            viewModel.createRoom(roomName)
        }

        binding.joinRoomButton.setOnClickListener {
            val roomCode = binding.roomCodeInput.text.toString()
            viewModel.joinRoom(roomCode)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is ChatRoomUiState.Idle -> {
                                binding.loadingSpinner.visibility = View.GONE
                            }
                            is ChatRoomUiState.Loading -> {
                                binding.loadingSpinner.visibility = View.VISIBLE
                                binding.statusText.text = ""
                            }
                            is ChatRoomUiState.Success -> {
                                binding.loadingSpinner.visibility = View.GONE
                                binding.statusText.text = state.message
                                binding.roomNameInput.text?.clear()
                                binding.roomCodeInput.text?.clear()
                            }
                            is ChatRoomUiState.Error -> {
                                binding.loadingSpinner.visibility = View.GONE
                                binding.statusText.text = state.message
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.userRooms.collect { rooms ->
                        roomAdapter.submitList(rooms)
                    }
                }

                launch {
                    viewModel.unreadCounts.collect { counts ->
                        roomAdapter.updateUnreadCounts(counts)
                    }
                }

                launch {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is ChatRoomNavigationEvent.NavigateToChatRoom -> {
                                val action = ChatRoomFragmentDirections
                                    .actionChatRoomFragmentToChatFragment(
                                        roomId = event.roomId,
                                        roomName = event.roomName
                                    )
                                findNavController().navigate(action)
                            }
                            is ChatRoomNavigationEvent.ShowCreatedRoomCode -> {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Room created")
                                    .setMessage(
                                        "Share this room code with others:\n\n${event.room.id}"
                                    )
                                    .setNegativeButton("Stay here", null)
                                    .setPositiveButton("Open room") { _, _ ->
                                        val action = ChatRoomFragmentDirections
                                            .actionChatRoomFragmentToChatFragment(
                                                roomId = event.room.id,
                                                roomName = event.room.name
                                            )
                                        findNavController().navigate(action)
                                    }
                                    .show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

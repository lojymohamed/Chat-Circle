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
import com.example.chatcircle.databinding.FragmentChatRoomsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatRoomFragment : Fragment() {

    private var _binding: FragmentChatRoomsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatRoomViewModel by viewModels()

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

        binding.createRoomButton.setOnClickListener {
            val roomName = binding.roomNameInput.text.toString()
            viewModel.createRoom(roomName)
        }

        binding.joinRoomButton.setOnClickListener {
            val roomCode = binding.roomCodeInput.text.toString()
            viewModel.joinRoom(roomCode)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ChatRoomUiState.Idle -> {
                            binding.loadingSpinner.visibility = View.GONE
                            binding.statusText.text = ""
                        }
                        is ChatRoomUiState.Loading -> {
                            binding.loadingSpinner.visibility = View.VISIBLE
                            binding.statusText.text = ""
                        }
                        is ChatRoomUiState.Success -> {
                            binding.loadingSpinner.visibility = View.GONE
                            binding.statusText.text = state.message
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        is ChatRoomUiState.Error -> {
                            binding.loadingSpinner.visibility = View.GONE
                            binding.statusText.text = state.message
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
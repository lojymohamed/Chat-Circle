package com.example.chatcircle.ui.chat

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
import com.example.chatcircle.databinding.FragmentChatBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chatToolbar.title = viewModel.roomName
        binding.chatToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val currentUserId =
            FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        messageAdapter = MessageAdapter(currentUserId)

        binding.messagesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        binding.messagesRecyclerView.adapter = messageAdapter

        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString()
            viewModel.sendMessage(text)
            binding.messageInput.text?.clear()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.uiState.collect { state ->

                    when (state) {

                        is ChatUiState.Loading -> {
                            // optional: show a spinner
                        }

                        is ChatUiState.Success -> {
                            messageAdapter.submitList(state.messages) {
                                binding.messagesRecyclerView.scrollToPosition(
                                    state.messages.lastIndex.coerceAtLeast(0)
                                )
                            }
                        }

                        is ChatUiState.Error -> {
                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
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
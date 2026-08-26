package com.example.chatcircle.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatcircle.databinding.FragmentChatBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter
    
    private var currentCameraUri: Uri? = null

    // Register ActivityResult for gallery picking
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.sendImageMessage(it)
        }
    }

    // Register ActivityResult for camera taking
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentCameraUri?.let {
                viewModel.sendImageMessage(it)
            }
        }
    }

    // Register ActivityResult for requesting camera permission
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

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

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        messageAdapter = MessageAdapter(currentUserId)

        binding.messagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.messagesRecyclerView.adapter = messageAdapter

        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString()
            viewModel.sendMessage(text)
            binding.messageInput.text?.clear()
        }

        binding.attachButton.setOnClickListener {
            showAttachmentOptions()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is ChatUiState.Loading -> {}
                            is ChatUiState.Success -> {
                                messageAdapter.submitList(state.messages) {
                                    binding.messagesRecyclerView.scrollToPosition(
                                        state.messages.lastIndex.coerceAtLeast(0)
                                    )
                                }
                            }
                            is ChatUiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                
                launch {
                    viewModel.onlineCount.collect { count ->
                        binding.chatToolbar.subtitle = if (count > 0) "$count online" else "Offline"
                    }
                }
                
                launch {
                    viewModel.isUploading.collect { isUploading ->
                        // Can add a small progress indicator on the UI or disable attach button
                        binding.attachButton.isEnabled = !isUploading
                    }
                }
            }
        }
    }
    
    private fun showAttachmentOptions() {
        val options = arrayOf("Gallery", "Camera")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Attach Image")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> checkCameraPermissionAndLaunch()
                }
            }
            .show()
    }
    
    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun launchCamera() {
        val photoFile = File(requireContext().cacheDir, "camera_images")
        if (!photoFile.exists()) {
            photoFile.mkdirs()
        }
        val file = File(photoFile, "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        currentCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.chatcircle.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.chatcircle.R
import com.example.chatcircle.ui.common.InitialsAvatar
import com.example.chatcircle.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.profileImage.load(it) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_profile)
                error(R.drawable.ic_profile)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupListeners() {
        binding.profileImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            val name = binding.displayNameInput.text.toString().trim()
            viewModel.updateProfile(name, selectedImageUri)
        }

        binding.signOutButton.setOnClickListener {
            viewModel.signOut()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.user.collect { user ->
                        user?.let {
                            if (binding.displayNameInput.text.isNullOrEmpty()) {
                                binding.displayNameInput.setText(it.displayName)
                            }
                            binding.emailText.text = it.email

                            // The header name is the display name, falling back
                            // to the email prefix so the header is never blank
                            // for someone who has not set a name yet.
                            binding.profileName.text = it.displayName.ifBlank {
                                it.email.substringBefore('@')
                            }
                            if (selectedImageUri == null) {
                                if (!it.photoUrl.isNullOrEmpty()) {
                                    val initials =
                                        InitialsAvatar.forUser(requireContext(), it)
                                    binding.profileImage.load(it.photoUrl) {
                                        crossfade(true)
                                        transformations(CircleCropTransformation())
                                        placeholder(initials)
                                        error(initials)
                                        fallback(initials)
                                    }
                                } else {
                                    binding.profileImage.setImageDrawable(
                                        InitialsAvatar.forUser(requireContext(), it)
                                    )
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is ProfileUiState.Idle -> {
                                binding.loadingSpinner.visibility = View.GONE
                                binding.saveButton.isEnabled = true
                            }
                            is ProfileUiState.Loading -> {
                                binding.loadingSpinner.visibility = View.VISIBLE
                                binding.saveButton.isEnabled = false
                            }
                            is ProfileUiState.Success -> {
                                binding.loadingSpinner.visibility = View.GONE
                                binding.saveButton.isEnabled = true
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                selectedImageUri = null
                            }
                            is ProfileUiState.Error -> {
                                binding.loadingSpinner.visibility = View.GONE
                                binding.saveButton.isEnabled = true
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is ProfileNavigationEvent.NavigateToLogin -> {
                                findNavController().navigate(
                                    ProfileFragmentDirections.actionProfileFragmentToLoginFragment()
                                )
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

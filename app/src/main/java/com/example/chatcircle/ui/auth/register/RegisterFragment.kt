package com.example.chatcircle.ui.auth.register

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.chatcircle.R
import com.example.chatcircle.databinding.FragmentRegisterBinding
import com.example.chatcircle.ui.auth.AuthUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.example.chatcircle.ui.common.focusWithoutKeyboard

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRegisterBinding.bind(view)

        // Email starts focused so the field reads as ready, but the keyboard
        // stays down until the user actually taps it.
        binding.etEmail.focusWithoutKeyboard()

        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {

        binding.btnSignUp.setOnClickListener {

            val email = binding.etEmail.text
                ?.toString()
                ?.trim()
                .orEmpty()

            val password = binding.etPassword.text
                ?.toString()
                .orEmpty()

            val confirmPassword = binding.etConfirmPassword.text
                ?.toString()
                .orEmpty()

            viewModel.register(
                email = email,
                password = password,
                confirmPassword = confirmPassword
            )
        }

        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeUiState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.uiState.collect { state ->

                    when (state) {

                        AuthUiState.Idle -> {
                            binding.btnSignUp.isEnabled = true
                        }

                        AuthUiState.Loading -> {
                            binding.btnSignUp.isEnabled = false
                        }

                        is AuthUiState.Success -> {

                            binding.btnSignUp.isEnabled = true

                            Toast.makeText(
                                requireContext(),
                                "Account created successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            findNavController().popBackStack()
                        }

                        is AuthUiState.Error -> {

                            binding.btnSignUp.isEnabled = true

                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_LONG
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
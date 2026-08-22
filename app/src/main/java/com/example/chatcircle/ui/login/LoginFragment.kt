package com.example.chatcircle.presentation.auth.login

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
import com.example.chatcircle.databinding.FragmentLoginBinding
import com.example.chatcircle.presentation.auth.AuthUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLoginBinding.bind(view)

        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {

        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text
                ?.toString()
                ?.trim()
                .orEmpty()

            val password = binding.etPassword.text
                ?.toString()
                .orEmpty()

            viewModel.login(
                email = email,
                password = password
            )
        }

        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_registerFragment
            )
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
                            binding.btnLogin.isEnabled = true
                        }

                        AuthUiState.Loading -> {
                            binding.btnLogin.isEnabled = false
                        }

                        is AuthUiState.Success -> {
                            binding.btnLogin.isEnabled = true

                            Toast.makeText(
                                requireContext(),
                                "Login successful",
                                Toast.LENGTH_SHORT
                            ).show()

                            // We'll navigate to Home later.
                        }

                        is AuthUiState.Error -> {
                            binding.btnLogin.isEnabled = true

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
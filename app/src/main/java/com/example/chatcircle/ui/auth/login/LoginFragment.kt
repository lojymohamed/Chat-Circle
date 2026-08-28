package com.example.chatcircle.ui.auth.login

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
import com.example.chatcircle.ui.auth.login.LoginViewModel
import com.example.chatcircle.ui.auth.AuthUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.example.chatcircle.ui.common.focusWithoutKeyboard

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var credentialManager: CredentialManager

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLoginBinding.bind(view)

        credentialManager =
            CredentialManager.create(requireContext())

        // Email starts focused so the field reads as ready, but the keyboard
        // stays down until the user actually taps it.
        binding.etEmail.focusWithoutKeyboard()

        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

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

                            findNavController().navigate(
                                R.id.action_loginFragment_to_chatRoomFragment
                            )
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

    private fun signInWithGoogle() {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val googleIdOption =
                    GetGoogleIdOption.Builder()
                        .setServerClientId(
                            getString(R.string.default_web_client_id)
                        )
                        .setFilterByAuthorizedAccounts(false)
                        .build()

                val request =
                    GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                val result = credentialManager.getCredential(
                    context = requireContext(),
                    request = request
                )

                handleGoogleCredential(result.credential)

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    e.message ?: "Google sign-in failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleGoogleCredential(
        credential: androidx.credentials.Credential
    ) {

        if (
            credential is CustomCredential &&
            credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {

            try {

                val googleIdTokenCredential =
                    GoogleIdTokenCredential
                        .createFrom(credential.data)

                val idToken =
                    googleIdTokenCredential.idToken

                viewModel.loginWithGoogle(idToken)

            } catch (e: GoogleIdTokenParsingException) {

                Toast.makeText(
                    requireContext(),
                    "Unable to process Google account",
                    Toast.LENGTH_LONG
                ).show()
            }

        } else {

            Toast.makeText(
                requireContext(),
                "Unexpected Google credential",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
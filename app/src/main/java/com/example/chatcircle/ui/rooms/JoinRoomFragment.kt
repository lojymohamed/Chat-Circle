package com.example.chatcircle.ui.rooms

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.util.Log
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
import com.example.chatcircle.R
import com.example.chatcircle.databinding.FragmentJoinRoomBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private const val TAG = "CC_JoinRoom"

/**
 * Join a room by typing its code, or by scanning the code from someone else.
 *
 * This screen only joins. Creating a room stays on the room list, and the link
 * at the bottom goes back there - keeping the two apart means neither screen
 * has to explain which of two things a single button will do.
 */
@AndroidEntryPoint
class JoinRoomFragment : Fragment() {

    private var _binding: FragmentJoinRoomBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatRoomViewModel by viewModels()

    /**
     * Must be registered at construction, not in a lifecycle callback, or the
     * Activity Result API throws when it tries to restore a pending result.
     */
    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        val scanned = result.contents
        Log.d(TAG, "scanLauncher() called: gotResult=${scanned != null}")

        if (scanned.isNullOrBlank()) return@registerForActivityResult
        // Guard against the result arriving after the view is gone.
        val safeBinding = _binding ?: return@registerForActivityResult

        val code = extractRoomCode(scanned)
        safeBinding.etRoomCode.setText(code)
        safeBinding.etRoomCode.setSelection(code.length)
        viewModel.joinRoom(code)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView() called")
        _binding = FragmentJoinRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated() called")

        setupListeners()
        observeUiState()
        observeNavigation()
    }

    private fun setupListeners() {
        Log.d(TAG, "setupListeners() called")

        binding.btnBack.setOnClickListener {
            Log.d(TAG, "setupListeners(): back")
            findNavController().popBackStack()
        }

        binding.tvCreateInstead.setOnClickListener {
            Log.d(TAG, "setupListeners(): create instead, returning to the room list")
            findNavController().popBackStack()
        }

        binding.btnScan.setOnClickListener {
            Log.i(TAG, "setupListeners(): launching scanner")
            scanLauncher.launch(
                ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt(getString(R.string.join_scan_prompt))
                    setBeepEnabled(false)
                    setOrientationLocked(false)
                }
            )
        }

        binding.btnEnter.setOnClickListener { submitCode() }
    }

    private fun submitCode() {
        val code = binding.etRoomCode.text?.toString()?.trim()?.uppercase().orEmpty()
        Log.d(TAG, "submitCode() called: length=${code.length}")

        if (code.isEmpty()) {
            Toast.makeText(requireContext(), R.string.join_code_empty, Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.joinRoom(code)
    }

    /**
     * Pulls a room code out of whatever the QR actually contained.
     *
     * A code may be shared as bare text or wrapped in a link, so anything up to
     * and including the last slash is dropped before the result is normalised.
     */
    private fun extractRoomCode(scanned: String): String {
        val trimmed = scanned.trim()
        val code = trimmed.substringAfterLast('/').trim().uppercase()
        Log.d(TAG, "extractRoomCode() called: rawLength=${trimmed.length}, codeLength=${code.length}")
        return code
    }

    private fun observeUiState() {
        Log.d(TAG, "observeUiState() called")

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ChatRoomUiState.Loading -> setBusy(true)

                        is ChatRoomUiState.Error -> {
                            setBusy(false)
                            Log.w(TAG, "observeUiState(): join failed - ${state.message}")
                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        else -> setBusy(false)
                    }
                }
            }
        }
    }

    private fun observeNavigation() {
        Log.d(TAG, "observeNavigation() called")

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { event ->
                    if (event is ChatRoomNavigationEvent.NavigateToChatRoom) {
                        Log.i(TAG, "observeNavigation(): joined, opening room ${event.roomId}")
                        findNavController().navigate(
                            JoinRoomFragmentDirections.actionJoinRoomFragmentToChatFragment(
                                roomId = event.roomId,
                                roomName = event.roomName
                            )
                        )
                    }
                }
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        Log.d(TAG, "setBusy() called: busy=$busy")
        binding.loadingSpinner.visibility = if (busy) View.VISIBLE else View.GONE
        binding.btnEnter.isEnabled = !busy
    }

    /**
     * Kicks off the sweeping line inside the scan icon.
     *
     * An AnimatedVectorDrawable does not play on its own - it has to be told
     * to start, and it stops when the view goes away, so this is tied to
     * onResume rather than being fired once at inflation.
     */
    private fun startQrAnimation() {
        val drawable = binding.btnScan.drawable
        Log.d(TAG, "startQrAnimation() called: animatable=${drawable is Animatable}")
        (drawable as? Animatable)?.start()
    }

    private fun stopQrAnimation() {
        val drawable = binding.btnScan.drawable
        Log.d(TAG, "stopQrAnimation() called")
        (drawable as? Animatable)?.stop()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")
        startQrAnimation()
    }

    override fun onPause() {
        Log.d(TAG, "onPause() called")
        stopQrAnimation()
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView() called")
        _binding = null
        super.onDestroyView()
    }
}

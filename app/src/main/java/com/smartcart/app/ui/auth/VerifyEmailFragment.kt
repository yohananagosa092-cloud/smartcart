package com.smartcart.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartcart.app.R
import com.smartcart.app.viewmodel.auth.VerifyEmailViewModel
import com.smartcart.app.viewmodel.auth.VerifyOutcome

class VerifyEmailFragment : Fragment(R.layout.fragment_verify_email) {
    private val viewModel: VerifyEmailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val email = view.findViewById<TextView>(R.id.auth_verify_email)
        val verify = view.findViewById<View>(R.id.auth_submit)
        val resend = view.findViewById<TextView>(R.id.auth_resend)
        val change = view.findViewById<View>(R.id.auth_alternate)
        val masked = BidiFormatter.getInstance(true)
            .unicodeWrap(viewModel.maskedEmail(), TextDirectionHeuristicsCompat.LTR)
        email.text = getString(R.string.auth_verify_masked, masked)

        verify.setOnClickListener { viewModel.verify() }
        resend.setOnClickListener { viewModel.resend() }
        change.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.auth_verify_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.auth_verify_confirm_delete) { _, _ -> viewModel.changeEmail() }
                .show()
        }

        viewModel.busy.observe(viewLifecycleOwner) { busy ->
            verify.isEnabled = !busy
            change.isEnabled = !busy
            resend.isEnabled = !busy && viewModel.remaining.value == 0L
        }
        viewModel.remaining.observe(viewLifecycleOwner) { seconds ->
            resend.isEnabled = viewModel.busy.value != true && seconds == 0L
            resend.text = if (seconds > 0) {
                getString(R.string.auth_verify_countdown, "00:" + seconds.toString().padStart(2, '0'))
            } else {
                getString(R.string.auth_verify_resend)
            }
        }
        viewModel.outcome.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            viewModel.consumeOutcome()
            result.onSuccess {
                when (it) {
                    VerifyOutcome.VERIFIED -> findNavController().navigate(R.id.action_verifyEmail_to_home)
                    VerifyOutcome.DELETED -> findNavController().navigate(R.id.action_verifyEmail_to_createAccount)
                    VerifyOutcome.PENDING -> Snackbar.make(view, R.string.auth_verify_pending, Snackbar.LENGTH_LONG).show()
                    VerifyOutcome.SENT -> Snackbar.make(view, R.string.auth_verify_sent, Snackbar.LENGTH_LONG).show()
                }
            }.onFailure {
                Snackbar.make(view, it.authErrorMessage(), Snackbar.LENGTH_LONG).show()
            }
        }
    }
}

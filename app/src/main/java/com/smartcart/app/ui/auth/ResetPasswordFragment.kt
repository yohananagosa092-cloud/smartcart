package com.smartcart.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.smartcart.app.R
import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import com.smartcart.app.viewmodel.auth.ResetPasswordViewModel

class ResetPasswordFragment : Fragment(R.layout.fragment_reset_password) {
    private val viewModel: ResetPasswordViewModel by viewModels()
    private var confirmationShown = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.auth_header_back).setOnClickListener { findNavController().popBackStack() }
        val email = view.findViewById<TextInputLayout>(R.id.auth_email_field)
        val submit = view.findViewById<View>(R.id.auth_submit)
        val back = view.findViewById<View>(R.id.auth_alternate)
        val done = view.findViewById<View>(R.id.auth_reset_done)
        val label = view.findViewById<View>(R.id.auth_reset_email_label)
        val icon = view.findViewById<View>(R.id.auth_reset_confirmation_icon)
        fun showConfirmation() {
            confirmationShown = true
            listOf(email, submit, back, label).forEach { it.isVisible = false }
            icon.isVisible = true
            done.isVisible = true
            view.findViewById<TextView>(R.id.auth_reset_heading).setText(R.string.auth_verify_heading)
            view.findViewById<TextView>(R.id.auth_reset_intro).setText(R.string.auth_reset_sent)
        }
        if (savedInstanceState?.getBoolean("confirmationShown") == true) showConfirmation()
        done.setOnClickListener { findNavController().popBackStack() }
        submit.setOnClickListener {
            email.error = null
            viewModel.reset(email.editText?.text.toString())
        }
        back.setOnClickListener { findNavController().popBackStack() }
        viewModel.busy.observe(viewLifecycleOwner) { busy ->
            submit.isEnabled = !busy
            back.isEnabled = !busy
        }
        viewModel.outcome.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            viewModel.consumeOutcome()
            result.onSuccess {
                showConfirmation()
            }.onFailure {
                if ((it as? AppException)?.code == ErrorCode.INVALID_EMAIL) {
                    email.error = getString(it.authErrorMessage())
                } else {
                    Snackbar.make(view, it.authErrorMessage(), Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("confirmationShown", confirmationShown)
    }
}

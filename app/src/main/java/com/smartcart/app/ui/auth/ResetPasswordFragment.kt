package com.smartcart.app.ui.auth

import android.os.Bundle
import android.view.View
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.auth_header_back).setOnClickListener { findNavController().popBackStack() }
        val email = view.findViewById<TextInputLayout>(R.id.auth_email_field)
        val submit = view.findViewById<View>(R.id.auth_submit)
        val back = view.findViewById<View>(R.id.auth_alternate)
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
                Snackbar.make(view, R.string.auth_reset_sent, Snackbar.LENGTH_LONG).show()
            }.onFailure {
                if ((it as? AppException)?.code == ErrorCode.INVALID_EMAIL) {
                    email.error = getString(it.authErrorMessage())
                } else {
                    Snackbar.make(view, it.authErrorMessage(), Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}

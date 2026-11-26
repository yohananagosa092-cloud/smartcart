package com.smartcart.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.smartcart.app.R
import com.smartcart.app.data.AppException
import com.smartcart.app.data.ErrorCode
import com.smartcart.app.viewmodel.auth.SignInViewModel

class SignInFragment : Fragment(R.layout.fragment_sign_in) {
    private val viewModel: SignInViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val email = view.findViewById<TextInputLayout>(R.id.auth_email_field)
        val password = view.findViewById<TextInputLayout>(R.id.auth_password_field)
        val submit = view.findViewById<View>(R.id.auth_submit)
        val alternate = view.findViewById<View>(R.id.auth_alternate)
        val forgot = view.findViewById<View>(R.id.auth_forgot)
        val errorCard = view.findViewById<View>(R.id.auth_sign_in_error_card)
        submit.setOnClickListener {
            email.error = null
            errorCard.isVisible = false
            viewModel.signIn(email.editText?.text.toString(), password.editText?.text.toString())
        }
        alternate.setOnClickListener {
            findNavController().navigate(R.id.action_signIn_to_createAccount)
        }
        forgot.setOnClickListener {
            findNavController().navigate(R.id.action_signIn_to_resetPassword)
        }
        viewModel.busy.observe(viewLifecycleOwner) { busy ->
            submit.isEnabled = !busy
            alternate.isEnabled = !busy
            forgot.isEnabled = !busy
        }
        viewModel.outcome.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            viewModel.consumeOutcome()
            result.onSuccess { user ->
                password.editText?.text?.clear()
                val action = if (user.isEmailVerified) R.id.action_signIn_to_home
                    else R.id.action_signIn_to_verifyEmail
                findNavController().navigate(action)
            }.onFailure { exception ->
                val message = getString(exception.authErrorMessage())
                listOf(R.id.auth_sign_in_brand, R.id.auth_sign_in_welcome, R.id.auth_sign_in_introduction).forEach {
                    view.findViewById<View>(it).isVisible = false
                }
                if ((exception as? AppException)?.code == ErrorCode.INVALID_EMAIL) {
                    email.error = message
                } else {
                    view.findViewById<TextView>(R.id.auth_sign_in_error_text).text = message
                    errorCard.isVisible = true
                }
            }
        }
        viewModel.restoreSession()
    }
}

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
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.smartcart.app.viewmodel.auth.CreateAccountViewModel

class CreateAccountFragment : Fragment(R.layout.fragment_create_account) {
    private val viewModel: CreateAccountViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.auth_header_back).setOnClickListener { findNavController().popBackStack() }
        val name = view.findViewById<TextInputLayout>(R.id.auth_name_field)
        val email = view.findViewById<TextInputLayout>(R.id.auth_email_field)
        val password = view.findViewById<TextInputLayout>(R.id.auth_password_field)
        val strengthBar = view.findViewById<LinearProgressIndicator>(R.id.auth_strength_bar)
        val strengthText = view.findViewById<TextView>(R.id.auth_strength_text)
        val terms = view.findViewById<MaterialCheckBox>(R.id.auth_terms_check)
        val termsValidation = view.findViewById<View>(R.id.auth_terms_validation)
        val submit = view.findViewById<View>(R.id.auth_submit)
        val alternate = view.findViewById<View>(R.id.auth_alternate)
        terms.setOnCheckedChangeListener { _, checked ->
            if (checked) termsValidation.isVisible = false
        }

        val strengthMessages = listOf(
            R.string.auth_strength_weak,
            R.string.auth_strength_good,
            R.string.auth_strength_strong,
        )
        strengthText.isVisible = false
        password.editText?.doAfterTextChanged { value ->
            val strength = viewModel.passwordStrength(value.toString())
            strengthBar.progress = strength
            strengthText.isVisible = strength > 0
            if (strength > 0) strengthText.setText(strengthMessages[strength - 1])
        }
        submit.setOnClickListener {
            name.error = null
            email.error = null
            password.error = null
            if (terms.isChecked) {
                viewModel.register(
                    name.editText?.text.toString(),
                    email.editText?.text.toString(),
                    password.editText?.text.toString(),
                    acceptedTerms = true,
                )
            } else {
                termsValidation.isVisible = true
            }
        }
        alternate.setOnClickListener { findNavController().popBackStack() }
        viewModel.busy.observe(viewLifecycleOwner) { busy ->
            submit.isEnabled = !busy
            alternate.isEnabled = !busy
        }
        viewModel.outcome.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            viewModel.consumeOutcome()
            result.onSuccess {
                password.editText?.text?.clear()
                findNavController().navigate(R.id.action_createAccount_to_verifyEmail)
            }.onFailure { exception ->
                val message = getString(exception.authErrorMessage())
                when ((exception as? AppException)?.code) {
                    ErrorCode.INVALID_NAME -> name.error = message
                    ErrorCode.INVALID_EMAIL, ErrorCode.EMAIL_IN_USE -> email.error = message
                    ErrorCode.WEAK_PASSWORD -> password.error = message
                    else -> Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}

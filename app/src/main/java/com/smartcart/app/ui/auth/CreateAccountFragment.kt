package com.smartcart.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartcart.app.R
import com.smartcart.app.viewmodel.auth.CreateAccountViewModel

/** מסך 02 · הרשמה. שלד: הכותרת מגיעה מה-ViewModel, והכפתורים מנווטים לפי גרף הניווט. */
class CreateAccountFragment : Fragment(R.layout.fragment_create_account) {

    private val viewModel: CreateAccountViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.screen_title)
        viewModel.title.observe(viewLifecycleOwner) { title.text = it }

        view.findViewById<View>(R.id.btn_to_verify_email).setOnClickListener {
            findNavController().navigate(R.id.action_createAccount_to_verifyEmail)
        }
    }
}

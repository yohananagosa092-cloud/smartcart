package com.smartcart.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.smartcart.app.R
import com.smartcart.app.viewmodel.auth.ResetPasswordViewModel

/** מסך 04 · איפוס סיסמה. שלד: הכותרת מגיעה מה-ViewModel, והכפתורים מנווטים לפי גרף הניווט. */
class ResetPasswordFragment : Fragment(R.layout.fragment_reset_password) {

    private val viewModel: ResetPasswordViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.screen_title)
        viewModel.title.observe(viewLifecycleOwner) { title.text = it }

    }
}

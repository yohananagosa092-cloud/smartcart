package com.smartcart.app.ui.sharing

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartcart.app.R
import com.smartcart.app.viewmodel.sharing.MembersViewModel

/** מסך 17 · ניהול משתתפות. שלד: הכותרת מגיעה מה-ViewModel, והכפתורים מנווטים לפי גרף הניווט. */
class MembersFragment : Fragment(R.layout.fragment_members) {

    private val viewModel: MembersViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.screen_title)
        viewModel.title.observe(viewLifecycleOwner) { title.text = it }

        view.findViewById<View>(R.id.btn_to_share_list).setOnClickListener {
            findNavController().navigate(R.id.action_members_to_shareList)
        }
    }
}

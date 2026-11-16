package com.smartcart.app.ui.sharing

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartcart.app.R
import com.smartcart.app.viewmodel.sharing.JoinListViewModel

/** מסך 15 · הצטרפות לרשימה. שלד: הכותרת מגיעה מה-ViewModel, והכפתורים מנווטים לפי גרף הניווט. */
class JoinListFragment : Fragment(R.layout.fragment_join_list) {

    private val viewModel: JoinListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.screen_title)
        viewModel.title.observe(viewLifecycleOwner) { title.text = it }

        view.findViewById<View>(R.id.btn_to_list_editor).setOnClickListener {
            findNavController().navigate(R.id.action_joinList_to_listEditor)
        }
    }
}

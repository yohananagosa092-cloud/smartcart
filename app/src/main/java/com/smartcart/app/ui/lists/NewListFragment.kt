package com.smartcart.app.ui.lists

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartcart.app.R
import com.smartcart.app.viewmodel.lists.NewListViewModel

/** מסך 06 · רשימה חדשה. שלד: הכותרת מגיעה מה-ViewModel, והכפתורים מנווטים לפי גרף הניווט. */
class NewListFragment : Fragment(R.layout.fragment_new_list) {

    private val viewModel: NewListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.screen_title)
        viewModel.title.observe(viewLifecycleOwner) { title.text = it }

        view.findViewById<View>(R.id.btn_to_list_editor).setOnClickListener {
            findNavController().navigate(R.id.action_newList_to_listEditor)
        }
    }
}

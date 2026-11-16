package com.smartcart.app.ui.lists

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartcart.app.R
import com.smartcart.app.viewmodel.lists.HomeViewModel

/** מסך 05 · מסך הבית. שלד: הכותרת מגיעה מה-ViewModel, והכפתורים מנווטים לפי גרף הניווט. */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.screen_title)
        viewModel.title.observe(viewLifecycleOwner) { title.text = it }

        view.findViewById<View>(R.id.btn_to_new_list).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_newList)
        }
        view.findViewById<View>(R.id.btn_to_my_lists).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_myLists)
        }
        view.findViewById<View>(R.id.btn_to_list_editor).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_listEditor)
        }
        view.findViewById<View>(R.id.btn_to_shared_lists).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_sharedLists)
        }
        view.findViewById<View>(R.id.btn_to_profile).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }
    }
}

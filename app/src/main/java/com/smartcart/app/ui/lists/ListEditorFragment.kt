package com.smartcart.app.ui.lists

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartcart.app.R
import com.smartcart.app.viewmodel.lists.ListEditorViewModel

/** מסך 08 · עריכת רשימה. שלד: הכותרת מגיעה מה-ViewModel, והכפתורים מנווטים לפי גרף הניווט. */
class ListEditorFragment : Fragment(R.layout.fragment_list_editor) {

    private val viewModel: ListEditorViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.screen_title)
        viewModel.title.observe(viewLifecycleOwner) { title.text = it }

        view.findViewById<View>(R.id.btn_to_add_products).setOnClickListener {
            findNavController().navigate(R.id.action_listEditor_to_addProducts)
        }
        view.findViewById<View>(R.id.btn_to_share_list).setOnClickListener {
            findNavController().navigate(R.id.action_listEditor_to_shareList)
        }
        view.findViewById<View>(R.id.btn_to_members).setOnClickListener {
            findNavController().navigate(R.id.action_listEditor_to_members)
        }
        view.findViewById<View>(R.id.btn_to_comparison_type).setOnClickListener {
            findNavController().navigate(R.id.action_listEditor_to_comparisonType)
        }
    }
}

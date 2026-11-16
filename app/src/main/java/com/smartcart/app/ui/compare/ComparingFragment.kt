package com.smartcart.app.ui.compare

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartcart.app.R
import com.smartcart.app.viewmodel.compare.ComparingViewModel

/** מסך 12 · חישוב ההשוואה. שלד: הכותרת מגיעה מה-ViewModel, והכפתורים מנווטים לפי גרף הניווט. */
class ComparingFragment : Fragment(R.layout.fragment_comparing) {

    private val viewModel: ComparingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.screen_title)
        viewModel.title.observe(viewLifecycleOwner) { title.text = it }

        view.findViewById<View>(R.id.btn_to_results).setOnClickListener {
            findNavController().navigate(R.id.action_comparing_to_results)
        }
    }
}

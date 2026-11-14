package com.smartcart.app.ui.compare

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartcart.app.R
import com.smartcart.app.viewmodel.compare.ComparisonTypeViewModel

/** מסך 11 · סוג ההשוואה. שלד: הכותרת מגיעה מה-ViewModel, והכפתורים מנווטים לפי גרף הניווט. */
class ComparisonTypeFragment : Fragment(R.layout.fragment_comparison_type) {

    private val viewModel: ComparisonTypeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.screen_title)
        viewModel.title.observe(viewLifecycleOwner) { title.text = it }

        view.findViewById<View>(R.id.btn_to_comparing).setOnClickListener {
            findNavController().navigate(R.id.action_comparisonType_to_comparing)
        }
    }
}

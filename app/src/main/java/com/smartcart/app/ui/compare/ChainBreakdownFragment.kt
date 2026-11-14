package com.smartcart.app.ui.compare

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.smartcart.app.R
import com.smartcart.app.viewmodel.compare.ChainBreakdownViewModel

/** מסך 14 · פירוט לפי רשת. שלד: הכותרת מגיעה מה-ViewModel, והכפתורים מנווטים לפי גרף הניווט. */
class ChainBreakdownFragment : Fragment(R.layout.fragment_chain_breakdown) {

    private val viewModel: ChainBreakdownViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.screen_title)
        viewModel.title.observe(viewLifecycleOwner) { title.text = it }

    }
}

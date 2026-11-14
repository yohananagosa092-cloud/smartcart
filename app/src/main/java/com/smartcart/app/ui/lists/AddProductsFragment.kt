package com.smartcart.app.ui.lists

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.smartcart.app.R
import com.smartcart.app.viewmodel.lists.AddProductsViewModel

/** מסך 07 · הוספת מוצרים. שלד: הכותרת מגיעה מה-ViewModel, והכפתורים מנווטים לפי גרף הניווט. */
class AddProductsFragment : Fragment(R.layout.fragment_add_products) {

    private val viewModel: AddProductsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.screen_title)
        viewModel.title.observe(viewLifecycleOwner) { title.text = it }

    }
}

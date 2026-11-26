package com.smartcart.app.ui.lists

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.smartcart.app.R
import com.smartcart.app.data.ServiceLocator
import kotlinx.coroutines.launch

class ListActionFragment : Fragment(R.layout.fragment_list_action) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val listId = arguments?.getString("listId").orEmpty()
        val deleting = arguments?.getBoolean("delete") == true
        val repository = ServiceLocator.listRepository
        val name = view.findViewById<TextInputLayout>(R.id.list_action_name_layout)
        val heading = view.findViewById<TextView>(R.id.list_action_heading)
        val warning = view.findViewById<View>(R.id.list_action_warning)
        val confirm = view.findViewById<MaterialButton>(R.id.list_action_confirm)
        val cancel = view.findViewById<View>(R.id.list_action_cancel)
        val back = view.findViewById<View>(R.id.list_back)

        view.findViewById<TextView>(R.id.list_action_title).setText(
            if (deleting) R.string.list_delete else R.string.list_rename_title,
        )
        confirm.setText(if (deleting) R.string.list_delete else R.string.list_save_name)
        if (deleting) {
            confirm.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.sc_warn_coral)
        }
        listOf(back, cancel).forEach { it.setOnClickListener { findNavController().popBackStack() } }
        name.editText?.doAfterTextChanged { name.error = null }

        viewLifecycleOwner.lifecycleScope.launch {
            val list = repository.getList(listId).getOrNull()
            if (list == null || !repository.isOwner(list)) {
                heading.setText(R.string.list_missing)
                heading.isVisible = true
                return@launch
            }
            heading.text = getString(R.string.list_delete_title, list.name)
            heading.isVisible = deleting
            warning.isVisible = deleting
            cancel.isVisible = deleting
            name.isVisible = !deleting
            name.helperText = getString(R.string.sc_name_length)
            if (savedInstanceState == null) name.editText?.setText(list.name)
            confirm.isEnabled = true
            confirm.setOnClickListener {
                val enteredName = name.editText?.text.toString()
                if (!deleting && enteredName.isBlank()) {
                    name.error = getString(R.string.list_name_invalid)
                    return@setOnClickListener
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    confirm.isEnabled = false
                    val saved = if (deleting) repository.deleteList(listId).getOrDefault(false)
                        else repository.renameList(listId, enteredName).isSuccess
                    if (saved) {
                        if (deleting) findNavController().popBackStack(R.id.listEditorFragment, true)
                        else findNavController().popBackStack()
                    } else {
                        Snackbar.make(view, R.string.list_write_error, Snackbar.LENGTH_LONG).show()
                        confirm.isEnabled = true
                    }
                }
            }
        }
    }
}

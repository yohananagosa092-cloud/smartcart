package com.smartcart.app.ui.lists

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.smartcart.app.R
import com.smartcart.app.data.ServiceLocator
import kotlinx.coroutines.launch

class NewListFragment : Fragment(R.layout.fragment_new_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name = view.findViewById<EditText>(R.id.new_list_name)
        val create = view.findViewById<View>(R.id.new_list_create)
        view.findViewById<View>(R.id.list_back).setOnClickListener { findNavController().popBackStack() }
        create.setOnClickListener {
            val value = name.text.toString().trim()
            if (value.length !in 1..40) {
                name.error = getString(R.string.list_name_invalid)
                return@setOnClickListener
            }
            create.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                ServiceLocator.listRepository.createList(value).onSuccess { list ->
                    findNavController().navigate(R.id.action_newList_to_listEditor, bundleOf("listId" to list.id))
                }.onFailure {
                    create.isEnabled = true
                    Snackbar.make(view, R.string.list_write_error, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}

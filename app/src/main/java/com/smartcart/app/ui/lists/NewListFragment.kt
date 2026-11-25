package com.smartcart.app.ui.lists

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.smartcart.app.R
import com.smartcart.app.viewmodel.lists.NewListViewModel
import kotlinx.coroutines.launch

class NewListFragment : Fragment(R.layout.fragment_new_list) {
    private val model: NewListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name = view.findViewById<EditText>(R.id.new_list_name)
        val create = view.findViewById<View>(R.id.new_list_create)
        view.findViewById<View>(R.id.list_back).setOnClickListener { findNavController().popBackStack() }
        val hints = mapOf(R.id.new_monthly to R.string.sc_monthly, R.id.new_weekend to R.string.sc_weekend,
            R.id.new_passover to R.string.sc_passover, R.id.new_hosting to R.string.sc_hosting)
        hints.forEach { (id, label) ->
            view.findViewById<View>(id).setOnClickListener {
                name.setText(label)
                name.setSelection(name.length())
            }
        }
        create.setOnClickListener {
            val value = name.text.toString().trim()
            if (value.length !in 1..40) {
                name.error = getString(R.string.list_name_invalid)
                return@setOnClickListener
            }
            create.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                model.createList(value).onSuccess { list ->
                    val args = Bundle().apply { putString("listId", list.id) }
                    findNavController().navigate(R.id.action_newList_to_listEditor, args)
                }.onFailure { failure ->
                    create.isEnabled = true
                    Snackbar.make(view, failure.listErrorMessage(
                        R.string.list_write_error, R.string.sc_list_create_permission,
                    ), Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}

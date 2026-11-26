package com.smartcart.app.ui.lists

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartcart.app.R
import com.smartcart.app.ui.components.QuantitySelectorView
import com.smartcart.app.ui.components.SwipeRevealCard
import com.smartcart.app.viewmodel.lists.ListEditorViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

class ListEditorFragment : Fragment(R.layout.fragment_list_editor) {
    private val model: ListEditorViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getString("listId").orEmpty()
        val title = view.findViewById<TextView>(R.id.list_title)
        val status = view.findViewById<TextView>(R.id.editor_status)
        val rows = view.findViewById<LinearLayout>(R.id.editor_items)
        val emptyDetail = view.findViewById<View>(R.id.editor_empty_detail)
        val share = view.findViewById<View>(R.id.editor_share)
        val menu = view.findViewById<View>(R.id.editor_menu)
        val actions = view.findViewById<View>(R.id.editor_actions)
        val members = view.findViewById<TextView>(R.id.editor_members)
        val add = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.editor_add)
        val compare = view.findViewById<View>(R.id.editor_compare)
        val budget = view.findViewById<View>(R.id.editor_budget)
        val budgetLabel = view.findViewById<TextView>(R.id.editor_budget_label)
        val budgetLeft = view.findViewById<TextView>(R.id.editor_budget_left)
        var namedMembers: Pair<List<String>, String>? = null

        fun navigate(action: Int) = findNavController().navigate(action, bundleOf("listId" to id))
        fun showListAction(delete: Boolean) {
            actions.isVisible = false
            findNavController().navigate(R.id.action_listEditor_to_listAction, Bundle().apply {
                putString("listId", id)
                putBoolean("delete", delete)
            })
        }
        fun unavailable() {
            title.setText(R.string.list_editor_title)
            title.setOnClickListener(null)
            status.isVisible = true
            status.setText(R.string.list_missing)
            emptyDetail.isVisible = false
            rows.removeAllViews()
            listOf(share, menu, actions, members, budget).forEach { it.isVisible = false }
            add.isEnabled = false
            compare.isEnabled = false
        }

        view.findViewById<View>(R.id.list_back).setOnClickListener { findNavController().popBackStack() }
        listOf(share, menu, members).forEach { it.isVisible = false }
        add.isEnabled = false
        compare.isEnabled = false
        if (id.isBlank()) {
            unavailable()
            return
        }

        add.setOnClickListener { navigate(R.id.action_listEditor_to_addProducts) }
        share.setOnClickListener { navigate(R.id.action_listEditor_to_shareList) }
        members.setOnClickListener { navigate(R.id.action_listEditor_to_members) }
        compare.setOnClickListener { navigate(R.id.action_listEditor_to_comparisonType) }
        budget.setOnClickListener { navigate(R.id.action_listEditor_to_budget) }
        menu.setOnClickListener {
            val state = model.state.value ?: return@setOnClickListener
            if (!state.owner || model.failed.value) return@setOnClickListener
            actions.isVisible = !actions.isVisible
        }
        view.findViewById<View>(R.id.editor_budget_action).setOnClickListener {
            actions.isVisible = false
            navigate(R.id.action_listEditor_to_budget)
        }
        view.findViewById<View>(R.id.editor_rename_action).setOnClickListener { showListAction(delete = false) }
        view.findViewById<View>(R.id.editor_delete_action).setOnClickListener { showListAction(delete = true) }

        model.observe(id)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.failed.collect { if (it) unavailable() }
                }
                launch {
                    model.state.collect { state ->
                        if (state == null || model.failed.value) return@collect
                        val list = state.list
                        if (list == null) {
                            unavailable()
                            return@collect
                        }
                        title.text = list.name
                        val itemsError = model.itemsError.value
                        val empty = itemsError == null && state.items.isEmpty()
                        status.isVisible = itemsError != null || empty
                        status.setTextAppearance(if (empty) R.style.TextAppearance_SmartCart_TitleLarge
                            else R.style.TextAppearance_SmartCart_Body)
                        status.setText(itemsError?.listErrorMessage(R.string.list_missing) ?: R.string.editor_empty_title)
                        emptyDetail.isVisible = empty
                        status.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            topMargin = if (empty) resources.getDimensionPixelSize(R.dimen.editor_empty_spacing) else 0
                        }
                        add.setText(if (empty) R.string.editor_add_first else R.string.list_add)
                        add.backgroundTintList = ContextCompat.getColorStateList(
                            requireContext(), if (empty) R.color.sc_button_background else android.R.color.transparent,
                        )
                        add.setTextColor(ContextCompat.getColor(
                            requireContext(), if (empty) R.color.sc_on_accent else R.color.sc_mint_primary,
                        ))
                        share.isVisible = state.owner
                        menu.isVisible = state.owner
                        if (!state.owner) actions.isVisible = false
                        members.isVisible = true
                        members.text = namedMembers?.takeIf { it.first == list.memberUids }?.second
                            ?: getString(R.string.list_members, list.memberUids.size)
                        title.setOnClickListener(if (state.owner) View.OnClickListener {
                            showListAction(delete = false)
                        } else null)
                        add.isEnabled = true
                        compare.isEnabled = itemsError == null && state.items.any { it.barcode.isNotBlank() }
                        budget.isVisible = list.lastBudget != null
                        list.lastBudget?.let { amount ->
                            budgetLabel.text = getString(R.string.list_budget, amount)
                            val left = list.lastCheapestTotal?.let { amount - it }
                            budgetLeft.isVisible = left != null
                            if (left != null) budgetLeft.text = getString(
                                if (left >= 0) R.string.list_budget_left else R.string.list_budget_over, abs(left),
                            )
                        }

                        rows.removeAllViews()
                        state.items.forEach { item ->
                            val row = layoutInflater.inflate(R.layout.list_item_row, rows, false)
                            row.findViewById<TextView>(R.id.list_row_name).text = item.name
                            val note = row.findViewById<TextView>(R.id.list_row_detail)
                            note.text = item.note.ifBlank { getString(R.string.list_note) }
                            note.setOnClickListener {
                                editText(R.string.list_edit_note, item.note) { value ->
                                    write(view) { model.note(id, item, value) }
                                }
                            }
                            val quantity = row.findViewById<QuantitySelectorView>(R.id.list_row_quantity)
                            quantity.value = item.quantity
                            quantity.onValueChanged = { value ->
                                quantity.isEnabled = false
                                write(view) {
                                    val updated = model.quantity(id, item, value)
                                    if (!updated) quantity.value = item.quantity
                                    quantity.isEnabled = true
                                    updated
                                }
                            }
                            val remove = row.findViewById<View>(R.id.list_row_remove)
                            remove.setOnClickListener {
                                confirm(R.string.list_remove_confirm, R.string.list_remove) {
                                    write(view) { model.remove(id, item) }
                                }
                            }
                            (row as SwipeRevealCard).onSwipe = { remove.isVisible = !remove.isVisible }
                            row.setOnLongClickListener {
                                remove.isVisible = true
                                true
                            }
                            rows.addView(row)
                        }
                    }
                }
                launch {
                    model.itemsError.collect { error ->
                        if (error != null && model.state.value?.list != null) {
                            rows.removeAllViews()
                            compare.isEnabled = false
                            status.setText(error.listErrorMessage(R.string.list_missing))
                            status.isVisible = true
                        }
                    }
                }
                launch {
                    var lastMembers: List<String>? = null
                    model.state.collect { state ->
                        val uids = state?.list?.memberUids ?: return@collect
                        if (uids == lastMembers) return@collect
                        lastMembers = uids
                        val initials = model.members(id).getOrNull().orEmpty()
                            .mapNotNull { it.displayName.trim().take(2).takeIf(String::isNotEmpty) }
                        if (initials.isNotEmpty() && model.state.value?.list?.memberUids == uids && !model.failed.value) {
                            val label = getString(R.string.list_members_named, initials.joinToString(" · "))
                            namedMembers = uids to label
                            members.text = label
                        }
                    }
                }
            }
        }
    }

    private fun write(view: View, operation: suspend () -> Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (!operation()) Snackbar.make(view, R.string.list_write_error, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun confirm(message: Int, action: Int, accept: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext()).setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(action) { _, _ -> accept() }.show()
    }

    private fun editText(title: Int, initial: String, save: (String) -> Unit) {
        val field = EditText(requireContext()).apply {
            setText(initial)
            setSelection(length())
        }
        MaterialAlertDialogBuilder(requireContext()).setTitle(title).setView(field)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.list_save) { _, _ -> save(field.text.toString()) }.show()
    }

}

package com.smartcart.app.ui.lists

import android.os.Bundle
import android.text.InputFilter
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
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
        val share = view.findViewById<View>(R.id.editor_share)
        val menu = view.findViewById<View>(R.id.editor_menu)
        val members = view.findViewById<TextView>(R.id.editor_members)
        val add = view.findViewById<View>(R.id.editor_add)
        val compare = view.findViewById<View>(R.id.editor_compare)
        val budget = view.findViewById<View>(R.id.editor_budget)
        val budgetLabel = view.findViewById<TextView>(R.id.editor_budget_label)
        val budgetLeft = view.findViewById<TextView>(R.id.editor_budget_left)
        var namedMembers: Pair<List<String>, String>? = null

        fun navigate(action: Int) = findNavController().navigate(action, bundleOf("listId" to id))
        fun unavailable() {
            title.setText(R.string.list_editor_title)
            title.setOnClickListener(null)
            status.isVisible = true
            status.setText(R.string.list_missing)
            rows.removeAllViews()
            listOf(share, menu, members, budget).forEach { it.isVisible = false }
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
        menu.setOnClickListener {
            val state = model.state.value ?: return@setOnClickListener
            val list = state.list ?: return@setOnClickListener
            if (!state.owner || model.failed.value) return@setOnClickListener
            PopupMenu(requireContext(), menu).apply {
                this.menu.add(0, 1, 0, R.string.list_rename)
                this.menu.add(0, 2, 1, R.string.list_delete)
                setOnMenuItemClickListener { item ->
                    if (item.itemId == 1) {
                        editText(R.string.list_rename, list.name, 40) { name ->
                            write(view) { model.rename(id, name) }
                        }
                    } else {
                        confirm(R.string.list_delete_confirm, R.string.list_delete) {
                            write(view) {
                                if (model.delete(id)) {
                                    findNavController().popBackStack()
                                    true
                                } else false
                            }
                        }
                    }
                    true
                }
            }.show()
        }

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
                        status.isVisible = itemsError != null || state.items.isEmpty()
                        status.setText(itemsError?.listErrorMessage(R.string.list_missing) ?: R.string.list_no_items)
                        share.isVisible = state.owner
                        menu.isVisible = state.owner
                        members.isVisible = true
                        members.text = namedMembers?.takeIf { it.first == list.memberUids }?.second
                            ?: getString(R.string.list_members, list.memberUids.size)
                        title.setOnClickListener(if (state.owner) View.OnClickListener {
                            editText(R.string.list_rename, list.name, 40) { name ->
                                write(view) { model.rename(id, name) }
                            }
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
                            addSwipeDelete(row) {
                                confirm(R.string.list_remove_confirm, R.string.list_remove) {
                                    write(view) { model.remove(id, item) }
                                }
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

    private fun editText(title: Int, initial: String, maxLength: Int? = null, save: (String) -> Unit) {
        val field = EditText(requireContext()).apply {
            setText(initial)
            setSelection(length())
            if (maxLength != null) filters = arrayOf(InputFilter.LengthFilter(maxLength))
        }
        MaterialAlertDialogBuilder(requireContext()).setTitle(title).setView(field)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.list_save) { _, _ -> save(field.text.toString()) }.show()
    }

    private fun addSwipeDelete(row: View, remove: () -> Unit) {
        val minSwipe = 64 * resources.displayMetrics.density
        val detector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true

            override fun onFling(start: MotionEvent?, end: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (start == null || abs(end.x - start.x) < minSwipe || abs(velocityX) <= abs(velocityY)) return false
                offerDelete()
                return true
            }

            private fun offerDelete() {
                PopupMenu(requireContext(), row).apply {
                    menu.add(R.string.list_remove)
                    setOnMenuItemClickListener { remove(); true }
                }.show()
            }

            override fun onLongPress(e: MotionEvent) = offerDelete()
        })
        row.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) row.performClick()
            detector.onTouchEvent(event)
        }
    }
}

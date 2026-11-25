package com.smartcart.app.ui.lists

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartcart.app.R
import com.smartcart.app.data.model.ShoppingList
import kotlinx.coroutines.launch

internal fun Fragment.openStoredList(
    button: View,
    action: Int,
    latestOnly: Boolean = false,
    load: suspend () -> Result<List<ShoppingList>>,
) {
    button.setOnClickListener {
        button.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val result = load()
            button.isEnabled = true
            result.onSuccess { lists ->
                fun open(list: ShoppingList) {
                    findNavController().navigate(action, bundleOf("listId" to list.id))
                }

                when {
                    lists.isEmpty() -> Snackbar.make(button, R.string.list_none, Snackbar.LENGTH_SHORT).show()
                    latestOnly -> open(lists.first())
                    else -> MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.list_select)
                        .setItems(lists.map { it.name }.toTypedArray()) { _, index -> open(lists[index]) }
                        .show()
                }
            }.onFailure {
                Snackbar.make(button, R.string.list_missing, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}

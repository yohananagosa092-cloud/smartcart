package com.smartcart.app.ui.lists

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.chip.Chip
import com.smartcart.app.R
import com.smartcart.app.data.ServiceLocator
import kotlinx.coroutines.launch

class BudgetFragment : Fragment(R.layout.fragment_budget) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getString("listId").orEmpty()
        val repository = ServiceLocator.listRepository
        val name = view.findViewById<TextView>(R.id.budget_list_name)
        val input = view.findViewById<TextInputLayout>(R.id.budget_input)
        val amount = view.findViewById<EditText>(R.id.budget_amount)
        val summary = view.findViewById<TextView>(R.id.budget_summary)
        val save = view.findViewById<View>(R.id.budget_save)
        val choices = mapOf(R.id.budget_150 to 150, R.id.budget_250 to 250, R.id.budget_400 to 400, R.id.budget_600 to 600)
        view.findViewById<View>(R.id.list_back).setOnClickListener { findNavController().popBackStack() }
        save.isEnabled = false
        amount.isEnabled = false
        val chips = choices.mapKeys { (button, _) -> view.findViewById<Chip>(button) }
        chips.keys.forEach {
            it.isCheckable = true
            it.isEnabled = false
        }
        amount.doAfterTextChanged { text ->
            input.error = null
            val value = text?.toString()?.toIntOrNull()
            chips.forEach { (chip, preset) -> chip.isChecked = value == preset }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val list = repository.getList(id).getOrNull()
            if (list == null) {
                name.setText(R.string.list_missing)
                return@launch
            }
            name.text = list.name
            list.lastBudget?.let { amount.setText(it.toString()) }
            val chain = list.lastCheapestChain
            val total = list.lastCheapestTotal
            if (chain != null && total != null) {
                summary.text = getString(R.string.budget_current_total, chain.displayName, total)
            }
            if (!repository.isOwner(list)) {
                input.helperText = getString(R.string.budget_not_owner)
                return@launch
            }
            amount.isEnabled = true
            save.isEnabled = true
            chips.forEach { (chip, value) ->
                chip.isEnabled = true
                chip.setOnClickListener { amount.setText(value.toString()) }
            }
            save.setOnClickListener {
                val value = amount.text.toString().toIntOrNull()
                if (value == null || value !in 1..10_000) {
                    input.error = getString(R.string.budget_range)
                    return@setOnClickListener
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    save.isEnabled = false
                    repository.updateBudget(id, value).onSuccess {
                        findNavController().popBackStack()
                    }.onFailure {
                        input.error = getString(R.string.list_write_error)
                        save.isEnabled = true
                    }
                }
            }
        }
    }
}

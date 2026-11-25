package com.smartcart.app.ui.lists

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.smartcart.app.R
import com.smartcart.app.viewmodel.lists.HomeViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val model: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name = model.displayName().trim()
        view.findViewById<TextView>(R.id.home_greeting).apply {
            if (name.isNotEmpty()) text = getString(R.string.sc_hello, name)
            isVisible = name.isNotEmpty()
        }
        val initials = name.split(' ').filter(String::isNotBlank).take(2).map { it.first() }.joinToString("")
        view.findViewById<TextView>(R.id.home_avatar).apply {
            text = initials
            isVisible = initials.isNotEmpty()
        }

        fun navigate(action: Int) = findNavController().navigate(action)
        view.findViewById<View>(R.id.btn_to_new_list).setOnClickListener { navigate(R.id.action_home_to_newList) }
        listOf(R.id.home_all_lists, R.id.btn_to_my_lists).forEach {
            view.findViewById<View>(it).setOnClickListener { navigate(R.id.action_home_to_myLists) }
        }
        view.findViewById<View>(R.id.btn_to_shared_lists).setOnClickListener { navigate(R.id.action_home_to_sharedLists) }
        view.findViewById<View>(R.id.btn_to_profile).setOnClickListener { navigate(R.id.action_home_to_profile) }

        val recent = view.findViewById<View>(R.id.btn_to_list_editor)
        recent.isEnabled = false
        view.findViewById<TextView>(R.id.home_recent_name).setText(R.string.list_loading)
        viewLifecycleOwner.lifecycleScope.launch {
            model.ownedLists().onSuccess { lists ->
                val shared = lists.count { it.memberUids.size > 1 }
                view.findViewById<TextView>(R.id.home_created).text = lists.size.toString()
                view.findViewById<TextView>(R.id.home_shared).text = shared.toString()
                view.findViewById<TextView>(R.id.home_private).text = (lists.size - shared).toString()
                val latest = lists.maxByOrNull { it.updatedAt }
                val title = view.findViewById<TextView>(R.id.home_recent_name)
                if (latest == null) {
                    title.setText(R.string.list_none)
                } else {
                    title.text = latest.name
                    val parts = mutableListOf(getString(R.string.sc_home_items, latest.itemCount))
                    val chain = latest.lastCheapestChain
                    val price = latest.lastCheapestTotal
                    if (chain != null && price != null) parts.add(getString(R.string.sc_home_price, chain.displayName, price))
                    view.findViewById<TextView>(R.id.home_recent_detail).text = parts.joinToString(" · ")
                    recent.isEnabled = true
                    recent.setOnClickListener {
                        val args = Bundle().apply { putString("listId", latest.id) }
                        findNavController().navigate(R.id.action_home_to_listEditor, args)
                    }
                }
            }.onFailure { failure ->
                val message = failure.listErrorMessage(R.string.sc_lists_loading_error)
                view.findViewById<TextView>(R.id.home_recent_name).setText(message)
                recent.isEnabled = true
                openStoredList(recent, R.id.action_home_to_listEditor, latestOnly = true) { model.ownedLists() }
            }
        }
    }
}

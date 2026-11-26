package com.smartcart.app.ui.sharing

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.smartcart.app.R
import com.smartcart.app.data.ServiceLocator
import kotlinx.coroutines.launch

class PendingInvitationFragment : Fragment(R.layout.fragment_pending_invitation) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val heading = view.findViewById<TextView>(R.id.invitation_heading)
        val accept = view.findViewById<View>(R.id.invitation_accept)
        val decline = view.findViewById<View>(R.id.invitation_decline)
        val repository = ServiceLocator.sharingRepository
        view.findViewById<View>(R.id.list_back).setOnClickListener { findNavController().popBackStack() }
        accept.isVisible = false
        decline.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            val token = arguments?.getString("token")
            val invitation = repository.pendingInvitations().getOrNull()?.firstOrNull { it.token == token }
            if (invitation == null) {
                heading.setText(R.string.invitation_unavailable)
                return@launch
            }
            heading.text = getString(R.string.invitation_heading, invitation.ownerName)
            view.findViewById<View>(R.id.invitation_details).isVisible = true
            view.findViewById<TextView>(R.id.invitation_description).text = getString(
                R.string.invitation_description, invitation.listName, invitation.itemCount,
                invitation.memberNames.joinToString(", "),
            )
            view.findViewById<TextView>(R.id.invitation_benefits).setText(R.string.invitation_benefits)
            accept.isVisible = true
            decline.isVisible = true
            fun respond(accepted: Boolean) = viewLifecycleOwner.lifecycleScope.launch {
                accept.isEnabled = false
                decline.isEnabled = false
                repository.respondToPendingInvitation(invitation.token, accepted).onSuccess { id ->
                    if (id == null) findNavController().popBackStack()
                    else findNavController().navigate(R.id.action_invitation_to_editor, bundleOf("listId" to id))
                }.onFailure {
                    Snackbar.make(view, R.string.invitation_unavailable, Snackbar.LENGTH_LONG).show()
                    accept.isEnabled = true
                    decline.isEnabled = true
                }
            }
            accept.setOnClickListener { respond(true) }
            decline.setOnClickListener { respond(false) }
        }
    }
}

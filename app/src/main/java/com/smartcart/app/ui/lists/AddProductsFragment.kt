package com.smartcart.app.ui.lists

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.smartcart.app.R
import com.smartcart.app.viewmodel.lists.AddProductsViewModel
import kotlinx.coroutines.launch

class AddProductsFragment : Fragment(R.layout.fragment_add_products) {
    private val model: AddProductsViewModel by viewModels()
    private var camera: BarcodeCamera? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        view?.let { if (it.findViewById<TabLayout>(R.id.product_tabs).selectedTabPosition == 1) showCamera(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val listId = arguments?.getString("listId").orEmpty()
        if (listId.isBlank()) {
            findNavController().popBackStack()
            return
        }
        val tabs = view.findViewById<TabLayout>(R.id.product_tabs)
        val query = view.findViewById<EditText>(R.id.product_query)
        val freeName = view.findViewById<EditText>(R.id.product_free_name)
        val manual = view.findViewById<EditText>(R.id.product_barcode)
        val results = view.findViewById<LinearLayout>(R.id.product_results)
        val status = view.findViewById<TextView>(R.id.product_status)
        val addFree = view.findViewById<View>(R.id.product_free_add)
        val fallback = view.findViewById<View>(R.id.product_fallback)
        val done = view.findViewById<View>(R.id.product_done)
        val back = view.findViewById<View>(R.id.list_back)

        query.doAfterTextChanged { if (tabs.selectedTabPosition == 0) model.search(it.toString()) }
        fallback.setOnClickListener {
            freeName.setText(query.text)
            tabs.selectTab(tabs.getTabAt(2))
            freeName.requestFocus()
        }
        back.setOnClickListener { findNavController().popBackStack() }
        done.setOnClickListener { findNavController().popBackStack() }
        view.findViewById<View>(R.id.product_camera_permission).setOnClickListener { requestCamera() }
        view.findViewById<View>(R.id.product_scan_again).setOnClickListener { showCamera(view) }
        view.findViewById<View>(R.id.product_barcode_lookup).setOnClickListener {
            val barcode = manual.text.toString().trim()
            if (barcode.matches(Regex("[0-9]+"))) {
                camera?.stop()
                view.findViewById<View>(R.id.product_preview).isVisible = false
                view.findViewById<View>(R.id.product_scan_again).isVisible = true
                model.scan(barcode)
            }
        }
        addFree.setOnClickListener {
            if (freeName.text.isNullOrBlank()) {
                freeName.error = getString(R.string.list_free_name)
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                if (model.addFree(listId, freeName.text.toString())) {
                    freeName.text?.clear()
                    Snackbar.make(view, R.string.list_write_ok, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(view, R.string.list_write_error, Snackbar.LENGTH_LONG).show()
                }
            }
        }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                view.findViewById<View>(R.id.product_search).isVisible = tab.position == 0
                view.findViewById<View>(R.id.product_camera).isVisible = tab.position == 1
                view.findViewById<View>(R.id.product_free).isVisible = tab.position == 2
                fallback.isVisible = tab.position == 0
                model.search(if (tab.position == 0) query.text.toString() else "")
                if (tab.position == 1) {
                    showCamera(view)
                    val prefs = requireContext().getSharedPreferences("smartcart_permissions", 0)
                    if (!hasCameraPermission() && !prefs.getBoolean("camera_requested", false)) {
                        prefs.edit().putBoolean("camera_requested", true).apply()
                        requestCamera()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                if (tab.position == 1) camera?.stop()
            }

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.results.collect { state ->
                        val message = when {
                            state.loading -> R.string.list_loading
                            state.failed -> R.string.list_lookup_error
                            state.empty -> R.string.list_not_found
                            state.products.isNotEmpty() -> R.string.list_results
                            else -> null
                        }
                        status.isVisible = message != null
                        message?.let(status::setText)
                        results.removeAllViews()
                        state.products.forEach { product ->
                            val row = layoutInflater.inflate(R.layout.list_product_row, results, false)
                            row.findViewById<TextView>(R.id.list_row_name).text = product.name
                            row.findViewById<TextView>(R.id.list_row_detail).text =
                                listOf(product.unitDescription, product.barcode).filter(String::isNotBlank).joinToString(" · ")
                            val add = row.findViewById<View>(R.id.list_row_add)
                            add.contentDescription = getString(R.string.list_add_product, product.name)
                            add.setOnClickListener {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    if (model.saving.value) return@launch
                                    val success = model.add(listId, product)
                                    Snackbar.make(view, if (success) R.string.list_write_ok else R.string.list_write_error,
                                        Snackbar.LENGTH_SHORT).show()
                                }
                            }
                            results.addView(row)
                        }
                    }
                }
                launch {
                    model.saving.collect { saving -> addFree.isEnabled = !saving }
                }
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestCamera() = permission.launch(Manifest.permission.CAMERA)

    private fun showCamera(view: View) {
        val preview = view.findViewById<PreviewView>(R.id.product_preview)
        val status = view.findViewById<TextView>(R.id.product_camera_status)
        val retry = view.findViewById<View>(R.id.product_camera_permission)
        val scanAgain = view.findViewById<View>(R.id.product_scan_again)
        val allowed = hasCameraPermission()
        preview.isVisible = allowed
        retry.isVisible = !allowed
        scanAgain.isVisible = false
        status.isVisible = !allowed
        status.setText(R.string.list_camera_permission)
        if (!allowed) return
        model.search("")
        val cameraSession = camera ?: BarcodeCamera(preview, viewLifecycleOwner, onBarcode = { barcode ->
            preview.isVisible = false
            scanAgain.isVisible = true
            model.scan(barcode)
        }, onError = {
            preview.isVisible = false
            status.isVisible = true
            status.setText(R.string.list_camera_error)
            scanAgain.isVisible = true
        }).also { camera = it }
        cameraSession.start()
    }

    override fun onDestroyView() {
        camera?.close()
        camera = null
        super.onDestroyView()
    }
}

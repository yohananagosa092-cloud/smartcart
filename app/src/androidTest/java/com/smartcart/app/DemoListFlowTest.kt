package com.smartcart.app

import android.Manifest
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.tabs.TabLayout
import com.smartcart.app.ui.components.QuantitySelectorView
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoListFlowTest {
    @Test
    fun createsListAndAddsCatalogAndFreeItems() {
        assumeFalse(BuildConfig.USE_FIRESTORE)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName, Manifest.permission.CAMERA,
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                if (host.navController.currentDestination?.id != R.id.homeFragment) {
                    host.navController.navigate(R.id.homeFragment)
                }
            }
            scenario.await { findViewById<TextView>(R.id.home_created)?.text?.isNotBlank() == true }
            capture("05-home")
            scenario.click(R.id.btn_to_new_list)
            scenario.await { findViewById<View>(R.id.new_list_create) != null }
            capture("06-new-list")
            scenario.click(R.id.new_weekend)
            scenario.click(R.id.new_list_create)
            scenario.await { findViewById<View>(R.id.editor_add)?.isEnabled == true }
            capture("08-empty-editor")
            scenario.click(R.id.editor_add)
            scenario.await { findViewById<EditText>(R.id.product_query) != null }
            scenario.onActivity { it.findViewById<EditText>(R.id.product_query).setText("חלב") }
            scenario.await { findViewById<View>(R.id.list_row_add) != null }
            capture("07-catalog-search")
            scenario.click(R.id.list_row_add)
            scenario.await {
                findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.text ==
                    getString(R.string.list_write_ok)
            }
            scenario.onActivity {
                val tabs = it.findViewById<TabLayout>(R.id.product_tabs)
                tabs.selectTab(tabs.getTabAt(1))
                it.findViewById<EditText>(R.id.product_barcode).setText("0000000000017")
            }
            scenario.click(R.id.product_barcode_lookup)
            scenario.await { findViewById<View>(R.id.list_row_add) != null }
            capture("07-manual-barcode")
            scenario.click(R.id.list_row_add)
            scenario.onActivity {
                val tabs = it.findViewById<TabLayout>(R.id.product_tabs)
                tabs.selectTab(tabs.getTabAt(2))
                it.findViewById<EditText>(R.id.product_free_name).setText("פטרוזיליה")
            }
            scenario.await { findViewById<View>(R.id.product_free_add)?.isEnabled == true }
            scenario.click(R.id.product_free_add)
            scenario.await { findViewById<EditText>(R.id.product_free_name)?.text?.isEmpty() == true }
            capture("07-free-item")
            scenario.click(R.id.product_done)
            scenario.await {
                findViewById<LinearLayout>(R.id.editor_items)?.childCount == 2 &&
                    findViewById<QuantitySelectorView>(R.id.list_row_quantity)?.value == 2 &&
                    findViewById<View>(R.id.editor_compare)?.isEnabled == true
            }
            capture("08-list-editor")
            scenario.click(R.id.sc_quantity_increase)
            scenario.await {
                val quantity = findViewById<QuantitySelectorView>(R.id.list_row_quantity)
                quantity?.isEnabled == true && quantity.value == 3
            }
        }
    }

    private fun ActivityScenario<MainActivity>.click(id: Int) {
        onActivity { assertTrue(it.findViewById<View>(id).performClick()) }
    }

    private fun ActivityScenario<MainActivity>.await(condition: MainActivity.() -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 10_000
        do {
            var matched = false
            onActivity { matched = it.condition() }
            if (matched) return
            SystemClock.sleep(100)
        } while (SystemClock.elapsedRealtime() < deadline)
        throw AssertionError("Timed out waiting for the expected SmartCart screen state")
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        SystemClock.sleep(400)
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.filesDir, "smartcart-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }
}

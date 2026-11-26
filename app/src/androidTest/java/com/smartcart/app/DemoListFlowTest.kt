package com.smartcart.app

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import android.view.MotionEvent
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.view.accessibility.AccessibilityNodeInfo
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.smartcart.app.ui.components.QuantitySelectorView
import com.smartcart.app.data.repository.ProductRepository
import com.smartcart.app.data.source.FakeDataSource
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoListFlowTest {
    @Test
    fun cameraFramesDetectAndAddProduct() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("fixtureCamera") == "true")
        assumeFalse(BuildConfig.USE_FIRESTORE)
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName, Manifest.permission.CAMERA,
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                val nav = (it.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
                    .navController
                nav.navigate(R.id.homeFragment)
            }
            scenario.await { findViewById<View>(R.id.btn_to_new_list) != null }
            scenario.click(R.id.btn_to_new_list)
            scenario.await { findViewById<View>(R.id.new_weekend) != null }
            scenario.click(R.id.new_weekend)
            scenario.click(R.id.new_list_create)
            scenario.await { findViewById<View>(R.id.editor_add)?.isEnabled == true }
            scenario.click(R.id.editor_add)
            scenario.await { findViewById<TabLayout>(R.id.product_tabs) != null }
            scenario.selectProductTab(1)
            try {
                scenario.await(timeoutMs = 25_000) {
                    findViewById<TextView>(R.id.list_row_name)?.text?.toString() == "חלב 3% שומן"
                }
            } catch (failure: AssertionError) {
                capture("07-camera-fixture-preview")
                throw failure
            }
            capture("07-camera-scan")
            scenario.click(R.id.list_row_add)
            scenario.await {
                findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.text ==
                    getString(R.string.list_write_ok)
            }
            scenario.click(R.id.product_done)
            scenario.await { findViewById<QuantitySelectorView>(R.id.list_row_quantity)?.value == 1 }
        }
    }

    @Test
    fun decodesCatalogEan13FromImage() {
        val pattern = "101" + "0001101".repeat(6) + "01010" +
            "1110010".repeat(4) + "1100110" + "1000100" + "101"
        val moduleWidth = 6
        val barcode = Bitmap.createBitmap((pattern.length + 24) * moduleWidth, 300, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(barcode)
            canvas.drawColor(Color.WHITE)
            val paint = Paint().apply { color = Color.BLACK }
            pattern.forEachIndexed { index, bit ->
                if (bit == '1') {
                    val x = ((index + 12) * moduleWidth).toFloat()
                    canvas.drawRect(x, 30f, x + moduleWidth, 270f, paint)
                }
            }
            BarcodeScanning.getClient().use { scanner ->
                val decoded = Tasks.await(scanner.process(InputImage.fromBitmap(barcode, 0)), 15, TimeUnit.SECONDS)
                val detected = requireNotNull(decoded.single().rawValue)
                val resolved = runBlocking { ProductRepository(FakeDataSource(delayMs = 0)).findBarcode(detected) }
                assertEquals("0000000000017", resolved.getOrThrow()?.barcode)
            }
        } finally {
            barcode.recycle()
        }
    }

    @Test
    fun cameraDenialKeepsRetryAndSearchAvailable() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeFalse(BuildConfig.USE_FIRESTORE)
        assumeFalse(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        context.getSharedPreferences("smartcart_permissions", 0).edit().remove("camera_requested").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                val nav = (it.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
                    .navController
                nav.navigate(R.id.homeFragment)
            }
            scenario.await { findViewById<View>(R.id.btn_to_list_editor)?.isEnabled == true }
            scenario.click(R.id.btn_to_list_editor)
            scenario.await { findViewById<View>(R.id.editor_add)?.isEnabled == true }
            scenario.click(R.id.editor_add)
            scenario.await { findViewById<TabLayout>(R.id.product_tabs) != null }
            scenario.selectProductTab(1)
            denyPermissionDialog()
            scenario.await { findViewById<View>(R.id.product_camera_permission)?.visibility == View.VISIBLE }
            capture("07-camera-denied")
            scenario.click(R.id.product_camera_permission)
            denyPermissionDialog()
            scenario.await {
                findViewById<TextView>(R.id.product_camera_permission)?.text == getString(R.string.list_camera_open_settings)
            }
            scenario.click(R.id.product_camera_search)
            scenario.setText(R.id.product_query, "חלב")
            scenario.await { findViewById<View>(R.id.list_row_add) != null }
        }
    }

    private fun denyPermissionDialog() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        val deadline = SystemClock.elapsedRealtime() + 10_000
        val observedPackages = mutableSetOf<String>()
        val controls = mutableSetOf<String>()
        fun findDecline(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            val id = node.viewIdResourceName.orEmpty()
            val label = node.text?.toString().orEmpty()
            if (node.isClickable) controls.add("$id/$label")
            if (node.isClickable && (id.contains("deny", ignoreCase = true) ||
                    label.replace('’', '\'').equals("Don't allow", ignoreCase = true))) return node
            return (0 until node.childCount).firstNotNullOfOrNull { node.getChild(it)?.let(::findDecline) }
        }
        while (SystemClock.elapsedRealtime() < deadline) {
            val roots = automation.windows.mapNotNull { it.root } + listOfNotNull(automation.rootInActiveWindow)
            for (root in roots) {
                val pkg = root.packageName?.toString() ?: continue
                observedPackages.add(pkg)
                if (!pkg.endsWith(".permissioncontroller")) continue
                val deny = findDecline(root)
                if (deny?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return
            }
            SystemClock.sleep(100)
        }
        throw AssertionError("No decline action in Android camera dialog. Windows: $observedPackages. Controls: $controls")
    }

    @Test
    fun rendersAuthScreensAndValidatesAnInvalidEmailLocally() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.await { findViewById<View>(R.id.auth_forgot) != null }
            capture("01-sign-in")
            scenario.setText(R.id.auth_email_value, "invalid")
            scenario.setText(R.id.auth_password_value, "example1!")
            scenario.click(R.id.auth_submit)
            scenario.await { findViewById<TextInputLayout>(R.id.auth_email_field)?.error != null }
            capture("01-invalid-email")
            scenario.click(R.id.auth_alternate)
            scenario.await { findViewById<View>(R.id.auth_terms_check) != null }
            capture("02-registration")
            scenario.click(R.id.auth_submit)
            scenario.await { findViewById<View>(R.id.auth_terms_validation)?.visibility == View.VISIBLE }
            capture("02-missing-terms")
            scenario.onActivity { it.findViewById<android.widget.CompoundButton>(R.id.auth_terms_check).isChecked = true }
            scenario.setText(R.id.auth_name_value, "א")
            scenario.setText(R.id.auth_email_value, "example@example.invalid")
            scenario.setText(R.id.auth_password_value, "1234")
            scenario.click(R.id.auth_submit)
            scenario.await { findViewById<TextInputLayout>(R.id.auth_name_field)?.error != null }
            capture("02-invalid-name")
            scenario.click(R.id.auth_header_back)
            scenario.await { findViewById<View>(R.id.auth_forgot) != null }
            scenario.onActivity {
                val host = it.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                host.navController.navigate(R.id.verifyEmailFragment)
            }
            scenario.await { findViewById<View>(R.id.auth_resend) != null }
            capture("03-verify")
            scenario.click(R.id.auth_header_back)
            scenario.await { findViewById<View>(R.id.auth_forgot) != null }
            scenario.click(R.id.auth_forgot)
            scenario.await { findViewById<View>(R.id.auth_reset_heading) != null }
            capture("04-reset")
            scenario.setText(R.id.auth_email_value, "invalid")
            scenario.click(R.id.auth_submit)
            scenario.await { findViewById<TextInputLayout>(R.id.auth_email_field)?.error != null }
        }
    }

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
            scenario.await { findViewById<View>(R.id.home_invitation)?.visibility == View.VISIBLE }
            capture("05-home")
            scenario.click(R.id.home_invitation)
            scenario.await { findViewById<View>(R.id.invitation_details)?.visibility == View.VISIBLE }
            capture("17-invitation")
            scenario.click(R.id.invitation_accept)
            scenario.await { findViewById<View>(R.id.editor_add)?.isEnabled == true }
            scenario.onActivity { assertTrue(it.findViewById<View>(R.id.editor_share).visibility == View.GONE) }
            scenario.click(R.id.list_back)
            scenario.await { findViewById<TextView>(R.id.home_created)?.text?.isNotBlank() == true }
            scenario.onActivity { assertTrue(it.findViewById<View>(R.id.home_invitation).visibility == View.GONE) }
            scenario.click(R.id.btn_to_new_list)
            scenario.await { findViewById<View>(R.id.new_list_create) != null }
            capture("06-new-list")
            scenario.click(R.id.new_list_create)
            scenario.click(R.id.new_weekend)
            scenario.click(R.id.new_list_create)
            scenario.await { findViewById<View>(R.id.editor_add)?.isEnabled == true }
            capture("08-empty-editor")
            scenario.click(R.id.editor_add)
            scenario.await { findViewById<EditText>(R.id.product_query) != null }
            scenario.setText(R.id.product_query, "ח")
            scenario.onActivity { assertTrue(it.findViewById<LinearLayout>(R.id.product_results).childCount == 0) }
            scenario.onActivity { it.findViewById<EditText>(R.id.product_query).setText("חלב") }
            scenario.await { findViewById<View>(R.id.list_row_add) != null }
            capture("07-catalog-search")
            scenario.click(R.id.list_row_add)
            scenario.await {
                findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.text ==
                    getString(R.string.list_write_ok)
            }
            scenario.selectProductTab(1)
            scenario.click(R.id.product_barcode_lookup)
            scenario.await { findViewById<EditText>(R.id.product_barcode)?.error != null }
            scenario.setText(R.id.product_barcode, "0000000000017")
            scenario.click(R.id.product_barcode_lookup)
            scenario.await { findViewById<View>(R.id.list_row_add) != null }
            capture("07-manual-barcode")
            scenario.click(R.id.list_row_add)
            scenario.selectProductTab(0)
            scenario.setText(R.id.product_query, "פטרוזיליה")
            scenario.await { findViewById<View>(R.id.product_fallback)?.visibility == View.VISIBLE }
            capture("07-no-results")
            scenario.click(R.id.product_fallback)
            scenario.await { findViewById<EditText>(R.id.product_free_name)?.text?.toString() == "פטרוזיליה" }
            scenario.await { findViewById<View>(R.id.product_free_add)?.isEnabled == true }
            scenario.setText(R.id.product_free_quantity, "0")
            scenario.click(R.id.product_free_add)
            scenario.await { findViewById<EditText>(R.id.product_free_quantity)?.error != null }
            scenario.setText(R.id.product_free_quantity, "4")
            scenario.setText(R.id.product_free_note, "טרייה")
            capture("07-free-item")
            scenario.click(R.id.product_free_add)
            scenario.await { findViewById<EditText>(R.id.product_free_name)?.text?.isEmpty() == true }
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
            scenario.click(R.id.list_row_detail)
            val noteField = findNode { it.className?.toString() == EditText::class.java.name }
            assertTrue(noteField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "ללא לקטוז")
            }))
            capture("08-note")
            clickText(R.string.list_save)
            scenario.await { findViewById<TextView>(R.id.list_row_detail)?.text?.toString() == "ללא לקטוז" }
            scenario.click(R.id.editor_menu)
            capture("08-menu")
            scenario.click(R.id.editor_budget_action)
            scenario.await { findViewById<View>(R.id.budget_save)?.isEnabled == true }
            scenario.setText(R.id.budget_amount, "10001")
            scenario.click(R.id.budget_save)
            scenario.await { findViewById<TextInputLayout>(R.id.budget_input)?.error != null }
            scenario.click(R.id.budget_250)
            capture("11-budget")
            scenario.click(R.id.budget_save)
            scenario.await { findViewById<View>(R.id.editor_budget)?.visibility == View.VISIBLE }
            scenario.click(R.id.editor_budget)
            scenario.await { findViewById<EditText>(R.id.budget_amount)?.text?.toString() == "250" }
            scenario.click(R.id.list_back)
            scenario.await { findViewById<LinearLayout>(R.id.editor_items)?.childCount == 2 }
            scenario.swipeSecondRow()
            scenario.await {
                val entries = findViewById<LinearLayout>(R.id.editor_items)
                entries?.getChildAt(1)?.findViewById<View>(R.id.list_row_remove)?.visibility == View.VISIBLE
            }
            capture("08-swipe")
            clickText(R.string.editor_swipe_delete)
            clickText(R.string.list_remove)
            scenario.await { findViewById<LinearLayout>(R.id.editor_items)?.childCount == 1 }
            scenario.click(R.id.editor_menu)
            scenario.click(R.id.editor_rename_action)
            scenario.await { findViewById<View>(R.id.list_action_confirm)?.isEnabled == true }
            scenario.setText(R.id.list_action_name, " ")
            scenario.click(R.id.list_action_confirm)
            scenario.await { findViewById<TextInputLayout>(R.id.list_action_name_layout)?.error != null }
            scenario.setText(R.id.list_action_name, "רשימה בדוקה")
            capture("08-rename")
            scenario.click(R.id.list_action_confirm)
            scenario.await { findViewById<TextView>(R.id.list_title)?.text?.toString() == "רשימה בדוקה" }
            scenario.click(R.id.editor_menu)
            scenario.click(R.id.editor_delete_action)
            scenario.await { findViewById<View>(R.id.list_action_confirm)?.isEnabled == true }
            capture("08-delete-list")
            scenario.click(R.id.list_action_confirm)
            scenario.await { findViewById<View>(R.id.home_counts) != null }
        }
    }

    private fun clickText(text: Int) {
        val label = InstrumentationRegistry.getInstrumentation().targetContext.getString(text)
        var node = findNode { it.text?.toString() == label }
        while (!node.isClickable) node = requireNotNull(node.parent)
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun findNode(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        fun search(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (matches(node)) return node
            return (0 until node.childCount).firstNotNullOfOrNull { node.getChild(it)?.let(::search) }
        }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.elapsedRealtime() + 5_000
        do {
            automation.rootInActiveWindow?.let(::search)?.let { return it }
            SystemClock.sleep(100)
        } while (SystemClock.elapsedRealtime() < deadline)
        throw AssertionError("Expected menu or dialog control was not visible")
    }

    private fun ActivityScenario<MainActivity>.swipeSecondRow() {
        val location = IntArray(2)
        var distance = 0
        onActivity {
            val row = it.findViewById<LinearLayout>(R.id.editor_items).getChildAt(1)
            row.getLocationOnScreen(location)
            location[1] += row.height / 3
            location[0] += row.width / 4
            distance = row.width / 2
        }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val down = SystemClock.uptimeMillis()
        for (step in 0..6) {
            val action = when (step) {
                0 -> MotionEvent.ACTION_DOWN
                6 -> MotionEvent.ACTION_UP
                else -> MotionEvent.ACTION_MOVE
            }
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action,
                location[0] + distance * step / 6f, location[1].toFloat(), 0)
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            try {
                assertTrue(automation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
            SystemClock.sleep(12)
        }
    }

    private fun ActivityScenario<MainActivity>.selectProductTab(index: Int) {
        onActivity {
            val tabs = it.findViewById<TabLayout>(R.id.product_tabs)
            tabs.selectTab(tabs.getTabAt(index))
        }
    }

    private fun ActivityScenario<MainActivity>.setText(id: Int, value: String) {
        onActivity { it.findViewById<EditText>(id).setText(value) }
    }

    private fun ActivityScenario<MainActivity>.click(id: Int) {
        onActivity { assertTrue(it.findViewById<View>(id).performClick()) }
    }

    private fun ActivityScenario<MainActivity>.await(timeoutMs: Long = 10_000, condition: MainActivity.() -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
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

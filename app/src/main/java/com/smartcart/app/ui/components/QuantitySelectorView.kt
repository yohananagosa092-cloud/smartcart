package com.smartcart.app.ui.components

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.smartcart.app.R

/** Reusable quantity control. Screens bind value and onValueChanged to their ViewModel. */
class QuantitySelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val decrease: MaterialButton
    private val increase: MaterialButton
    private val number: TextView
    var onValueChanged: ((Int) -> Unit)? = null

    var value: Int = 1
        set(newValue) {
            val bounded = newValue.coerceIn(1, 999)
            if (field == bounded) return
            field = bounded
            render()
        }

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.sc_quantity_selector, this, true)
        decrease = findViewById(R.id.sc_quantity_decrease)
        increase = findViewById(R.id.sc_quantity_increase)
        number = findViewById(R.id.sc_quantity_value)
        decrease.setOnClickListener { changeBy(-1) }
        increase.setOnClickListener { changeBy(1) }
        render()
    }

    private fun changeBy(delta: Int) {
        val previous = value
        value += delta
        if (value != previous) onValueChanged?.invoke(value)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        // Framework inflation can call this override before child views exist.
        if (childCount > 0) render()
    }

    private fun render() {
        number.text = value.toString()
        decrease.isEnabled = isEnabled && value > 1
        increase.isEnabled = isEnabled && value < 999
    }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable("super", super.onSaveInstanceState())
        putInt("quantity", value)
    }

    @Suppress("DEPRECATION")
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable("super"))
            value = state.getInt("quantity", 1)
            render()
        } else super.onRestoreInstanceState(state)
    }
}

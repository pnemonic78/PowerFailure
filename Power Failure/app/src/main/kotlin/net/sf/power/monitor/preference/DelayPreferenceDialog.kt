/*
 * Copyright 2012, Moshe Waisberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.power.monitor.preference

import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import com.github.preference.PreferenceDialog
import net.sf.power.monitor.R

class DelayPreferenceDialog : PreferenceDialog() {
    private var picker: NumberPicker? = null
    private var units: NumberPicker? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        val preference = delayPreference
        val seconds = delayPreference.value
        val pickerMax = preference.max
        val pickerValue = if (seconds > pickerMax) {
            seconds / MULTIPLIER_MINUTES
        } else {
            seconds / MULTIPLIER_SECONDS
        }
        val unitsPosition = if (seconds > pickerMax) {
            POSITION_MINUTES
        } else {
            POSITION_SECONDS
        }

        picker = view.findViewById(android.R.id.edit)
        checkNotNull(picker) { "Dialog view must contain a NumberPicker with id 'edit'" }
        picker!!.apply {
            minValue = preference.min
            maxValue = pickerMax
            value = pickerValue
        }

        units = view.findViewById(R.id.units)
        checkNotNull(units) { "Dialog view must contain a NumberPicker with id 'units'" }
        val unitsEntries = view.context.resources.getStringArray(R.array.delay_units_entries)
        units!!.apply {
            minValue = 0
            maxValue = unitsEntries.lastIndex
            value = unitsPosition
            displayedValues = unitsEntries
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            picker!!.clearFocus()
            val number = picker!!.value
            val unitsPosition = units!!.value
            val unitsMultiplier = if (unitsPosition == POSITION_MINUTES) {
                MULTIPLIER_MINUTES
            } else {
                MULTIPLIER_SECONDS
            }
            val value = number * unitsMultiplier
            val preference = delayPreference
            if (preference.callChangeListener(value)) {
                preference.value = value
            }
        }
    }

    private val delayPreference: DelayPreference
        get() = preference as DelayPreference

    companion object {
        fun newInstance(key: String?): DelayPreferenceDialog {
            val dialog = DelayPreferenceDialog()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            dialog.arguments = b
            return dialog
        }

        private const val POSITION_SECONDS = 0
        private const val POSITION_MINUTES = 1
        private const val MULTIPLIER_SECONDS = 1
        private const val MULTIPLIER_MINUTES = 60
    }
}
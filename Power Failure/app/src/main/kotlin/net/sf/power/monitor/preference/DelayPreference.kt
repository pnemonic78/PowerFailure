/*
 * Copyright 2021, Moshe Waisberg
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

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import com.github.preference.NumberPickerPreference
import net.sf.power.monitor.R

/**
 * Preference that shows a time delay picker.
 *
 *
 * The preference value is stored as the number of seconds.
 *
 *
 * @author Moshe Waisberg
 */
class DelayPreference(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : NumberPickerPreference(context, attrs, defStyleAttr, defStyleRes) {

    @SuppressLint("RestrictedApi")
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, TypedArrayUtils.getAttr(context, androidx.preference.R.attr.dialogPreferenceStyle, android.R.attr.dialogPreferenceStyle))

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    init {
        min = 1
        max = 99
        dialogLayoutResource = R.layout.preference_dialog_delay
        summaryProvider = SummaryProvider<DelayPreference> { format() }
    }

    override fun getPersistedInt(defaultReturnValue: Int): Int {
        return try {
            super.getPersistedInt(defaultReturnValue)
        } catch (e: ClassCastException) {
            val value = getPersistedString(null)
            return value?.toInt() ?: defaultReturnValue
        }
    }

    override fun shouldDisableDependents(): Boolean {
        return (value <= 0) || super.shouldDisableDependents()
    }

    /**
     * Format the delay as per user's locale.
     *
     * @return the formatted delay.
     */
    private fun format(): String {
        val elapsedSeconds = value.toLong()
        return DateUtils.formatElapsedTime(elapsedSeconds)
    }
}
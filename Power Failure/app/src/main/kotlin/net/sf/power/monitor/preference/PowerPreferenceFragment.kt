/*
 * Copyright 2016, Moshe Waisberg
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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.Preference
import com.github.preference.AbstractPreferenceFragment
import net.sf.power.monitor.PowerConnectionService

/**
 * This fragment shows the preferences for a header.
 *
 * @author Moshe Waisberg
 */
abstract class PowerPreferenceFragment : AbstractPreferenceFragment() {

    protected lateinit var settings: PowerPreferences

    private val target by lazy {
        val context: Context = requireContext()
        ComponentName(context, PowerConnectionService::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        settings = PowerPreferences(context)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val result = super.onPreferenceChange(preference, newValue)

        val context: Context = preference.context
        notifyService(context)

        return result
    }

    // Notify the service.
    private fun notifyService(context: Context) {
        val intent = Intent(PowerPreferences.ACTION_PREFERENCES_CHANGED)
            .setComponent(target)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}

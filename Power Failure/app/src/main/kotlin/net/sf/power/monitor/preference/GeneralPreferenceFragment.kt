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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.github.preference.RingtonePreference
import net.sf.power.monitor.BuildConfig
import net.sf.power.monitor.R

/**
 * This fragment shows the preferences for the General header.
 * @author Moshe Waisberg
 */
@Keep
class GeneralPreferenceFragment : PowerPreferenceFragment() {

    private var reminderRingtonePreference: RingtonePreference? = null
    private var smsPreference: SwitchPreference? = null
    private var recipientPreference: RecipientPreference? = null

    override fun getPreferencesXml(): Int {
        return R.xml.general_preferences
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        initList(PowerPreferences.KEY_FAILURE_DELAY)

        reminderRingtonePreference = initRingtone(PowerPreferences.KEY_RINGTONE_TONE)
        initList(PowerPreferences.KEY_RINGTONE_TYPE)

        smsPreference = findPreference(PowerPreferences.KEY_SMS_ENABLED)
        smsPreference?.onPreferenceChangeListener = this
        recipientPreference = initSmsRecipient(PowerPreferences.KEY_SMS_RECIPIENT)
        if (!BuildConfig.FEATURE_SMS) {
            smsPreference?.isEnabled = false
            recipientPreference?.isEnabled = false
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference === smsPreference) {
            if (newValue == true) {
                checkSmsPermission(preference.context)
            }
        } else if (preference === recipientPreference) {
            updateRecipientSummary(preference, newValue?.toString() ?: "")
        }
        return super.onPreferenceChange(preference, newValue)
    }

    override fun onListPreferenceChange(preference: ListPreference, newValue: Any?): Boolean {
        super.onListPreferenceChange(preference, newValue)

        val key = preference.key
        if (PowerPreferences.KEY_RINGTONE_TYPE == key && reminderRingtonePreference != null) {
            val value = newValue.toString()
            val ringType = if (value.isEmpty()) RingtoneManager.TYPE_ALARM else value.toInt()
            reminderRingtonePreference!!.ringtoneType = ringType
        }
        return false
    }

    private fun initSmsRecipient(key: String): RecipientPreference? {
        val preference = findPreference<RecipientPreference>(key) ?: return null
        preference.onPreferenceChangeListener = this
        preference.setOnClick(this, REQUEST_RECIPIENT)
        updateRecipientSummary(preference, preference.recipient)
        return preference
    }

    private fun updateRecipientSummary(preference: Preference, recipient: String) {
        preference.summary = getString(R.string.sms_summary, recipient)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        recipientPreference?.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkSmsPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_SMS) {
            if (grantResults.isNotEmpty() && (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                smsPreference?.isChecked = false
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val REQUEST_SMS = 0x535
        private const val REQUEST_RECIPIENT = 0x73C1
    }
}

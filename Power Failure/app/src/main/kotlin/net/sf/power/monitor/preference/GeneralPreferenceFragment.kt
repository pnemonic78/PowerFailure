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
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.core.content.PermissionChecker
import androidx.fragment.app.DialogFragment
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

    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    override val preferencesXml = R.xml.general_preferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        reminderRingtonePreference = initRingtone(PowerPreferences.KEY_RINGTONE_TONE)
        smsPreference = initSmsFeature()
        recipientPreference = initSmsRecipient()

        this.requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    smsPreference?.isEnabled = true
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)) {
                    smsPreference?.isEnabled = false
                    // TODO explain that we need this permission to send SMS to a contact.
                } else {
                    smsPreference?.isEnabled = false
                }
            }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference === smsPreference) {
            if (newValue == true) {
                checkPermissions(preference.context)
            }
        }
        return super.onPreferenceChange(preference, newValue)
    }

    private fun initSmsFeature(): SwitchPreference? {
        val preference =
            findPreference<SwitchPreference>(PowerPreferences.KEY_SMS_ENABLED) ?: return null
        if (BuildConfig.FEATURE_SMS) {
            preference.onPreferenceChangeListener = this
            preference.summaryProvider = Preference.SummaryProvider<SwitchPreference> {
                val dateTime = DateUtils.formatDateTime(
                    context,
                    settings.failureTime,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
                )
                getString(R.string.sms_failed_on, dateTime)
            }
        } else {
            preference.isEnabled = false
            preference.isVisible = false
        }
        return preference
    }

    private fun initSmsRecipient(): RecipientPreference? {
        val preference =
            findPreference<RecipientPreference>(PowerPreferences.KEY_SMS_RECIPIENT) ?: return null
        if (BuildConfig.FEATURE_SMS) {
            preference.setHost(this)
            preference.summaryProvider = Preference.SummaryProvider<RecipientPreference> {
                getString(R.string.sms_summary, it.recipient)
            }
        } else {
            preference.isEnabled = false
            preference.isVisible = false
        }
        return preference
    }

    private fun checkPermissions(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionChecker.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) != PermissionChecker.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher?.launch(Manifest.permission.SEND_SMS)
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val fragmentManager = parentFragmentManager
        // check if dialog is already showing
        if (fragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return
        }
        val key = preference.key
        val f: DialogFragment
        if (preference is DelayPreference) {
            f = DelayPreferenceDialog.newInstance(key)
            f.setTargetFragment(this, 0)
            f.show(fragmentManager, DIALOG_FRAGMENT_TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}

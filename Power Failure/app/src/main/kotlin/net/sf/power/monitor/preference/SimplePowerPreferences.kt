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

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import androidx.core.content.edit
import androidx.core.net.toUri
import com.github.media.RingtoneManager
import com.github.preference.SimplePreferences
import net.sf.power.monitor.NEVER
import net.sf.power.monitor.R
import net.sf.power.monitor.TimeMillis
import kotlin.math.max

/**
 * Application settings.
 *
 * @author Moshe Waisberg
 */
class SimplePowerPreferences(context: Context) : SimplePreferences(context), PowerPreferences {
    override val ringtone: Uri?
        get() {
            val type = RingtoneManager.TYPE_ALARM
            var path = preferences.getString(KEY_RINGTONE_TONE, RingtoneManager.DEFAULT_PATH)
            if (path == RingtoneManager.DEFAULT_PATH) {
                path = android.media.RingtoneManager.getDefaultUri(type).toString()
            }
            val ringtoneManager = RingtoneManager(context)
            ringtoneManager.setType(type)
            if (!ringtoneManager.isIncludeExternal) {
                path = ringtoneManager.filterInternal(path)
            }
            return if (path.isNullOrEmpty()) null else path.toUri()
        }

    override val isVibrate: Boolean
        get() = preferences.getBoolean(
            KEY_VIBRATE,
            context.resources.getBoolean(R.bool.vibrate_defaultValue)
        )

    override val failureDelay: TimeMillis
        get() {
            val defaultValue = context.resources.getInteger(R.integer.delay_defaultValue)
            val delay: Int = try {
                preferences.getInt(KEY_FAILURE_DELAY, defaultValue)
            } catch (_: ClassCastException) {
                preferences.getString(KEY_FAILURE_DELAY, null)?.toInt() ?: defaultValue
            }
            return max(0L, delay * DateUtils.SECOND_IN_MILLIS)
        }

    override var failureTime: TimeMillis
        get() = preferences.getLong(KEY_FAILURE_TIME, NEVER)
        set(value) {
            preferences.edit { putLong(KEY_FAILURE_TIME, value) }
        }

    override var restoredTime: TimeMillis
        get() = preferences.getLong(KEY_RESTORED_TIME, NEVER)
        set(value) {
            preferences.edit { putLong(KEY_RESTORED_TIME, value) }
        }

    override var isSmsEnabled: Boolean
        get() = preferences.getBoolean(KEY_SMS_ENABLED, false)
        set(value) {
            preferences.edit { putBoolean(KEY_SMS_ENABLED, value) }
        }

    override var smsRecipient: String
        get() = preferences.getString(KEY_SMS_RECIPIENT, "") ?: ""
        set(value) {
            preferences.edit { putString(KEY_SMS_RECIPIENT, value) }
        }

    companion object {
        /**
         * Preference name for the reminder ringtone.
         */
        const val KEY_RINGTONE_TONE = "ringtone.tone"

        /**
         * Preference name for vibration.
         */
        const val KEY_VIBRATE = "vibrate"

        /**
         * Preference name for delay (seconds).
         */
        const val KEY_FAILURE_DELAY = "delay"

        /**
         * Preference name for the when was the last significant power failure.
         */
        const val KEY_FAILURE_TIME = "failure.time"

        /**
         * Preference name for when was the last significant power restoration.
         */
        const val KEY_RESTORED_TIME = "restored.time"

        /**
         * Preference name for enabling sending an SMS.
         */
        const val KEY_SMS_ENABLED = "sms.enabled"

        /**
         * Preference name for the SMS recipient number.
         */
        const val KEY_SMS_RECIPIENT = "sms.recipient"
    }
}
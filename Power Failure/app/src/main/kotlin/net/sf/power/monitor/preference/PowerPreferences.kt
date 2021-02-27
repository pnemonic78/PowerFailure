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
import com.github.media.RingtoneManager
import com.github.preference.SimplePreferences
import net.sf.power.monitor.R

/**
 * Application settings.
 *
 * @author Moshe Waisberg
 */
class PowerPreferences(context: Context) : SimplePreferences(context) {

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
         * Preference name for enabling sending an SMS.
         */
        const val KEY_SMS_ENABLED = "sms.enabled"

        /**
         * Preference name for the SMS recipient number.
         */
        const val KEY_SMS_RECIPIENT = "sms.recipient"

        /**
         * Action that the shared preferences have changed.
         */
        const val ACTION_PREFERENCES_CHANGED = "net.sf.power.monitor.action.PREFERENCES_CHANGED"
    }

    /**
     * Get the ringtone.
     *
     * @return the ringtone.
     * @see RingtoneManager.getDefaultUri
     */
    val ringtone: Uri?
        get() {
            val type = RingtoneManager.TYPE_ALARM
            var path = preferences.getString(KEY_RINGTONE_TONE, RingtoneManager.DEFAULT_PATH)
            if (path == RingtoneManager.DEFAULT_PATH) {
                path = RingtoneManager.getDefaultUri(type).toString()
            }
            val ringtoneManager = RingtoneManager(context)
            ringtoneManager.setType(type)
            if (!ringtoneManager.isIncludeExternal) {
                path = ringtoneManager.filterInternal(path)
            }
            return if (path.isNullOrEmpty()) null else Uri.parse(path)
        }

    /**
     * Vibrate the device when power disconnected?
     *
     * @return `true` to vibrate.
     */
    val isVibrate: Boolean
        get() = preferences.getBoolean(KEY_VIBRATE, context.resources.getBoolean(R.bool.vibrate_defaultValue))

    /**
     * Get the time delay after the power is disconnected to when to notify about the failure.
     *
     * @return the time in milliseconds.
     */
    val failureDelay: Long
        get() = try {
            preferences.getInt(KEY_FAILURE_DELAY, context.resources.getInteger(R.integer.delay_defaultValue)).toLong() * DateUtils.SECOND_IN_MILLIS
        } catch (e: ClassCastException) {
            preferences.getString(KEY_FAILURE_DELAY, context.resources.getInteger(R.integer.delay_defaultValue).toString())!!.toLong() * DateUtils.SECOND_IN_MILLIS
        }

    /**
     * Get the time when the power failed.
     *
     * @return the time in milliseconds.
     */
    var failureTime: Long = 0L
        get() = preferences.getLong(KEY_FAILURE_TIME, 0L)
        set(value) {
            field = value
            preferences.edit()
                .putLong(KEY_FAILURE_TIME, value)
                .apply()
        }

    /**
     * Is sending an SMS enabled?
     *
     * @return `true` if enabled.
     */
    var isSmsEnabled: Boolean
        get() = preferences.getBoolean(KEY_SMS_ENABLED, false)
        set(value) {
            preferences.edit().putBoolean(KEY_SMS_ENABLED, value).apply()
        }

    /**
     * Get the SMS recipient.
     *
     * @return A contact number.
     */
    var smsRecipient: String
        get() = preferences.getString(KEY_SMS_RECIPIENT, "") ?: ""
        set(value) {
            preferences.edit().putString(KEY_SMS_RECIPIENT, value).apply()
        }
}
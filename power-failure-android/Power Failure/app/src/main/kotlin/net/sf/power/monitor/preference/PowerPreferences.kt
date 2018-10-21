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
import android.text.TextUtils
import android.text.format.DateUtils

import com.github.media.RingtoneManager
import com.github.preference.SimplePreferences

import net.sf.power.monitor.R

/**
 * Application settings.
 *
 * @author Moshe Waisberg
 */
class PowerPreferences(context: Context) : SimplePreferences(context, true) {

    companion object {

        /**
         * Preference name for the reminder type.
         */
        const val KEY_RINGTONE_TYPE = "ringtone.type"
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
        const val KEY_DELAY = "delay"

        /**
         * Action that the shared preferences have changed.
         */
        const val ACTION_PREFERENCES_CHANGED = "net.sf.power.monitor.action.PREFERENCES_CHANGED"
    }

    /**
     * Get the ringtone type.
     *
     * @return the ringtone type. One of [RingtoneManager.TYPE_ALARM] or [RingtoneManager.TYPE_NOTIFICATION].
     */
    val ringtoneType: Int
        get() = Integer.parseInt(preferences.getString(KEY_RINGTONE_TYPE, context.getString(R.string.ringtone_type_defaultValue))!!)

    /**
     * Get the ringtone.
     *
     * @return the ringtone.
     * @see RingtoneManager.getDefaultUri
     */
    val ringtone: Uri?
        get() {
            val type = ringtoneType
            var path = preferences.getString(KEY_RINGTONE_TONE, RingtoneManager.DEFAULT_PATH)
            if (path == RingtoneManager.DEFAULT_PATH) {
                path = RingtoneManager.getDefaultUri(type).toString()
            }
            val ringtoneManager = RingtoneManager(context)
            ringtoneManager.setType(type)
            if (!ringtoneManager.isIncludeExternal) {
                path = ringtoneManager.filterInternal(path)
            }
            return if (TextUtils.isEmpty(path)) null else Uri.parse(path)
        }

    /**
     * Vibrate the device when power disconnected?
     *
     * @return `true` to vibrate.
     */
    val isVibrate: Boolean
        get() = preferences.getBoolean(KEY_VIBRATE, context.resources.getBoolean(R.bool.vibrate_defaultValue))

    /**
     * Get the time delay after the power is disconnected to when to notify about the disconnection.
     *
     * @return the time in milliseconds.
     */
    val timeDelay: Long
        get() = java.lang.Long.parseLong(preferences.getString(KEY_DELAY, context.resources.getString(R.string.delay_defaultValue))!!) * DateUtils.SECOND_IN_MILLIS
}
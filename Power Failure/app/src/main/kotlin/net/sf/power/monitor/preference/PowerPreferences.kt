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

import android.net.Uri
import net.sf.power.monitor.TimeMillis

/**
 * Application settings.
 *
 * @author Moshe Waisberg
 */
interface PowerPreferences {
    /**
     * Get the ringtone.
     *
     * @return the ringtone.
     * @see [android.media.RingtoneManager.getDefaultUri]
     */
    val ringtone: Uri?

    /**
     * Vibrate the device when power disconnected?
     *
     * @return `true` to vibrate.
     */
    val isVibrate: Boolean

    /**
     * Get the time delay after the power is disconnected to when to notify about the failure.
     *
     * @return the time in milliseconds.
     */
    val failureDelay: TimeMillis

    /**
     * Get the time when the power failed.
     *
     * @return the time in milliseconds.
     */
    var failureTime: TimeMillis

    /**
     * Get the time when the power was restored.
     *
     * @return the time in milliseconds.
     */
    var restoredTime: TimeMillis

    /**
     * Is sending an SMS enabled?
     *
     * @return `true` if enabled.
     */
    var isSmsEnabled: Boolean

    /**
     * Get the SMS recipient.
     *
     * @return A contact number.
     */
    var smsRecipient: String

    companion object {
        /**
         * Action that the shared preferences have changed.
         */
        const val ACTION_PREFERENCES_CHANGED = "net.sf.power.monitor.action.PREFERENCES_CHANGED"
    }
}
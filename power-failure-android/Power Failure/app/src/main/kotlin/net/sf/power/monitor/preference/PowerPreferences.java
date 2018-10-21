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
package net.sf.power.monitor.preference;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.github.media.RingtoneManager;
import com.github.preference.SimplePreferences;

import net.sf.power.monitor.R;

/**
 * Application settings.
 *
 * @author Moshe Waisberg
 */
public class PowerPreferences extends SimplePreferences {

    /**
     * Preference name for the reminder type.
     */
    public static final String KEY_RINGTONE_TYPE = "ringtone.type";
    /**
     * Preference name for the reminder ringtone.
     */
    public static final String KEY_RINGTONE_TONE = "ringtone.tone";
    /**
     * Preference name for vibration.
     */
    public static final String KEY_VIBRATE = "vibrate";
    /**
     * Preference name for delay (seconds).
     */
    public static final String KEY_DELAY = "delay";

    /**
     * Action that the shared preferences have changed.
     */
    public static final String ACTION_PREFERENCES_CHANGED = "net.sf.power.monitor.action.PREFERENCES_CHANGED";

    /**
     * Constructs a new settings.
     *
     * @param context the context.
     */
    public PowerPreferences(Context context) {
        super(context, true);
    }

    /**
     * Get the ringtone type.
     *
     * @return the ringtone type. One of {@link RingtoneManager#TYPE_ALARM} or {@link RingtoneManager#TYPE_NOTIFICATION}.
     */
    public int getRingtoneType() {
        return Integer.parseInt(preferences.getString(KEY_RINGTONE_TYPE, context.getString(R.string.ringtone_type_defaultValue)));
    }

    /**
     * Get the ringtone.
     *
     * @return the ringtone.
     * @see RingtoneManager#getDefaultUri(int)
     */
    public Uri getRingtone() {
        int type = getRingtoneType();
        String path = preferences.getString(KEY_RINGTONE_TONE, RingtoneManager.DEFAULT_PATH);
        if (path == RingtoneManager.DEFAULT_PATH) {
            path = RingtoneManager.getDefaultUri(type).toString();
        }
        RingtoneManager ringtoneManager = new RingtoneManager(context);
        ringtoneManager.setType(type);
        if (!ringtoneManager.isIncludeExternal()) {
            path = ringtoneManager.filterInternal(path);
        }
        return TextUtils.isEmpty(path) ? null : Uri.parse(path);
    }

    /**
     * Vibrate the device when power disconnected?
     *
     * @return {@code true} to vibrate.
     */
    public boolean isVibrate() {
        return preferences.getBoolean(KEY_VIBRATE, context.getResources().getBoolean(R.bool.vibrate_defaultValue));
    }

    /**
     * Get the time delay after the power is disconnected to when to notify about the disconnection.
     *
     * @return the time in milliseconds.
     */
    public long getTimeDelay() {
        return Long.parseLong(preferences.getString(KEY_DELAY, context.getResources().getString(R.string.delay_defaultValue))) * DateUtils.SECOND_IN_MILLIS;
    }
}

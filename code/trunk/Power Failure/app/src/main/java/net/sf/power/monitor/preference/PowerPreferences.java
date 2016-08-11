/*
 * Source file of the Power Failure Monitor project.
 * Copyright (c) 2016. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/2.0
 *
 * Contributors can be contacted by electronic mail via the project Web pages:
 *
 * https://sourceforge.net/projects/power-failure/
 *
 * Contributor(s):
 *   Moshe Waisberg
 *
 */
package net.sf.power.monitor.preference;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import net.sf.media.RingtoneManager;
import net.sf.power.monitor.R;
import net.sf.preference.SimplePreferences;

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
     * Constructs a new settings.
     *
     * @param context the context.
     */
    public PowerPreferences(Context context) {
        super(context);
    }

    /**
     * Get the reminder ringtone type.
     *
     * @return the ringtone type. One of {@link RingtoneManager#TYPE_ALARM} or {@link RingtoneManager#TYPE_NOTIFICATION}.
     */
    public int getReminderType() {
        return Integer.parseInt(preferences.getString(KEY_RINGTONE_TYPE, context.getString(R.string.ringtone_type_defaultValue)));
    }

    /**
     * Get the reminder ringtone.
     *
     * @return the ringtone.
     * @see RingtoneManager#getDefaultUri(int)
     */
    public Uri getReminderRingtone() {
        int type = getReminderType();
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
     * Get the reminder ringtone type.
     *
     * @return the ringtone type. One of {@link RingtoneManager#TYPE_ALARM} or {@link RingtoneManager#TYPE_NOTIFICATION}.
     */
    public boolean isVibrate() {
        return preferences.getBoolean(KEY_VIBRATE, context.getResources().getBoolean(R.bool.vibrate_defaultValue));
    }
}

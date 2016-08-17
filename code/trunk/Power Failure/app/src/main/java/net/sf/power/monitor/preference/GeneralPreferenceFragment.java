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
 * https://sourceforge.net/projects/power-failure
 *
 * Contributor(s):
 *   Moshe Waisberg
 *
 */
package net.sf.power.monitor.preference;

import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.text.TextUtils;

import net.sf.power.monitor.R;
import net.sf.preference.RingtonePreference;
import net.sf.preference.SeekBarDialogPreference;

/**
 * This fragment shows the preferences for the General header.
 */
public class GeneralPreferenceFragment extends PowerPreferenceFragment {

    private RingtonePreference reminderRingtonePreference;

    @Override
    protected int getPreferencesXml() {
        return R.xml.general_preferences;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SeekBarDialogPreference seek = (SeekBarDialogPreference) findPreference(PowerPreferences.KEY_DELAY);
        seek.setSummaryFormat(R.plurals.delay_summary);
        seek.setOnPreferenceChangeListener(this);

        reminderRingtonePreference = initRingtone(PowerPreferences.KEY_RINGTONE_TONE);
        initList(PowerPreferences.KEY_RINGTONE_TYPE);

        findPreference(PowerPreferences.KEY_VIBRATE).setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onListPreferenceChange(ListPreference preference, Object newValue) {
        super.onListPreferenceChange(preference, newValue);

        String key = preference.getKey();
        if (PowerPreferences.KEY_RINGTONE_TYPE.equals(key) && (reminderRingtonePreference != null)) {
            String value = newValue.toString();
            int ringType = TextUtils.isEmpty(value) ? RingtoneManager.TYPE_ALARM : Integer.parseInt(value);
            reminderRingtonePreference.setRingtoneType(ringType);
        }
    }
}

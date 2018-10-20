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

import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.text.TextUtils;

import com.github.preference.RingtonePreference;

import net.sf.power.monitor.R;

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

        initList(PowerPreferences.KEY_DELAY);

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

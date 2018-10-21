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
import android.content.Intent;
import android.preference.Preference;

import com.github.preference.AbstractPreferenceFragment;

/**
 * This fragment shows the preferences for a header.
 *
 * @author moshe.w
 */
public abstract class PowerPreferenceFragment extends AbstractPreferenceFragment {

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = super.onPreferenceChange(preference, newValue);

        // Notify the service.
        Context context = getActivity();
        if (context != null) {
            Intent intent = new Intent(PowerPreferences.ACTION_PREFERENCES_CHANGED);
            context.sendBroadcast(intent);
        }

        return result;
    }
}

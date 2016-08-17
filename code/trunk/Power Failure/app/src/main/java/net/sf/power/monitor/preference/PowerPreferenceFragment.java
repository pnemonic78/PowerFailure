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

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;

import net.sf.preference.AbstractPreferenceFragment;

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

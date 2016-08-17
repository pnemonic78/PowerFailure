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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;

import net.sf.power.monitor.R;

/**
 * This fragment shows the preferences for the About header.
 */
public class AboutPreferenceFragment extends PowerPreferenceFragment {

    @Override
    protected int getPreferencesXml() {
        return R.xml.about_preferences;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        Preference version = findPreference("about.version");
        try {
            version.setSummary(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            // Never should happen with our own package!
        }
        validateIntent(version);
    }
}

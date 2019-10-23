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

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.view.MenuItem

import net.sf.power.monitor.R

/**
 * Preferences activity.
 *
 * @author Moshe Waisberg
 */
class PreferenceActivity : android.preference.PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_PowerFailure_Settings)
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            actionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onBuildHeaders(target: List<android.preference.PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.preference_headers, target)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    override fun isValidFragment(fragmentName: String): Boolean {
        val packageName: String = javaClass.getPackage()!!.name
        return fragmentName.startsWith(packageName)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

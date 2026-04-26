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
package net.sf.power.monitor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import net.sf.power.monitor.model.BatteryState
import net.sf.power.monitor.model.Plugged

/**
 * Battery utilities.
 *
 * @author Moshe Waisberg
 */
object BatteryUtils {

    private var batteryFilter: IntentFilter? = null

    fun getBatteryFilter(): IntentFilter {
        var filter = batteryFilter
        if (filter == null) {
            filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            batteryFilter = filter
        }
        return filter
    }

    fun getBatteryIntent(context: Context): Intent? {
        return context.registerReceiver(null, getBatteryFilter())
    }

    fun getPlugged(context: Context): Plugged {
        return getPlugged(getBatteryIntent(context))
    }

    fun getPlugged(intent: Intent?): Plugged {
        val extras = intent?.extras
        val plugged = extras?.getInt(BatteryManager.EXTRA_PLUGGED, Plugged.PLUGGED_NONE)
            ?: Plugged.PLUGGED_NONE
        return Plugged.of(plugged)
    }

    fun getState(context: Context): BatteryState {
        val intent = getBatteryIntent(context)
        val extras = intent?.extras
        if (extras != null) {
            return BatteryState.of(extras)
        }
        return BatteryState()
    }
}

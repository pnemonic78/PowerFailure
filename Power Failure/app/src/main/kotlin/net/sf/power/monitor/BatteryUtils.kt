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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import timber.log.Timber

/**
 * Battery utilities.
 *
 * @author Moshe Waisberg
 */
object BatteryUtils {

    /**
     * No power source.
     */
    const val BATTERY_PLUGGED_NONE = 0

    /**
     * Power source is an AC charger.
     */
    const val BATTERY_PLUGGED_AC = BatteryManager.BATTERY_PLUGGED_AC

    /**
     * Power source is dock.
     */
    @SuppressLint("InlinedApi")
    const val BATTERY_PLUGGED_DOCK = BatteryManager.BATTERY_PLUGGED_DOCK

    /**
     * Power source is a USB port.
     */
    const val BATTERY_PLUGGED_USB = BatteryManager.BATTERY_PLUGGED_USB

    /**
     * Power source is wireless.
     */
    const val BATTERY_PLUGGED_WIRELESS = BatteryManager.BATTERY_PLUGGED_WIRELESS

    const val BATTERY_STATUS_CHARGING = BatteryManager.BATTERY_STATUS_CHARGING
    const val BATTERY_STATUS_DISCHARGING = BatteryManager.BATTERY_STATUS_DISCHARGING
    const val BATTERY_STATUS_FULL = BatteryManager.BATTERY_STATUS_FULL
    const val BATTERY_STATUS_NOT_CHARGING = BatteryManager.BATTERY_STATUS_NOT_CHARGING
    const val BATTERY_STATUS_UNKNOWN = BatteryManager.BATTERY_STATUS_UNKNOWN

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

    fun getPlugged(context: Context): Int {
        return getPlugged(getBatteryIntent(context))
    }

    fun getPlugged(intent: Intent?): Int {
        val extras = intent?.extras
        return extras?.getInt(BatteryManager.EXTRA_PLUGGED, BATTERY_PLUGGED_NONE)
            ?: BATTERY_PLUGGED_NONE
    }

    fun isPlugged(context: Context): Boolean {
        return getPlugged(context) != BATTERY_PLUGGED_NONE
    }

    fun isPlugged(intent: Intent): Boolean {
        return getPlugged(intent) != BATTERY_PLUGGED_NONE
    }

    fun printStatus(context: Context) {
        printStatus(getBatteryIntent(context))
    }

    fun printStatus(intent: Intent?) {
        val extras = intent?.extras
        if (extras != null) {
            val plugged = extras.getInt(BatteryManager.EXTRA_PLUGGED, -1)
            val pluggedLabel = when (plugged) {
                BATTERY_PLUGGED_AC -> "AC"
                BATTERY_PLUGGED_DOCK -> "DOCK"
                BATTERY_PLUGGED_NONE -> "NONE"
                BATTERY_PLUGGED_USB -> "USB"
                BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
                else -> plugged.toString()
            }

            val present = extras.getBoolean(BatteryManager.EXTRA_PRESENT, false)
            val status = extras.getInt(BatteryManager.EXTRA_STATUS, -1)
            val statusLabel = when (status) {
                BATTERY_STATUS_CHARGING -> "CHARGING"
                BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
                BATTERY_STATUS_FULL -> "FULL"
                BATTERY_STATUS_NOT_CHARGING -> "NOT_CHARGING"
                BATTERY_STATUS_UNKNOWN -> "UNKNOWN"
                else -> status.toString()
            }
            Timber.i("{plugged:$pluggedLabel, present:$present, status:$statusLabel}")
        }
    }
}

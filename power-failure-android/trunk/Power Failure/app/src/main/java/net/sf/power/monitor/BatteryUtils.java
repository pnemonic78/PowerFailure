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
package net.sf.power.monitor;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Battery utilities.
 *
 * @author Moshe Waisberg
 */
public class BatteryUtils {

    private static final String TAG = "BatteryUtils";

    /**
     * No power source.
     */
    public static final int BATTERY_PLUGGED_NONE = 0;
    /**
     * Power source is an AC charger.
     */
    public static final int BATTERY_PLUGGED_AC = BatteryManager.BATTERY_PLUGGED_AC;
    /**
     * Power source is a USB port.
     */
    public static final int BATTERY_PLUGGED_USB = BatteryManager.BATTERY_PLUGGED_USB;
    /**
     * Power source is wireless.
     */
    public static final int BATTERY_PLUGGED_WIRELESS = 4;//BatteryManager.BATTERY_PLUGGED_WIRELESS;

    private static IntentFilter batteryFilter;

    private BatteryUtils() {
    }

    public static IntentFilter getBatteryFilter() {
        if (batteryFilter == null) {
            batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        }
        return batteryFilter;
    }

    public static Intent getBatteryIntent(Context context) {
        return context.registerReceiver(null, getBatteryFilter());
    }

    public static int getPlugged(Context context) {
        return getPlugged(getBatteryIntent(context));
    }

    public static int getPlugged(Intent intent) {
        Bundle extras = intent.getExtras();
        return extras.getInt(BatteryManager.EXTRA_PLUGGED, BATTERY_PLUGGED_NONE);
    }

    public static boolean isPlugged(Context context) {
        return getPlugged(context) != BATTERY_PLUGGED_NONE;
    }

    public static boolean isPlugged(Intent intent) {
        return getPlugged(intent) != BATTERY_PLUGGED_NONE;
    }

    public static void printStatus(Context context) {
        printStatus(getBatteryIntent(context));
    }

    public static void printStatus(Intent intent) {
        Bundle extras = intent.getExtras();
        int plugged = extras.getInt(BatteryManager.EXTRA_PLUGGED, -1);
        boolean present = extras.getBoolean(BatteryManager.EXTRA_PRESENT, false);
        int status = extras.getInt(BatteryManager.EXTRA_STATUS, -1);
        Log.i(TAG, "{status:" + status + ", plugged:" + plugged + ", present:" + present + "}");
    }
}
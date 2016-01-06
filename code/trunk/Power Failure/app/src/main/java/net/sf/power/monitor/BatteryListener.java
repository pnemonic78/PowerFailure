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
package net.sf.power.monitor;

/**
 * Created by moshe.w on 03/01/2016.
 */
public interface BatteryListener {

    int BATTERY_PLUGGED_NONE = BatteryUtils.BATTERY_PLUGGED_NONE;
    int BATTERY_PLUGGED_AC = BatteryUtils.BATTERY_PLUGGED_AC;
    int BATTERY_PLUGGED_USB = BatteryUtils.BATTERY_PLUGGED_USB;
    int BATTERY_PLUGGED_WIRELESS = BatteryUtils.BATTERY_PLUGGED_WIRELESS;

    /**
     * Notification that the battery is being been charged.
     *
     * @param plugged the type of plugged-in device.
     */
    void onBatteryPlugged(int plugged);

}

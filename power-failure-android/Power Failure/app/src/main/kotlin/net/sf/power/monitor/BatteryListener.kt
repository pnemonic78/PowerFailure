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

/**
 * Battery status listener interface.
 *
 * @author Moshe Waisberg
 */
interface BatteryListener {

    /**
     * Notification that the battery is being been charged.
     *
     * @param plugged the type of plugged-in device.
     */
    fun onBatteryPlugged(plugged: Int)

    companion object {
        const val BATTERY_PLUGGED_NONE = BatteryUtils.BATTERY_PLUGGED_NONE
        const val BATTERY_PLUGGED_AC = BatteryUtils.BATTERY_PLUGGED_AC
        const val BATTERY_PLUGGED_USB = BatteryUtils.BATTERY_PLUGGED_USB
        const val BATTERY_PLUGGED_WIRELESS = BatteryUtils.BATTERY_PLUGGED_WIRELESS
    }

}

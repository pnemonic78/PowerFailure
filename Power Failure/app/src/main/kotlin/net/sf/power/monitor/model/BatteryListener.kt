package net.sf.power.monitor.model

/**
 * Battery state listener interface.
 *
 * @author Moshe Waisberg
 */
interface BatteryListener {

    /**
     * Notification that the battery is being been charged.
     *
     * @param plugged the type of plugged-in device.
     */
    fun onBatteryPlugged(plugged: Plugged)
}
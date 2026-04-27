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
     * @param state the battery state.
     */
    fun onBatteryState(state: BatteryState)
}
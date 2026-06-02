package net.sf.power.monitor.model

import android.os.BatteryManager
import android.os.Bundle
import net.sf.power.monitor.NEVER
import net.sf.power.monitor.TimeMillis

data class BatteryState(
    val plugged: Plugged,
    /** Get the battery status. */
    val status: BatteryStatus,
    /** Check whether the hardware has a battery. */
    val isPresent: Boolean,
    val timestamp: TimeMillis
) {
    constructor() : this(Plugged.Unknown, BatteryStatus.Unknown, false, NEVER)

    companion object {
        fun of(extras: Bundle): BatteryState {
            val plugged = extras.getInt(BatteryManager.EXTRA_PLUGGED, Plugged.PLUGGED_UNKNOWN)
            val status = extras.getInt(BatteryManager.EXTRA_STATUS, BatteryStatus.STATUS_UNKNOWN)
            val isPresent = extras.getBoolean(BatteryManager.EXTRA_PRESENT, false)
            val timestamp = System.currentTimeMillis()

            return BatteryState(Plugged.of(plugged), BatteryStatus.of(status), isPresent, timestamp)
        }
    }
}
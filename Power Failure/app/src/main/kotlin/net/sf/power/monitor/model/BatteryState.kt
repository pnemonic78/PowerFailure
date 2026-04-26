package net.sf.power.monitor.model

import android.os.BatteryManager
import android.os.Bundle

data class BatteryState(
    val plugged: Plugged,
    /** Get the battery status. */
    val status: BatteryStatus = BatteryStatus.Unknown,
    /** Check whether the hardware has a battery. */
    val isPresent: Boolean,
) {
    constructor() : this(Plugged.Unknown, BatteryStatus.Unknown, false)

    companion object {
        fun of(extras: Bundle): BatteryState {
            val plugged = extras.getInt(BatteryManager.EXTRA_PLUGGED, Plugged.PLUGGED_UNKNOWN)
            val status = extras.getInt(BatteryManager.EXTRA_STATUS, BatteryStatus.STATUS_UNKNOWN)
            val isPresent = extras.getBoolean(BatteryManager.EXTRA_PRESENT, false)

            return BatteryState(Plugged.of(plugged), BatteryStatus.of(status), isPresent)
        }
    }
}
package net.sf.power.monitor.model

import android.os.BatteryManager
import android.os.Build

sealed class Plugged(val source: Int, val name: String) {
    object None : Plugged(PLUGGED_NONE, "NONE")
    object AC : Plugged(PLUGGED_AC, "AC")
    object Dock : Plugged(PLUGGED_DOCK, "DOCK")
    object USB : Plugged(PLUGGED_USB, "USB")
    object Wireless : Plugged(PLUGGED_WIRELESS, "WIRELESS")
    object Unknown : Plugged(PLUGGED_UNKNOWN, "UNKNOWN")

    override fun toString(): String {
        return name
    }

    companion object {
        /**
         * No power source.
         */
        const val PLUGGED_NONE = 0

        /**
         * Power source is an AC charger.
         */
        const val PLUGGED_AC = BatteryManager.BATTERY_PLUGGED_AC

        /**
         * Power source is dock.
         */
        val PLUGGED_DOCK = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BatteryManager.BATTERY_PLUGGED_DOCK
        } else {
            8
        }

        /**
         * Power source is a USB port.
         */
        const val PLUGGED_USB = BatteryManager.BATTERY_PLUGGED_USB

        /**
         * Power source is wireless.
         */
        const val PLUGGED_WIRELESS = BatteryManager.BATTERY_PLUGGED_WIRELESS

        const val PLUGGED_UNKNOWN = -1

        private val entries by lazy {
            mapOf(
                PLUGGED_NONE to None,
                PLUGGED_AC to AC,
                PLUGGED_DOCK to Dock,
                PLUGGED_USB to USB,
                PLUGGED_WIRELESS to Wireless,
                PLUGGED_UNKNOWN to Unknown,
            )
        }

        fun of(plugged: Int): Plugged {
            return entries[plugged] ?: Unknown
        }
    }
}
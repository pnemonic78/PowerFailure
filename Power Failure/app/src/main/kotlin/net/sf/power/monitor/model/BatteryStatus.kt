package net.sf.power.monitor.model

import android.os.BatteryManager
import kotlin.lazy

sealed class BatteryStatus(val source: Int, val name: String) {
    object Charging : BatteryStatus(STATUS_CHARGING, "CHARGING")
    object Discharging : BatteryStatus(STATUS_DISCHARGING, "DISCHARGING")
    object Full : BatteryStatus(STATUS_FULL, "FULL")
    object NotCharging : BatteryStatus(STATUS_NOT_CHARGING, "NOT CHARGING")
    object Unknown : BatteryStatus(STATUS_UNKNOWN, "UNKNOWN")

    override fun toString(): String {
        return name
    }

    companion object {
        const val STATUS_CHARGING = BatteryManager.BATTERY_STATUS_CHARGING
        const val STATUS_DISCHARGING = BatteryManager.BATTERY_STATUS_DISCHARGING
        const val STATUS_FULL = BatteryManager.BATTERY_STATUS_FULL
        const val STATUS_NOT_CHARGING = BatteryManager.BATTERY_STATUS_NOT_CHARGING
        const val STATUS_UNKNOWN = BatteryManager.BATTERY_STATUS_UNKNOWN

        private val entries by lazy {
            mapOf(
                STATUS_CHARGING to Charging,
                STATUS_DISCHARGING to Discharging,
                STATUS_FULL to Full,
                STATUS_NOT_CHARGING to NotCharging,
                STATUS_UNKNOWN to Unknown,
            )
        }

        fun of(status: Int): BatteryStatus {
            return entries[status] ?: Unknown
        }
    }
}
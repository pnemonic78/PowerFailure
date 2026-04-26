package net.sf.power.monitor.model

import net.sf.power.monitor.model.BatteryStatus.Companion.STATUS_CHARGING
import net.sf.power.monitor.model.BatteryStatus.Companion.STATUS_DISCHARGING
import net.sf.power.monitor.model.BatteryStatus.Companion.STATUS_FULL
import net.sf.power.monitor.model.BatteryStatus.Companion.STATUS_NOT_CHARGING
import net.sf.power.monitor.model.BatteryStatus.Companion.STATUS_UNKNOWN
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusTest {
    @Test
    fun of() {
        assertEquals(BatteryStatus.Charging, BatteryStatus.of(STATUS_CHARGING))
        assertEquals(BatteryStatus.Discharging, BatteryStatus.of(STATUS_DISCHARGING))
        assertEquals(BatteryStatus.Full, BatteryStatus.of(STATUS_FULL))
        assertEquals(BatteryStatus.NotCharging, BatteryStatus.of(STATUS_NOT_CHARGING))
        assertEquals(BatteryStatus.Unknown, BatteryStatus.of(STATUS_UNKNOWN))
        assertEquals(BatteryStatus.Unknown, BatteryStatus.of(-1))
        assertEquals(BatteryStatus.Unknown, BatteryStatus.of(Int.MAX_VALUE))
    }
}
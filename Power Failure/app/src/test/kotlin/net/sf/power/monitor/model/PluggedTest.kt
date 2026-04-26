package net.sf.power.monitor.model

import net.sf.power.monitor.model.Plugged.Companion.PLUGGED_AC
import net.sf.power.monitor.model.Plugged.Companion.PLUGGED_DOCK
import net.sf.power.monitor.model.Plugged.Companion.PLUGGED_NONE
import net.sf.power.monitor.model.Plugged.Companion.PLUGGED_UNKNOWN
import net.sf.power.monitor.model.Plugged.Companion.PLUGGED_USB
import net.sf.power.monitor.model.Plugged.Companion.PLUGGED_WIRELESS
import org.junit.Assert.assertEquals
import org.junit.Test

class PluggedTest {
    @Test
    fun of() {
        assertEquals(Plugged.None, Plugged.of(PLUGGED_NONE))
        assertEquals(Plugged.AC, Plugged.of(PLUGGED_AC))
        assertEquals(Plugged.Dock, Plugged.of(PLUGGED_DOCK))
        assertEquals(Plugged.USB, Plugged.of(PLUGGED_USB))
        assertEquals(Plugged.Wireless, Plugged.of(PLUGGED_WIRELESS))
        assertEquals(Plugged.Unknown, Plugged.of(PLUGGED_UNKNOWN))
        assertEquals(Plugged.Unknown, Plugged.of(Int.MAX_VALUE))
    }
}
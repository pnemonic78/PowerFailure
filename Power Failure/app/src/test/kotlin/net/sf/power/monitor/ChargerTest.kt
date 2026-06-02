package net.sf.power.monitor

import android.net.Uri
import com.github.util.TimeUtils.HOUR_IN_MILLIS
import com.github.util.TimeUtils.MINUTE_IN_MILLIS
import com.github.util.TimeUtils.SECOND_IN_MILLIS
import net.sf.power.monitor.model.Plugged
import net.sf.power.monitor.preference.PowerPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargerTest {
    private fun now(): TimeMillis = System.currentTimeMillis()
    private fun nextTick(): TimeMillis = now() + TICK
    private fun later(): TimeMillis = now() + MANY_TICKS

    private open class TestPowerPreferences : PowerPreferences {
        override val ringtone: Uri? = null
        override val isVibrate: Boolean = false
        override val failureDelay: TimeMillis = 10 * SECOND_IN_MILLIS
        override var failureTime: TimeMillis = NEVER
        override var restoredTime: TimeMillis = NEVER
        override var isSmsEnabled: Boolean = false
        override var smsRecipient: String = ""
    }

    private val settingsDefault = TestPowerPreferences()

    private val settingsFailed = object : TestPowerPreferences() {
        init {
            failureTime = now() - HOUR_IN_MILLIS
        }
    }

    private val settingsRestored = object : TestPowerPreferences() {
        init {
            failureTime = now() - HOUR_IN_MILLIS
            restoredTime = now() - MINUTE_IN_MILLIS
        }
    }

    private val plugged = Plugged.AC
    private val unplugged = Plugged.None

    @Test
    fun `was nothing and plugged and then still plugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsDefault)

        // was nothing
        assertEquals(ChargerStateMachine.State.Initial, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and plugged
        stateMachine.next(plugged, now())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then still plugged
        stateMachine.next(plugged, nextTick())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then still plugged (restored only after failure)
        stateMachine.next(plugged, later())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
    }

    @Test
    fun `was nothing and plugged and then unplugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsDefault)

        // was nothing
        assertEquals(ChargerStateMachine.State.Initial, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and plugged
        stateMachine.next(plugged, now())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then unplugged
        stateMachine.next(unplugged, nextTick())
        assertEquals(ChargerStateMachine.State.Unplugged, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then fail
        val timestamp = later()
        stateMachine.next(unplugged, timestamp)
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, timestamp)
        assertEquals(stateMachine.restoredTime.value, NEVER)
    }

    @Test
    fun `was nothing and unplugged and then still unplugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsDefault)

        // was nothing
        assertEquals(ChargerStateMachine.State.Initial, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and unplugged
        stateMachine.next(unplugged, now())
        assertEquals(ChargerStateMachine.State.Unplugged, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then still unplugged
        stateMachine.next(unplugged, nextTick())
        assertEquals(ChargerStateMachine.State.Unplugged, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then fail
        val timestamp = later()
        stateMachine.next(unplugged, timestamp)
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.failedTime.value, timestamp)
    }

    @Test
    fun `was nothing and unplugged and then plugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsDefault)

        // was nothing
        assertEquals(ChargerStateMachine.State.Initial, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and unplugged
        stateMachine.next(unplugged, now())
        assertEquals(ChargerStateMachine.State.Unplugged, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then plugged
        stateMachine.next(plugged, nextTick())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then still plugged
        stateMachine.next(plugged, later())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
    }

    @Test
    fun `was failed and plugged and then still plugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsFailed)

        // was failed
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and plugged
        stateMachine.next(plugged, now())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then still plugged
        stateMachine.next(plugged, nextTick())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then still plugged
        val timestamp = later()
        stateMachine.next(plugged, timestamp)
        assertEquals(ChargerStateMachine.State.Restored, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)
    }

    @Test
    fun `was failed and plugged and then unplugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsFailed)

        // was failed
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and plugged
        stateMachine.next(plugged, now())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then unplugged
        stateMachine.next(unplugged, nextTick())
        assertEquals(ChargerStateMachine.State.Unplugged, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then still unplugged
        val timestamp = later()
        stateMachine.next(unplugged, timestamp)
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.failedTime.value, timestamp)
    }

    @Test
    fun `was failed and restored and then fail`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsFailed)

        // was failed
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then plugged
        stateMachine.next(plugged, now())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)

        // and then restored
        stateMachine.next(plugged, later())
        assertEquals(ChargerStateMachine.State.Restored, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)

        // and then unplugged
        stateMachine.next(unplugged, later() + TICK)
        assertEquals(ChargerStateMachine.State.Unplugged, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)

        // and then fail
        val timestamp = later() + MANY_TICKS
        stateMachine.next(unplugged, timestamp)
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.failedTime.value, timestamp)
    }

    @Test
    fun `was failed and unplugged and then still unplugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsFailed)
        val timestamp = stateMachine.failedTime.value

        // was failed
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.failedTime.value, timestamp)

        // and unplugged
        stateMachine.next(unplugged, nextTick())
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.failedTime.value, timestamp)

        // and then still unplugged
        stateMachine.next(unplugged, later())
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.failedTime.value, timestamp)
    }

    @Test
    fun `was failed and unplugged and then plugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsFailed)
        val timestamp = stateMachine.failedTime.value

        // was failed
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.failedTime.value, timestamp)

        // and unplugged
        stateMachine.next(unplugged, nextTick())
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.failedTime.value, timestamp)

        // and then plugged
        stateMachine.next(plugged, nextTick())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.failedTime.value, timestamp)
    }

    @Test
    fun `was restored and plugged and then still plugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsRestored)
        val timestamp = stateMachine.restoredTime.value

        // was restored
        assertEquals(ChargerStateMachine.State.Restored, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)

        // and plugged
        stateMachine.next(plugged, nextTick())
        assertEquals(ChargerStateMachine.State.Restored, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)

        // and then still plugged
        stateMachine.next(plugged, later())
        assertEquals(ChargerStateMachine.State.Restored, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)
    }

    @Test
    fun `was restored and plugged and then unplugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsRestored)
        val timestamp = stateMachine.restoredTime.value

        // was restored
        assertEquals(ChargerStateMachine.State.Restored, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)

        // and plugged
        stateMachine.next(plugged, nextTick())
        assertEquals(ChargerStateMachine.State.Restored, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)

        // and then unplugged
        stateMachine.next(unplugged, later())
        assertEquals(ChargerStateMachine.State.Unplugged, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)
    }

    @Test
    fun `was restored and unplugged and then still unplugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsRestored)
        val timestamp = stateMachine.restoredTime.value

        // was restored
        assertEquals(ChargerStateMachine.State.Restored, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)

        // and unplugged
        stateMachine.next(unplugged, nextTick())
        assertEquals(ChargerStateMachine.State.Unplugged, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)

        // and then still unplugged
        stateMachine.next(unplugged, later())
        assertNotNull(stateMachine.state)
        assertEquals(ChargerStateMachine.State.Failed, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, NEVER)
    }

    @Test
    fun `was restored and unplugged and then plugged`() {
        val stateMachine = ChargerStateMachine()
        stateMachine.applySettings(settingsRestored)
        val timestamp = stateMachine.restoredTime.value

        // was restored
        assertEquals(ChargerStateMachine.State.Restored, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)

        // and unplugged
        stateMachine.next(unplugged, nextTick())
        assertEquals(ChargerStateMachine.State.Unplugged, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)

        // and then plugged
        stateMachine.next(plugged, nextTick())
        assertEquals(ChargerStateMachine.State.Plugged, stateMachine.state)
        assertNotEquals(stateMachine.failedTime.value, NEVER)
        assertNotEquals(stateMachine.restoredTime.value, NEVER)
        assertEquals(stateMachine.restoredTime.value, timestamp)
        assertTrue(stateMachine.failedTime.value < stateMachine.restoredTime.value)
    }

    companion object {
        private const val TICK = SECOND_IN_MILLIS
        private const val MANY_TICKS = MINUTE_IN_MILLIS
    }
}
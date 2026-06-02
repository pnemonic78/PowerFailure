package net.sf.power.monitor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import net.sf.power.monitor.model.BatteryState
import net.sf.power.monitor.model.Plugged
import net.sf.power.monitor.preference.PowerPreferences

class ChargerStateMachine {
    enum class State {
        Initial,
        Plugged,
        Unplugged,
        Failed,
        Restored,
    }

    private val _failedTime = MutableStateFlow(NEVER)
    val failedTime: StateFlow<TimeMillis> = _failedTime

    private val _restoredTime = MutableStateFlow(NEVER)
    val restoredTime: StateFlow<TimeMillis> = _restoredTime

    private var failureDelay: TimeMillis = 10
    private var powerFailureSince: TimeMillis = NEVER
    private var powerRestoredSince: TimeMillis = NEVER
    private var stateTime: TimeMillis = NEVER

    var state: State = State.Initial
        private set

    init {
        reset()
    }

    fun reset() {
        state = State.Initial
        powerFailureSince = NEVER
        powerRestoredSince = NEVER
    }

    fun applySettings(settings: PowerPreferences) {
        failureDelay = settings.failureDelay

        val failureTime = settings.failureTime
        _failedTime.update { failureTime }
        if (failureTime != NEVER) {
            state = State.Failed
        }

        val restoredTime = settings.restoredTime
        _restoredTime.update { restoredTime }
        if (restoredTime != NEVER && failureTime < restoredTime) {
            state = State.Restored
        }
    }

    fun next(state: BatteryState) {
        next(state.plugged, state.timestamp)
    }

    fun next(plugged: Plugged, timestamp: TimeMillis) {
        if (timestamp <= NEVER) return
        if (timestamp < stateTime) return

        when (state) {
            State.Initial -> {
                if (isPlugged(plugged)) {
                    state = State.Plugged
                } else {
                    state = State.Unplugged
                    startFailureTimer(timestamp)
                }
            }

            State.Plugged -> {
                if (isPlugged(plugged)) {
                    if (wasFailure(timestamp)) {
                        checkRestoreTimer(timestamp)
                    }
                } else {
                    state = State.Unplugged
                    startFailureTimer(timestamp)
                }
            }

            State.Unplugged -> {
                if (isPlugged(plugged)) {
                    state = State.Plugged
                    startRestoreTimer(timestamp)
                } else {
                    checkFailureTimer(timestamp)
                }
            }

            State.Failed -> {
                if (isPlugged(plugged)) {
                    state = State.Plugged
                    startRestoreTimer(timestamp)
                }
            }

            State.Restored -> {
                if (!isPlugged(plugged)) {
                    state = State.Unplugged
                    startFailureTimer(timestamp)
                }
            }
        }

        stateTime = timestamp
    }

    private fun checkFailureTimer(timestamp: TimeMillis) {
        if (timestamp >= powerFailureSince + failureDelay) {
            notifyFailure(timestamp)
        }
    }

    private fun checkRestoreTimer(timestamp: TimeMillis) {
        if (timestamp >= powerRestoredSince + failureDelay) {
            notifyRestore(timestamp)
        }
    }

    private fun isPlugged(plugged: Plugged): Boolean {
        return plugged != Plugged.None
    }

    private fun startFailureTimer(timestamp: TimeMillis) {
        powerFailureSince = timestamp
    }

    private fun startRestoreTimer(timestamp: TimeMillis) {
        powerRestoredSince = timestamp
    }

    private fun wasFailure(timestamp: TimeMillis): Boolean {
        val failedTime = failedTime.value
        val restoredTime = restoredTime.value
        return (failedTime > NEVER)
                && (failedTime >= restoredTime)
                && (failedTime < timestamp)
    }

    private fun wasRestore(timestamp: TimeMillis): Boolean {
        val failedTime = failedTime.value
        val restoredTime = restoredTime.value
        return (failedTime > NEVER)
                && (restoredTime > NEVER)
                && (failedTime < restoredTime)
                && (restoredTime < timestamp)
    }

    private fun notifyFailure(timestamp: TimeMillis) {
        state = State.Failed
        _failedTime.update { timestamp }
        _restoredTime.update { NEVER }
    }

    private fun notifyRestore(timestamp: TimeMillis) {
        state = State.Restored
        _restoredTime.update { timestamp }
    }
}
package net.sf.power.monitor

import android.content.Context
import android.text.format.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.sf.power.monitor.model.BatteryState
import net.sf.power.monitor.model.Plugged
import net.sf.power.monitor.preference.PowerPreferences
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArraySet

class ChargerPoll(
    private val context: Context,
    private val lifecycleScope: CoroutineScope
) {
    private val _state = MutableStateFlow(BatteryState())
    val state: StateFlow<BatteryState> = _state

    private val _failedTime = MutableStateFlow(NEVER)
    val failedTime: StateFlow<TimeMillis> = _failedTime

    private val _restoredTime = MutableStateFlow(NEVER)
    val restoredTime: StateFlow<TimeMillis> = _restoredTime

    private var pollingJob: Job? = null
    val isPolling: Boolean get() = pollingJob != null && pollingJob?.isCancelled == false

    private val owners = CopyOnWriteArraySet<Any>()
    private lateinit var settings: PowerPreferences

    private var powerFailureSince: TimeMillis = NEVER
    private var powerSince: TimeMillis = NEVER
    private var failureDelay: TimeMillis = 10

    fun start(owner: Any, settings: PowerPreferences) {
        this.settings = settings
        failureDelay = settings.failureDelay
        powerSince = NEVER
        powerFailureSince = NEVER

        if (owners.contains(owner)) {
            return
        }
        owners.add(owner)

        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch {
            do {
                pollCharger()
                delay(POLL_RATE)
            } while (isPolling)
        }
    }

    private fun pollCharger() {
        val context: Context = context
        val state: BatteryState = BatteryUtils.getState(context)
        handleBatteryState(state)
    }

    fun stop(owner: Any) {
        owners.remove(owner)
        if (owners.isEmpty()) {
            cancel()
        }
    }

    private fun cancel() {
        pollingJob?.cancel()
        pollingJob = null
    }

    internal fun kill() {
        cancel()
        owners.clear()
    }

    private fun handleBatteryState(state: BatteryState) {
        Timber.i("$state")
        _state.update { state }

        val plugged = state.plugged
        val now = System.currentTimeMillis()

        if (plugged != Plugged.None) {
            powerSince = now
            if (powerFailureSince > NEVER) {
                if (now >= powerFailureSince + failureDelay) {
                    powerFailureSince = NEVER
                    handleRestore(now)
                }
            } else {
                powerFailureSince = NEVER
            }
        } else if (now >= powerSince + failureDelay) {
            if (powerFailureSince <= NEVER) {
                powerFailureSince = now
                handleFailure(now)
            }
        }
    }

    private fun handleFailure(timestamp: TimeMillis) {
        Timber.i("power failed $timestamp")
        _failedTime.update { timestamp }
        settings.failureTime = timestamp
        settings.restoredTime = NEVER
    }

    private fun handleRestore(timestamp: TimeMillis) {
        Timber.i("power restored $timestamp")
        _restoredTime.update { timestamp }
        settings.restoredTime = timestamp
    }

    internal fun fail() {
        handleFailure(System.currentTimeMillis())
    }

    companion object {
        private const val POLL_RATE = DateUtils.SECOND_IN_MILLIS
    }
}
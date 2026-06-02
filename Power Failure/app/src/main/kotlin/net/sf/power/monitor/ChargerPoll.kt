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
import net.sf.power.monitor.preference.PowerPreferences
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArraySet

class ChargerPoll(
    private val context: Context,
    private val lifecycleScope: CoroutineScope
) {
    private val _state = MutableStateFlow(BatteryState())
    val state: StateFlow<BatteryState> = _state

    private val stateMachine = ChargerStateMachine()
    val failedTime: StateFlow<TimeMillis> = stateMachine.failedTime
    val restoredTime: StateFlow<TimeMillis> = stateMachine.restoredTime

    private var pollingJob: Job? = null
    val isPolling: Boolean get() = pollingJob != null && pollingJob?.isCancelled == false

    private val owners = CopyOnWriteArraySet<Any>()
    private lateinit var settings: PowerPreferences

    init {
        lifecycleScope.launch {
            failedTime.collect {
                handleFailure(it)
            }
        }
        lifecycleScope.launch {
            restoredTime.collect {
                handleRestore(it)
            }
        }
    }

    fun start(owner: Any, settings: PowerPreferences) {
        if (!owners.contains(owner)) {
            owners.add(owner)
        }

        this.settings = settings

        stateMachine.reset()
        stateMachine.applySettings(settings)

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
        stateMachine.next(state)
    }

    private fun handleFailure(timestamp: TimeMillis) {
        Timber.i("power failed $timestamp")
        if (::settings.isInitialized) {
            settings.failureTime = timestamp
            settings.restoredTime = NEVER
        }
    }

    private fun handleRestore(timestamp: TimeMillis) {
        Timber.i("power restored $timestamp")
        if (::settings.isInitialized) {
            settings.restoredTime = timestamp
        }
    }

    internal fun fail() {
        handleFailure(System.currentTimeMillis())
    }

    companion object {
        private const val POLL_RATE = DateUtils.SECOND_IN_MILLIS
    }
}
package net.sf.power.monitor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.sf.power.monitor.model.BatteryListener
import net.sf.power.monitor.model.BatteryState
import net.sf.power.monitor.model.Command
import net.sf.power.monitor.preference.PowerPreferences
import net.sf.power.monitor.preference.PowerPreferences.Companion.NEVER

class MonitorViewModel(application: Application) : AndroidViewModel(application), BatteryListener {
    private val _state = MutableStateFlow(BatteryState())
    val state: StateFlow<BatteryState> = _state

    private val _command = MutableSharedFlow<Command>(1)
    val command: Flow<Command> = _command

    private val _polling = MutableStateFlow(false)
    var isPolling: StateFlow<Boolean> = _polling

    private val _failedTime = MutableStateFlow(NEVER)
    val failedTime: Flow<TimeMillis> = _failedTime

    private val _restoredTime = MutableStateFlow(NEVER)
    val restoredTime: Flow<TimeMillis> = _restoredTime

    private val settings: PowerPreferences = PowerPreferences(application)

    init {
        viewModelScope.launch {
            _failedTime.update { settings.failureTime }
            _restoredTime.update { settings.restoredTime }
        }
    }

    fun onStartClick() {
        sendCommand(Command.StartMonitor)
    }

    fun onStopClick() {
        sendCommand(Command.StopMonitor)
    }

    fun onSettingsClick() {
        sendCommand(Command.Settings)
    }

    fun onTestClick() {
        sendCommand(Command.Test)
    }

    fun onActionButtonClick() {
        if (isPolling.value) {
            stop()
        } else {
            start()
        }
    }

    fun start() {
        sendCommand(Command.StartMonitor)
    }

    fun stop() {
        sendCommand(Command.StopMonitor)
    }

    private fun sendCommand(command: Command) {
        viewModelScope.launch {
            _command.emit(command)
        }
    }

    override fun onBatteryState(state: BatteryState) {
        viewModelScope.launch {
            _state.update { state }
        }
    }

    fun setMonitorStatus(polling: Boolean) {
        _polling.update { polling }
    }

    fun onPowerFailed(timeMillis: TimeMillis) {
        settings.failureTime = timeMillis
        viewModelScope.launch {
            _failedTime.update { timeMillis }
        }
    }

    fun onPowerRestored(timeMillis: TimeMillis) {
        settings.restoredTime = timeMillis
        viewModelScope.launch {
            _restoredTime.update { timeMillis }
        }
    }
}
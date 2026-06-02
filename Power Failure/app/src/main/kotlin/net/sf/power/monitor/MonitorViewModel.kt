package net.sf.power.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.sf.power.monitor.model.BatteryState
import net.sf.power.monitor.model.Command
import net.sf.power.monitor.preference.PowerPreferences

class MonitorViewModel(
    private val poll: ChargerPoll,
    private val settings: PowerPreferences
) : ViewModel() {
    val state: StateFlow<BatteryState> = poll.state
    val failedTime: StateFlow<TimeMillis> = poll.failedTime
    val restoredTime: StateFlow<TimeMillis> = poll.restoredTime

    private val _command = MutableSharedFlow<Command>(1)
    val command: Flow<Command> = _command

    private val _monitoring = MutableStateFlow(false)
    var isMonitoring: StateFlow<Boolean> = _monitoring

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
        if (isMonitoring.value) {
            sendCommand(Command.StopMonitor)
        } else {
            sendCommand(Command.StartMonitor)
        }
    }

    fun start() {
        poll.start(this, settings)
    }

    fun stop() {
        poll.stop(this)
    }

    private fun sendCommand(command: Command) {
        viewModelScope.launch {
            _command.emit(command)
        }
    }

    fun setMonitorStatus(polling: Boolean) {
        _monitoring.update { polling }
    }
}
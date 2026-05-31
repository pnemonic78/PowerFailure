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

class MonitorViewModel(private val settings: PowerPreferences) : ViewModel() {
    private lateinit var poll: ChargerPoll
    private val _state = MutableStateFlow(BatteryState())
    val state: StateFlow<BatteryState> = _state

    private val _command = MutableSharedFlow<Command>(1)
    val command: Flow<Command> = _command

    private val _monitoring = MutableStateFlow(false)
    var isMonitoring: StateFlow<Boolean> = _monitoring

    private val _failedTime = MutableStateFlow(NEVER)
    val failedTime: StateFlow<TimeMillis> = _failedTime

    private val _restoredTime = MutableStateFlow(NEVER)
    val restoredTime: StateFlow<TimeMillis> = _restoredTime

    init {
        viewModelScope.launch {
            _failedTime.update { settings.failureTime }
            _restoredTime.update { settings.restoredTime }
        }
    }

    fun setPoller(poll: ChargerPoll) {
        this.poll = poll
        viewModelScope.launch {
            poll.state.collect {
                onBatteryState(it)
            }
        }
        viewModelScope.launch {
            poll.failedTime.collect {
                onPowerFailed(it)
            }
        }
        viewModelScope.launch {
            poll.restoredTime.collect {
                onPowerRestored(it)
            }
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

    private fun onBatteryState(state: BatteryState) {
        viewModelScope.launch {
            _state.update { state }
        }
    }

    private fun onPowerFailed(timeMillis: TimeMillis) {
        viewModelScope.launch {
            _failedTime.update { timeMillis }
        }
    }

    private fun onPowerRestored(timeMillis: TimeMillis) {
        viewModelScope.launch {
            _restoredTime.update { timeMillis }
        }
    }
}
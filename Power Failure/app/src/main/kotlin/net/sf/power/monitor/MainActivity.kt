/*
 * Copyright 2016, Moshe Waisberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.power.monitor

import android.Manifest
import android.annotation.TargetApi
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.preference.PermitRingtonePreference
import kotlinx.coroutines.launch
import net.sf.power.monitor.compose.AppTheme
import net.sf.power.monitor.compose.MainScreen
import net.sf.power.monitor.model.BatteryState
import net.sf.power.monitor.model.Command
import net.sf.power.monitor.preference.PowerPreferenceActivity
import net.sf.power.monitor.preference.PowerPreferences
import timber.log.Timber

/**
 * Main activity.
 *
 * @author Moshe Waisberg
 */
class MainActivity : AppCompatActivity(), PowerConnectionBinder.BinderListener {

    private val settings by lazy { PowerPreferences(context) }
    private val viewModel by viewModels<MonitorViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MonitorViewModel(settings) as T
            }
        }
    }
    private val binder = PowerConnectionBinder(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                MainScreen(viewModel)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            initNotificationPermissions()
        }
        PermitRingtonePreference.askPermission(this)

        lifecycleScope.launch {
            viewModel.command.collect {
                onCommand(it)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binder.onStart()
    }

    override fun onStop() {
        super.onStop()
        binder.onStop()
    }

    override fun onResume() {
        super.onResume()
        binder.fetchState()
    }

    override val context: Context
        get() = this

    override fun onMonitorStatus(polling: Boolean) {
        Timber.v("onMonitorStatus $polling")
        viewModel.setMonitorStatus(polling)
    }

    override fun onBatteryState(state: BatteryState) {
        viewModel.onBatteryState(state)
    }

    override fun onPowerFailed(timeMillis: TimeMillis) {
        viewModel.onPowerFailed(timeMillis)
    }

    override fun onPowerRestored(timeMillis: TimeMillis) {
        viewModel.onPowerRestored(timeMillis)
    }

    private fun onCommand(command: Command) {
        Timber.v("command $command")
        when (command) {
            Command.Settings -> startActivity(
                Intent(this, PowerPreferenceActivity::class.java)
            )

            Command.StartMonitor -> binder.startMonitor()

            Command.StopMonitor -> binder.stopMonitor()

            Command.Test -> binder.fail()
        }
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermissions(activity: AppCompatActivity) {
        val nm = getNotificationManager()
        if (nm.areNotificationsEnabled()) return
        activity.requestPermissions(PERMISSIONS, ACTIVITY_PERMISSIONS)
    }

    private fun getNotificationManager(): NotificationManager {
        return getSystemService(NotificationManager::class.java)
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    private fun initNotificationPermissions() {
        checkNotificationPermissions(this)
    }

    companion object {
        @TargetApi(Build.VERSION_CODES.TIRAMISU)
        private val PERMISSIONS = arrayOf(Manifest.permission.POST_NOTIFICATIONS)

        /**
         * Activity id for requesting notification permissions.
         */
        private const val ACTIVITY_PERMISSIONS = 0x6057 // "POST"
    }
}

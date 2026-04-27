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
import android.text.format.DateUtils
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.github.preference.PermitRingtonePreference
import kotlinx.coroutines.launch
import net.sf.power.monitor.compose.AppTheme
import net.sf.power.monitor.databinding.ActivityMainBinding
import net.sf.power.monitor.menu.ActionsMenuCollapsed
import net.sf.power.monitor.menu.SettingsButton
import net.sf.power.monitor.menu.StartButton
import net.sf.power.monitor.menu.StopButton
import net.sf.power.monitor.menu.TestButton
import net.sf.power.monitor.model.BatteryState
import net.sf.power.monitor.model.Command
import net.sf.power.monitor.model.Plugged
import net.sf.power.monitor.preference.PowerPreferenceActivity
import net.sf.power.monitor.preference.PowerPreferences
import timber.log.Timber

/**
 * Main activity.
 *
 * @author Moshe Waisberg
 */
class MainActivity : AppCompatActivity(), PowerConnectionBinder.BinderListener {

    private val viewModel by viewModels<MonitorViewModel>()
    private lateinit var binding: ActivityMainBinding
    private val binder = PowerConnectionBinder(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        this.binding = binding
        setContentView(binding.root)
        initView(binding)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            initNotificationPermissions()
        }
        PermitRingtonePreference.askPermission(this)

        lifecycleScope.launch {
            viewModel.command.collect {
                onCommand(it)
            }
        }
        lifecycleScope.launch {
            viewModel.state.collect {
                setBatteryState(it)
            }
        }
        lifecycleScope.launch {
            viewModel.isPolling.collect {
                onMonitorStatus(it)
            }
        }
        lifecycleScope.launch {
            viewModel.failedTime.collect {
                showFailureTime(it)
            }
        }
        lifecycleScope.launch {
            viewModel.restoredTime.collect {
                showRestoredTime(it)
            }
        }
    }

    private fun initView(binding: ActivityMainBinding) {
        val mainView = binding.main

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val actionBar = binding.actionBar
        actionBar.setContent {
            AppTheme {
                val polling by viewModel.isPolling.collectAsState(false)

                ActionsMenuCollapsed { spacing ->
                    if (BuildConfig.DEBUG) {
                        TestButton(onClick = {
                            viewModel.onTestClick()
                        })
                        Spacer(modifier = Modifier.width(spacing))
                    }
                    if (polling) {
                        StopButton(onClick = {
                            viewModel.onStopClick()
                        })
                        Spacer(modifier = Modifier.width(spacing))
                    } else {
                        StartButton(onClick = {
                            viewModel.onStartClick()
                        })
                        Spacer(modifier = Modifier.width(spacing))
                    }
                    SettingsButton(onClick = {
                        viewModel.onSettingsClick()
                    })
                }
            }
        }

        val actionButton = binding.floatingActionButton
        actionButton.setOnClickListener { viewModel.onActionButtonClick() }
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
        Timber.v("polling $polling")
        val mainView = binding.main
        val mainBackground = mainView.background
        mainBackground.level = LEVEL_UNKNOWN
        binding.plugged.setImageLevel(LEVEL_UNKNOWN)

        @DrawableRes val iconId =
            if (polling) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val actionButton = binding.floatingActionButton
        actionButton.setImageResource(iconId)
        onBatteryState(viewModel.state.value)
    }

    override fun onBatteryState(state: BatteryState) {
        viewModel.onBatteryState(state)
    }

    private fun setBatteryState(state: BatteryState) {
        Timber.v("state $state")
        val plugged = state.plugged
        val mainView = binding.main
        val mainBackground = mainView.background
        val pluggedView = binding.plugged

        when (plugged) {
            Plugged.None -> {
                mainBackground.level = LEVEL_UNPLUGGED
                pluggedView.setImageLevel(LEVEL_UNPLUGGED)
                pluggedView.contentDescription = getText(R.string.plugged_unplugged)
            }

            Plugged.AC -> {
                mainBackground.level = LEVEL_PLUGGED_AC
                pluggedView.setImageLevel(LEVEL_PLUGGED_AC)
                pluggedView.contentDescription = getText(R.string.plugged_ac)
            }

            Plugged.Dock -> {
                mainBackground.level = LEVEL_PLUGGED_DOCK
                pluggedView.setImageLevel(LEVEL_PLUGGED_DOCK)
                pluggedView.contentDescription = getText(R.string.plugged_dock)
            }

            Plugged.USB -> {
                mainBackground.level = LEVEL_PLUGGED_USB
                pluggedView.setImageLevel(LEVEL_PLUGGED_USB)
                pluggedView.contentDescription = getText(R.string.plugged_usb)
            }

            Plugged.Wireless -> {
                mainBackground.level = LEVEL_PLUGGED_WIRELESS
                pluggedView.setImageLevel(LEVEL_PLUGGED_WIRELESS)
                pluggedView.contentDescription = getText(R.string.plugged_wireless)
            }

            else -> {
                mainBackground.level = LEVEL_PLUGGED_UNKNOWN
                pluggedView.setImageLevel(LEVEL_PLUGGED_UNKNOWN)
                pluggedView.contentDescription = getText(R.string.plugged_unknown)
            }
        }
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

    private fun showFailureTime(timeMillis: TimeMillis) {
        Timber.v("failure $timeMillis")
        val context: Context = this
        val timeView = binding.failedOn
        if (timeMillis > PowerPreferences.NEVER) {
            val dateTime = DateUtils.formatDateTime(
                context,
                timeMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
            )
            timeView.text = getString(R.string.power_failed_on, dateTime)
            timeView.isVisible = true
        } else {
            timeView.isVisible = false
        }
    }

    private fun showRestoredTime(timeMillis: TimeMillis) {
        Timber.v("restore $timeMillis")
        val context: Context = this
        val timeView = binding.restoredOn
        if (timeMillis > PowerPreferences.NEVER) {
            val dateTime = DateUtils.formatDateTime(
                context,
                timeMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
            )
            timeView.text = getString(R.string.power_restored_on, dateTime)
            timeView.isVisible = true
        } else {
            timeView.isVisible = false
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
        private const val LEVEL_UNKNOWN = 0
        private const val LEVEL_UNPLUGGED = 1
        private const val LEVEL_PLUGGED_AC = 2
        private const val LEVEL_PLUGGED_USB = 3
        private const val LEVEL_PLUGGED_WIRELESS = 4
        private const val LEVEL_PLUGGED_DOCK = 5
        private const val LEVEL_PLUGGED_UNKNOWN = LEVEL_UNKNOWN

        @TargetApi(Build.VERSION_CODES.TIRAMISU)
        private val PERMISSIONS = arrayOf(Manifest.permission.POST_NOTIFICATIONS)

        /**
         * Activity id for requesting notification permissions.
         */
        private const val ACTIVITY_PERMISSIONS = 0x6057 // "POST"
    }
}

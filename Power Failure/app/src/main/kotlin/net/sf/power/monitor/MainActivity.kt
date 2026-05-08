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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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

    private val settings by lazy {
        val context: Context = this
        PowerPreferences(context)
    }
    private val viewModel by viewModels<MonitorViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MonitorViewModel(settings) as T
            }
        }
    }
    private val binder = PowerConnectionBinder(this, this)
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Timber.i("Permission ${it.key} granted: ${it.value}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                MainScreen(viewModel)
            }
        }

        initPermissions()

        lifecycleScope.launch {
            viewModel.command.collect {
                onCommand(it)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binder.start()
    }

    override fun onStop() {
        super.onStop()
        binder.stop()
    }

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

    override fun onBindFailed(error: Exception) {
        Timber.e(error)
        showForegroundWarning()
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

    private fun initPermissions() {
        val missing = PERMISSIONS.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun showForegroundWarning() {
        val context: Context = this

        AlertDialog.Builder(context)
            .setTitle(R.string.title_activity_main)
            .setMessage(R.string.monitor_stopped)
            .setCancelable(false)
            .setNeutralButton(com.github.lib.R.string.menu_settings) { _, _ ->
                showAppPermissions()
            }
            .setNegativeButton(com.github.lib.R.string.cancel) { _, _ ->
                finish()
            }
            .setPositiveButton(com.github.lib.R.string.retry) { _, _ ->
                binder.start()
            }
            .show()
    }

    private fun showAppPermissions() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, "permissions")
        }
        startActivity(intent)
    }

    companion object {
        private val PERMISSIONS = mutableListOf<String>().apply {
            add(PermitRingtonePreference.PERMISSION_RINGTONE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(Manifest.permission.FOREGROUND_SERVICE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
            }
        }
    }
}

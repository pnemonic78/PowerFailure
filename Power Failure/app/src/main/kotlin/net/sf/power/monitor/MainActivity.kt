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
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.preference.PermitRingtonePreference
import kotlinx.coroutines.launch
import net.sf.power.monitor.compose.AppTheme
import net.sf.power.monitor.compose.MainScreen
import net.sf.power.monitor.model.Command
import net.sf.power.monitor.preference.PowerPreferenceActivity
import net.sf.power.monitor.preference.PowerPreferences
import net.sf.power.monitor.preference.SimplePowerPreferences
import timber.log.Timber

/**
 * Main activity.
 *
 * @author Moshe Waisberg
 */
class MainActivity : AppCompatActivity(), ServiceBinder.BinderListener {

    private val settings: PowerPreferences by lazy {
        val context: Context = this
        SimplePowerPreferences(context)
    }
    private val viewModel by viewModels<MonitorViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as PowerMonitorApplication
                return MonitorViewModel(app.poll, settings) as T
            }
        }
    }
    private val binder = ServiceBinder(this, this)
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
        Timber.i("activity created")

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
        Timber.i("start activity")
        viewModel.start()
        binder.onStart()
    }

    override fun onStop() {
        Timber.i("stop activity")
        viewModel.stop()
        binder.onStop()
        super.onStop()
    }

    override fun onMonitorStatus(polling: Boolean) {
        Timber.v("onMonitorStatus polling=$polling")
        viewModel.setMonitorStatus(polling)
    }

    override fun onClientRegistered(registered: Boolean) {
        Timber.v("onClientRegistered registered=$registered")
    }

    private fun onCommand(command: Command) {
        Timber.i("command $command")
        when (command) {
            Command.Settings -> startActivity(
                Intent(this, PowerPreferenceActivity::class.java)
            )

            Command.StartMonitor -> binder.start()

            Command.StopMonitor -> binder.stop()

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

    companion object {
        private val PERMISSIONS = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(Manifest.permission.FOREGROUND_SERVICE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
            }
            add(PermitRingtonePreference.PERMISSION_RINGTONE)
        }
    }
}

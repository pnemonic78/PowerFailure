package net.sf.power.monitor.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import net.sf.power.monitor.BuildConfig
import net.sf.power.monitor.MonitorViewModel
import net.sf.power.monitor.menu.ActionsMenuCollapsed
import net.sf.power.monitor.menu.SettingsButton
import net.sf.power.monitor.menu.StartButton
import net.sf.power.monitor.menu.StopButton
import net.sf.power.monitor.menu.TestButton

@Composable
fun MainMenu(modifier: Modifier = Modifier, viewModel: MonitorViewModel) {
    val polling by viewModel.isPolling.collectAsState(false)

    ActionsMenuCollapsed(modifier = modifier) { spacing ->
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
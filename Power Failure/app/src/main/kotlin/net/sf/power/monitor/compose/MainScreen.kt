package net.sf.power.monitor.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.sf.power.monitor.MonitorViewModel
import net.sf.power.monitor.preference.PowerPreferences
import net.sf.power.monitor.preference.PowerPreferences.Companion.NEVER

@Composable
fun MainScreen(viewModel: MonitorViewModel) {
    val state = viewModel.state.collectAsState()
    val plugged = state.value.plugged
    val isPolling by viewModel.isPolling.collectAsState()
    val failedTime by viewModel.failedTime.collectAsState(NEVER)
    val restoredTime by viewModel.restoredTime.collectAsState(NEVER)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(isPolling) { viewModel.onActionButtonClick() }
        }
    ) { innerPadding ->
        PlugBackground(plugged)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .align(BiasAlignment(0f, -0.6f))
            ) {
                PlugImage(
                    plugged,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                )
                FailedText(
                    failedTime,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                )
                RestoredText(
                    restoredTime,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                )
            }
            MainMenu(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp),
                viewModel = viewModel
            )
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Composable
@Preview
private fun Preview() {
    val context = LocalContext.current
    val settings = PowerPreferences(context)
    val viewModel = MonitorViewModel(settings)

    AppTheme {
        MainScreen(viewModel)
    }
}
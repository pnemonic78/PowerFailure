package net.sf.power.monitor.compose

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.sf.power.monitor.MonitorViewModel
import net.sf.power.monitor.TimeMillis
import net.sf.power.monitor.model.Plugged
import net.sf.power.monitor.preference.PowerPreferences
import net.sf.power.monitor.preference.PowerPreferences.Companion.NEVER

@Composable
fun MainScreen(viewModel: MonitorViewModel) {
    val state = viewModel.state.collectAsState()
    val plugged = state.value.plugged
    val isPolling by viewModel.isPolling.collectAsState()
    val failedTime by viewModel.failedTime.collectAsState(NEVER)
    val restoredTime by viewModel.restoredTime.collectAsState(NEVER)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var pluggedPosition by remember { mutableStateOf(Rect.Zero) }
    val onPluggedPositioned: PositionCallback = { pluggedPosition = it }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(isPolling) { viewModel.onActionButtonClick() }
        }
    ) { innerPadding ->
        PlugBackground(plugged, imagePosition = pluggedPosition)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLandscape) {
                MainScreenLandscape(
                    plugged = plugged,
                    failedTime = failedTime,
                    restoredTime = restoredTime,
                    onPluggedPositioned = onPluggedPositioned
                )
            } else {
                MainScreenPortrait(
                    plugged = plugged,
                    failedTime = failedTime,
                    restoredTime = restoredTime,
                    onPluggedPositioned = onPluggedPositioned
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

private val margin = 16.dp

@Composable
private fun BoxScope.MainScreenPortrait(
    plugged: Plugged,
    failedTime: TimeMillis,
    restoredTime: TimeMillis,
    onPluggedPositioned: PositionCallback
) {
    PlugImage(
        plugged,
        modifier = Modifier
            .align(BiasAlignment(0f, 0.6f))
            .aspectRatio(1f)
            .fillMaxSize()
            .onGloballyPositioned { onPluggedPositioned(it.boundsInRoot()) }
    )
    Column(
        modifier = Modifier
            .align(BiasAlignment(0f, -1f))
            .padding(top = sizeButton + paddingButton + paddingPanel + margin)
    ) {
        FailedText(
            failedTime,
            modifier = Modifier.padding(start = margin, end = margin)
        )
        RestoredText(
            restoredTime,
            modifier = Modifier.padding(start = margin, end = margin, top = 16.dp)
        )
    }
}

@Composable
private fun BoxScope.MainScreenLandscape(
    plugged: Plugged,
    failedTime: TimeMillis,
    restoredTime: TimeMillis,
    onPluggedPositioned: PositionCallback
) {
    PlugImage(
        plugged,
        modifier = Modifier
            .align(BiasAlignment(0.8f, 0f))
            .aspectRatio(1f)
            .fillMaxSize()
            .onGloballyPositioned { onPluggedPositioned(it.boundsInRoot()) }
    )
    Column(
        modifier = Modifier
            .align(BiasAlignment(-1f, 0f))
            .fillMaxWidth(0.5f)
    ) {
        FailedText(
            failedTime,
            modifier = Modifier.padding(start = margin, end = margin)
        )
        RestoredText(
            restoredTime,
            modifier = Modifier.padding(start = margin, end = margin, top = 16.dp)
        )
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
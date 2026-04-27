package net.sf.power.monitor.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.lang.VoidCallback
import net.sf.power.monitor.R

private val sizeIcon = 48.dp

@Composable
fun FloatingActionButton(isPolling: Boolean, onClick: VoidCallback) {
    LargeFloatingActionButton(onClick = onClick) {
        if (isPolling) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = stringResource(R.string.stop_monitor),
                modifier = Modifier.modifierIcon(sizeIcon)
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.start_monitor),
                modifier = Modifier.modifierIcon(sizeIcon)
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun Preview0() {
    AppTheme {
        FloatingActionButton(isPolling = false, onClick = {})
    }
}

@Composable
@Preview(showBackground = true)
private fun Preview1() {
    AppTheme {
        FloatingActionButton(isPolling = true, onClick = {})
    }
}
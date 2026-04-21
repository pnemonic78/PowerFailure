package net.sf.power.monitor.menu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import net.sf.power.monitor.R
import net.sf.power.monitor.VoidCallback
import net.sf.power.monitor.compose.AppTheme
import net.sf.power.monitor.compose.colorButton
import net.sf.power.monitor.compose.modifierButton
import net.sf.power.monitor.compose.modifierIcon
import net.sf.power.monitor.compose.sizeButton

@Composable
fun StopButton(size: Dp = sizeButton, onClick: VoidCallback) {
    IconButton(onClick = onClick, modifier = Modifier.modifierButton(size = size)) {
        Icon(
            imageVector = Icons.Default.StopCircle,
            contentDescription = stringResource(R.string.stop_monitor),
            tint = colorButton,
            modifier = Modifier.modifierIcon(size = size)
        )
    }
}

@Composable
@Preview
private fun Preview() {
    AppTheme {
        StopButton {}
    }
}
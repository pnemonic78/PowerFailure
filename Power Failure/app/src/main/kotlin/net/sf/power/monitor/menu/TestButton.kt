package net.sf.power.monitor.menu

import android.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.github.lang.VoidCallback
import net.sf.power.monitor.compose.AppTheme
import net.sf.power.monitor.compose.colorButton
import net.sf.power.monitor.compose.modifierButton
import net.sf.power.monitor.compose.modifierIcon
import net.sf.power.monitor.compose.sizeButton

@Composable
fun TestButton(size: Dp = sizeButton, onClick: VoidCallback) {
    IconButton(onClick = onClick, modifier = Modifier.modifierButton(size = size)) {
        Icon(
            imageVector = Icons.Default.Quiz,
            contentDescription = stringResource(R.string.dialog_alert_title),
            tint = colorButton,
            modifier = Modifier.modifierIcon(size = size)
        )
    }
}

@Composable
@Preview
private fun Preview() {
    AppTheme {
        TestButton {}
    }
}
package net.sf.power.monitor.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.sf.power.monitor.compose.AppTheme
import net.sf.power.monitor.compose.Left
import net.sf.power.monitor.compose.panelScrollable

private val spacing = 8.dp

@Composable
fun ActionsMenu(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier = modifier.panelScrollable(), horizontalArrangement = Arrangement.Left) {
        content()
    }
}

@Composable
fun ActionsMenuCollapsed(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    content: @Composable RowScope.(Dp) -> Unit
) {
    var expanded by remember { mutableStateOf(isExpanded) }

    ActionsMenu(modifier = modifier) {
        AnimatedVisibility(
            visible = expanded,
            // Combines sliding and expanding horizontally
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth } // Start from the right
            ) + expandHorizontally(
                expandFrom = Alignment.End // Expand from the end
            ) + fadeIn(),
            // Defines how it disappears
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth } // Exit to the right
            ) + shrinkHorizontally(
                shrinkTowards = Alignment.End // Shrink towards the end
            ) + fadeOut()
        ) {
            Row(horizontalArrangement = Arrangement.Left) {
                content(spacing)
                Spacer(modifier = Modifier.width(spacing))
            }
        }
        MenuButton(onClick = { expanded = !expanded })
    }
}

@Composable
@Preview
private fun Preview() {
    AppTheme {
        ActionsMenuCollapsed(isExpanded = true) {
            SettingsButton(onClick = {})
        }
    }
}
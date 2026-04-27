package net.sf.power.monitor.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import net.sf.power.monitor.model.Plugged

@Composable
fun PlugBackground(plugged: Plugged) {
    when (plugged) {
        Plugged.AC -> PlugBackgroundPlugged()
        Plugged.Dock -> PlugBackgroundPlugged()
        Plugged.None -> PlugBackgroundUnplugged()
        Plugged.USB -> PlugBackgroundPlugged()
        Plugged.Unknown -> PlugBackgroundUnknown()
        Plugged.Wireless -> PlugBackgroundPlugged()
    }
}

private val radiusSize = 350.dp

@Composable
@Suppress("AssignedValueIsNeverRead")
private fun PlugBackgroundUnknown() {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val gradientBrush = Brush.radialGradient(
        colors = listOf(White, Gray),
        center = Offset(size.width * 0.5f, size.height * 0.3f),
        radius = radiusSize.toPx()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .background(gradientBrush)
    )
}

@Composable
@Suppress("AssignedValueIsNeverRead")
private fun PlugBackgroundUnplugged() {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val gradientBrush = Brush.radialGradient(
        colors = listOf(White, Color(0xffa00000)),
        center = Offset(size.width * 0.5f, size.height * 0.3f),
        radius = radiusSize.toPx()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .background(gradientBrush)
    )
}

@Composable
@Suppress("AssignedValueIsNeverRead")
private fun PlugBackgroundPlugged() {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val gradientBrush = Brush.radialGradient(
        colors = listOf(White, Color(0xff00a000)),
        center = Offset(size.width * 0.5f, size.height * 0.3f),
        radius = radiusSize.toPx()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .background(gradientBrush)
    )
}

@Composable
@Preview
private fun PreviewUnknown() {
    AppTheme {
        PlugBackground(Plugged.Unknown)
    }
}

@Composable
@Preview
private fun PreviewUnplugged() {
    AppTheme {
        PlugBackground(Plugged.None)
    }
}

@Composable
@Preview
private fun PreviewPlugged() {
    AppTheme {
        PlugBackground(Plugged.USB)
    }
}
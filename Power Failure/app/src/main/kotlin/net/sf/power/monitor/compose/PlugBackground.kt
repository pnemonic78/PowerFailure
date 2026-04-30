package net.sf.power.monitor.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.sf.power.monitor.model.Plugged

@Composable
fun PlugBackground(
    plugged: Plugged,
    imagePosition: Rect = Rect.Zero
) {
    when (plugged) {
        Plugged.AC -> PlugBackgroundPlugged(imagePosition)
        Plugged.Dock -> PlugBackgroundPlugged(imagePosition)
        Plugged.None -> PlugBackgroundUnplugged(imagePosition)
        Plugged.USB -> PlugBackgroundPlugged(imagePosition)
        Plugged.Unknown -> PlugBackgroundUnknown(imagePosition)
        Plugged.Wireless -> PlugBackgroundPlugged(imagePosition)
    }
}

private val radiusSize = 350.dp

@Composable
private fun PlugBackgroundUnknown(imagePosition: Rect) {
    val center = imagePosition.center
    val gradientBrush = Brush.radialGradient(
        colors = listOf(White, Gray),
        center = center,
        radius = radiusSize.toPx()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    )
}

@Composable
private fun PlugBackgroundUnplugged(imagePosition: Rect) {
    val center = imagePosition.center
    val gradientBrush = Brush.radialGradient(
        colors = listOf(White, Color(0xffa00000)),
        center = center,
        radius = radiusSize.toPx()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    )
}

@Composable
private fun PlugBackgroundPlugged(imagePosition: Rect) {
    val center = imagePosition.center
    val gradientBrush = Brush.radialGradient(
        colors = listOf(White, Color(0xff00a000)),
        center = center,
        radius = radiusSize.toPx()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    )
}

@Composable
@Preview(widthDp = previewWidthDp, heightDp = previewHeightDp)
private fun PreviewUnknown() {
    val previewWidthPx = previewWidthDp.dp.toPx()
    val previewHeightPx = previewHeightDp.dp.toPx()
    val pos = Rect(0f, previewHeightPx * 0.5f, previewWidthPx, previewHeightPx)

    AppTheme {
        PlugBackground(Plugged.Unknown, pos)
    }
}

@Composable
@Preview(widthDp = previewWidthDp, heightDp = previewHeightDp)
private fun PreviewUnplugged() {
    val previewWidthPx = previewWidthDp.dp.toPx()
    val previewHeightPx = previewHeightDp.dp.toPx()
    val pos = Rect(0f, previewHeightPx * 0.5f, previewWidthPx, previewHeightPx)

    AppTheme {
        PlugBackground(Plugged.None, pos)
    }
}

@Composable
@Preview(widthDp = previewWidthDp, heightDp = previewHeightDp)
private fun PreviewPlugged() {
    val previewWidthPx = previewWidthDp.dp.toPx()
    val previewHeightPx = previewHeightDp.dp.toPx()
    val pos = Rect(0f, previewHeightPx * 0.5f, previewWidthPx, previewHeightPx)

    AppTheme {
        PlugBackground(Plugged.USB, pos)
    }
}
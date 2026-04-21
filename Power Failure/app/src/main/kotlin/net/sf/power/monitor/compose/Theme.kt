package net.sf.power.monitor.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.modifierButton(size: Dp = sizeButton): Modifier = this.size(size)
    .padding(paddingButton)

fun Modifier.modifierIcon(size: Dp = sizeButton): Modifier = this.size(size)

fun Modifier.panel(): Modifier =
    this.background(color = colorButtonBackground, shape = shapePanel)
        .padding(paddingPanel)
        .padding(horizontal = 4.dp)

@Composable
fun Modifier.panelScrollable(): Modifier = panel().horizontalScroll(rememberScrollState())

typealias ComposableContent = @Composable (() -> Unit)

@Composable
fun AppTheme(content: ComposableContent) {
    MaterialTheme(
        content = content
    )
}

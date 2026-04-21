package net.sf.power.monitor.compose

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize

typealias OnSizeCallback = (IntSize) -> Unit
typealias OnTapCallback = (Offset) -> Unit

@ReadOnlyComposable
@Composable
fun Dp.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }

@ReadOnlyComposable
@Composable
fun Float.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }

@ReadOnlyComposable
@Composable
fun IntSize.toDp(): DpSize = with(LocalDensity.current) { this@toDp.toSize().toDpSize() }

fun IntSize.toOffset(): Offset = Offset(width.toFloat(), height.toFloat())

val ImageBitmap.orientation: Orientation
    get() = if (height >= width) Orientation.Vertical else Orientation.Horizontal

fun IntSize.toOrientation(): Orientation {
    return if (height >= width) Orientation.Vertical else Orientation.Horizontal
}

fun IntSize.rotate(): IntSize = IntSize(width = height, height = width)

operator fun IntSize.times(other: Float): Size = Size(width * other, height * other)

fun DpSize.rotate(): DpSize = DpSize(width = height, height = width)

fun IntSize.isZero(): Boolean = (width == 0) && (height == 0)

fun HapticFeedback.toggleOn() = performHapticFeedback(HapticFeedbackType.ToggleOn)
fun HapticFeedback.toggleOff() = performHapticFeedback(HapticFeedbackType.ToggleOff)
fun HapticFeedback.toggle(checked: Boolean) = if (checked) toggleOn() else toggleOff()
fun HapticFeedback.click() = performHapticFeedback(HapticFeedbackType.ContextClick)

private var arrangementLeft: Arrangement.Horizontal? = null
val Arrangement.Left: Arrangement.Horizontal
    get() {
        if (arrangementLeft != null) {
            return arrangementLeft!!
        }
        arrangementLeft = object : Arrangement.Horizontal {
            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray,
            ) = placeLeftOrTop(sizes, outPositions)

            override fun toString() = "Arrangement#Left"

            private fun placeLeftOrTop(size: IntArray, outPosition: IntArray) {
                var current = 0
                size.forEachIndexed { index, item ->
                    outPosition[index] = current
                    current += item
                }
            }
        }
        return arrangementLeft!!
    }
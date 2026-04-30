package net.sf.power.monitor.compose

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.sf.power.monitor.R
import net.sf.power.monitor.model.Plugged

@Composable
fun PlugImage(plugged: Plugged, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        when (plugged) {
            Plugged.AC -> PluggedAC()
            Plugged.Dock -> PluggedDock()
            Plugged.None -> PluggedNone()
            Plugged.USB -> PluggedUSB()
            Plugged.Unknown -> PluggedUnknown()
            Plugged.Wireless -> PluggedWireless()
        }
    }
}

private val borderWidth = 3.dp
private val borderShape = CircleShape
private val sizeImageMin = 100.dp
private val sizeImageMax = 250.dp

@Composable
private fun BoxScope.PluggedImageAndBorder(
    @DrawableRes imageId: Int,
    @StringRes descriptionId: Int,
    @ColorRes ringColor: Int
) {
    val painter = rememberVectorPainter(ImageVector.vectorResource(imageId))
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .widthIn(min = sizeImageMin, max = sizeImageMax)
            .heightIn(min = sizeImageMin, max = sizeImageMax)
            .aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(borderWidth, colorResource(ringColor), borderShape)
        )
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painter,
            contentDescription = stringResource(descriptionId),
        )
    }
}

@Composable
private fun BoxScope.PluggedAC() {
    PluggedImageAndBorder(R.drawable.plug_ac, R.string.plugged_ac, R.color.plugged_border)
}

@Composable
private fun BoxScope.PluggedDock() {
    PluggedImageAndBorder(R.drawable.plug_dock, R.string.plugged_dock, R.color.plugged_border)
}

@Composable
private fun BoxScope.PluggedNone() {
    PluggedImageAndBorder(
        R.drawable.plug_unplugged,
        R.string.plugged_unplugged,
        R.color.unplugged_border
    )
}

@Composable
private fun BoxScope.PluggedUSB() {
    PluggedImageAndBorder(R.drawable.plug_usb, R.string.plugged_usb, R.color.plugged_border)
}

@Composable
private fun BoxScope.PluggedUnknown() {
    PluggedImageAndBorder(R.drawable.plug_unknown, R.string.plugged_unknown, R.color.unknown_border)
}

@Composable
private fun BoxScope.PluggedWireless() {
    PluggedImageAndBorder(
        R.drawable.plug_wireless,
        R.string.plugged_wireless,
        R.color.plugged_border
    )
}

@Composable
@Preview(showBackground = true, widthDp = previewWidthDp, heightDp = previewHeightDp)
private fun Preview_AC() {
    AppTheme {
        PlugImage(Plugged.AC)
    }
}

@Composable
@Preview(showBackground = true, widthDp = previewWidthDp, heightDp = previewHeightDp)
private fun Preview_Dock() {
    AppTheme {
        PlugImage(Plugged.Dock)
    }
}

@Composable
@Preview(showBackground = true, widthDp = previewWidthDp, heightDp = previewHeightDp)
private fun Preview_None() {
    AppTheme {
        PlugImage(Plugged.None)
    }
}

@Composable
@Preview(showBackground = true, widthDp = previewWidthDp, heightDp = previewHeightDp)
private fun Preview_Unknown() {
    AppTheme {
        PlugImage(Plugged.Unknown)
    }
}

@Composable
@Preview(showBackground = true, widthDp = previewWidthDp, heightDp = previewHeightDp)
private fun Preview_USB() {
    AppTheme {
        PlugImage(Plugged.USB)
    }
}

@Composable
@Preview(showBackground = true, widthDp = previewWidthDp, heightDp = previewHeightDp)
private fun Preview_Wireless() {
    AppTheme {
        PlugImage(Plugged.Wireless)
    }
}

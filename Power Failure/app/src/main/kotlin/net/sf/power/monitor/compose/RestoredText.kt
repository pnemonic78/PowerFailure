package net.sf.power.monitor.compose

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.sf.power.monitor.R
import net.sf.power.monitor.TimeMillis
import net.sf.power.monitor.preference.PowerPreferences

@Composable
fun RestoredText(timeMillis: TimeMillis, modifier: Modifier = Modifier) {
    if (timeMillis <= PowerPreferences.NEVER) return
    val context = LocalContext.current
    val dateTime = DateUtils.formatDateTime(
        context,
        timeMillis,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
    )
    val text = stringResource(R.string.power_restored_on, dateTime)
    val shape = MaterialTheme.shapes.medium
    Text(
        modifier = modifier
            .background(DarkGreen, shape = shape)
            .border(1.dp, Green, shape = shape)
            .padding(16.dp),
        text = text,
        fontSize = 22.sp,
        color = White
    )
}

@Composable
@Preview(showBackground = true)
private fun Preview() {
    AppTheme {
        RestoredText(System.currentTimeMillis())
    }
}
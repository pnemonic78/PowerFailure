package net.sf.power.monitor.notify

import android.content.Context
import android.os.Vibrator
import android.text.format.DateUtils

class NotifyVibrate(private val context: Context) {
    private var isVibrating: Boolean = false

    /**
     * Vibrate the device.
     *
     * @param vibrate `true` to start vibrating - `false` to stop.
     */
    fun vibrate(vibrate: Boolean) {
        val context: Context = context
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrate) {
            if (!isVibrating && vibrator.hasVibrator()) {
                isVibrating = true
                vibrator.vibrate(VIBRATE_PATTERN, 0)
            }
        } else if (isVibrating) {
            isVibrating = false
            vibrator.cancel()
        }
    }

    companion object {

        private val VIBRATE_PATTERN =
            longArrayOf(DateUtils.SECOND_IN_MILLIS, DateUtils.SECOND_IN_MILLIS)

    }
}
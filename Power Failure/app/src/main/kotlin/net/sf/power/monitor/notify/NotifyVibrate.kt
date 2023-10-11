package net.sf.power.monitor.notify

import android.content.Context
import android.text.format.DateUtils
import com.github.os.VibratorCompat

class NotifyVibrate(context: Context) {

    private val vibrator = VibratorCompat(context)
    private var isVibrating: Boolean = false

    /**
     * Vibrate the device.
     *
     * @param vibrate `true` to start vibrating - `false` to stop.
     */
    fun vibrate(vibrate: Boolean) {
        if (vibrate) {
            if (!isVibrating) {
                isVibrating = true
                vibrator.vibrate(VIBRATE_PATTERN, VibratorCompat.USAGE_ALARM)
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
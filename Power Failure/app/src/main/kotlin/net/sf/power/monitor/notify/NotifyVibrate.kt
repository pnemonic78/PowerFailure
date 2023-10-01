package net.sf.power.monitor.notify

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

class NotifyVibrate(private val context: Context) {
    private var isVibrating: Boolean = false

    /**
     * Vibrate the device.
     *
     * @param vibrate `true` to start vibrating - `false` to stop.
     */
    fun vibrate(vibrate: Boolean) {
        val context: Context = context
        val vibrator = getVibrator(context)
        if (vibrate) {
            if (!isVibrating && vibrator.hasVibrator()) {
                isVibrating = true
                vibrate(vibrator)
            }
        } else if (isVibrating) {
            isVibrating = false
            vibrator.cancel()
        }
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(VibratorManager::class.java)
            manager.defaultVibrator
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(Vibrator::class.java)
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrate33(vibrator)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrate26(vibrator)
        } else {
            vibrateLegacy(vibrator)
        }
    }

    @Suppress("DEPRECATION")
    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrateLegacy(vibrator: Vibrator) {
        vibrator.vibrate(VIBRATE_PATTERN, 0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate26(vibrator: Vibrator) {
        val vibe: VibrationEffect = VibrationEffect.createWaveform(VIBRATE_PATTERN, 0)
        vibrator.vibrate(vibe)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate33(vibrator: Vibrator) {
        val vibe: VibrationEffect = VibrationEffect.createWaveform(VIBRATE_PATTERN, 0)
        val attributes: VibrationAttributes =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
        vibrator.vibrate(vibe, attributes)
    }

    companion object {
        private val VIBRATE_PATTERN =
            longArrayOf(DateUtils.SECOND_IN_MILLIS, DateUtils.SECOND_IN_MILLIS)
    }
}
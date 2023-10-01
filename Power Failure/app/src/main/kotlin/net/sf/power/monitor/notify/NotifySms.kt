package net.sf.power.monitor.notify

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.text.format.DateUtils
import net.sf.power.monitor.R
import timber.log.Timber

class NotifySms(private val context: Context) {
    fun send(timeInMillis: Long, destination: String) {
        if (destination.isEmpty()) return
        val context: Context = context

        val dateTime = DateUtils.formatDateTime(
            context,
            timeInMillis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
        )
        val text = context.getString(R.string.sms_message, dateTime)

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        } ?: return

        Timber.i("send SMS to $destination")
        smsManager.sendTextMessage(destination, null, text, null, null)
    }
}
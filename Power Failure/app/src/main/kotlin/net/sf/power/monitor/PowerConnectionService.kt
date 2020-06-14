/*
 * Copyright 2016, Moshe Waisberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.power.monitor

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.telephony.SmsManager
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import net.sf.power.monitor.preference.PowerPreferences
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

/**
 * Power connection events service.
 *
 * @author Moshe Waisberg
 */
class PowerConnectionService : Service(), BatteryListener {

    companion object {

        /**
         * Command to the service to register a client, receiving callbacks
         * from the service.  The Message's replyTo field must be a Messenger of
         * the client where callbacks should be sent.
         */
        const val MSG_REGISTER_CLIENT = 1

        /**
         * Command to the service to unregister a client, ot stop receiving callbacks
         * from the service.  The Message's replyTo field must be a Messenger of
         * the client as previously given with MSG_REGISTER_CLIENT.
         */
        const val MSG_UNREGISTER_CLIENT = 2

        /**
         * Command to the service to start monitoring.
         */
        const val MSG_START_MONITOR = 3

        /**
         * Command to the service to stop monitoring.
         */
        const val MSG_STOP_MONITOR = 4

        /**
         * Command to the service to query the monitoring status.
         */
        const val MSG_GET_STATUS_MONITOR = 5

        /**
         * Command to the clients about the monitoring status.
         */
        const val MSG_SET_STATUS_MONITOR = 6

        /**
         * Command to check the battery status.
         */
        const val MSG_CHECK_BATTERY = 10

        /**
         * Command to the clients that the battery status has been changed.
         */
        const val MSG_BATTERY_CHANGED = 11

        /**
         * Command to the clients that the alarm has activated.
         */
        const val MSG_ALARM = 12

        /**
         * Command to the service that the shared preferences have changed.
         */
        const val MSG_PREFERENCES_CHANGED = 20

        private const val POLL_RATE = DateUtils.SECOND_IN_MILLIS
        private const val ID_NOTIFY = R.string.start_monitor

        private val VIBRATE_PATTERN = longArrayOf(DateUtils.SECOND_IN_MILLIS, DateUtils.SECOND_IN_MILLIS)

        private const val CHANNEL_ID = "power"
    }

    private lateinit var context: Context
    private lateinit var handler: Handler

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private lateinit var messenger: Messenger

    /**
     * Keeps track of all current registered clients.
     */
    private val clients = ArrayList<Messenger>()
    private lateinit var notificationManager: NotificationManager
    private var notificationTextId: Int = 0
    private var notificationIconId: Int = 0
    private var ringtone: Ringtone? = null
    private var powerSince: Long = 0L
    private var powerFailureSince: Long = 0L
    private var logging: Boolean = false
    private lateinit var settings: PowerPreferences
    private var vibrating: Boolean = false
    private var prefTimeDelay: Long = 0
    private var prefRingtone: Uri? = null
    private var prefVibrate: Boolean = false
    private var prefSmsRecipient: String = ""

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (PowerPreferences.ACTION_PREFERENCES_CHANGED == action) {
                Message.obtain(handler, MSG_PREFERENCES_CHANGED).sendToTarget()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return messenger.binder
    }

    override fun onCreate() {
        super.onCreate()
        context = this

        handler = PowerConnectionHandler(this)
        messenger = Messenger(handler)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        settings = PowerPreferences(this)

        val filter = IntentFilter()
        filter.addAction(PowerPreferences.ACTION_PREFERENCES_CHANGED)
        registerReceiver(receiver, filter)
        onPreferencesChanged()

        stopAlarm()
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification(R.string.monitor_stopped, R.mipmap.ic_launcher)
        checkBatteryStatus()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopLogging()
        stopPolling()
        stopAlarm()
        hideNotification()
        unregisterReceiver(receiver)
    }

    private fun startPolling() {
        Timber.v("start polling")
        printBatteryStatus(context)
        if (!handler.hasMessages(MSG_CHECK_BATTERY)) {
            pollBattery()
        }
    }

    private fun stopPolling() {
        Timber.v("stop polling")
        handler.removeMessages(MSG_CHECK_BATTERY)
        if (clients.isEmpty()) {
            stopSelf()
        }
    }

    private fun checkBatteryStatus() {
        printBatteryStatus(context)

        val plugged = BatteryUtils.getPlugged(context)
        val msg = handler.obtainMessage(MSG_BATTERY_CHANGED, plugged, 0)
        msg.sendToTarget()

        pollBattery()
    }

    private fun pollBattery() {
        handler.sendEmptyMessageDelayed(MSG_CHECK_BATTERY, POLL_RATE)
    }

    private fun printBatteryStatus(context: Context) {
        BatteryUtils.printStatus(context)
    }

    private class PowerConnectionHandler(service: PowerConnectionService) : Handler() {

        private val service: WeakReference<PowerConnectionService> = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = this.service.get() ?: return
            val client = msg.replyTo

            when (msg.what) {
                MSG_REGISTER_CLIENT -> service.registerClient(client)
                MSG_UNREGISTER_CLIENT -> service.unregisterClient(client)
                MSG_START_MONITOR -> service.startLogging()
                MSG_STOP_MONITOR -> service.stopLogging()
                MSG_GET_STATUS_MONITOR -> service.notifyClients(MSG_SET_STATUS_MONITOR, if (service.logging) 1 else 0, 0)
                MSG_CHECK_BATTERY -> service.checkBatteryStatus()
                MSG_BATTERY_CHANGED -> service.onBatteryPlugged(msg.arg1)
                MSG_PREFERENCES_CHANGED -> service.onPreferencesChanged()
                else -> super.handleMessage(msg)
            }
        }
    }

    override fun onBatteryPlugged(plugged: Int) {
        val now = SystemClock.uptimeMillis()

        if (plugged != BatteryListener.BATTERY_PLUGGED_NONE) {
            powerSince = now
            powerFailureSince = 0L
            stopAlarm()
        } else if (logging && (now >= powerSince + prefTimeDelay)) {
            if (powerFailureSince <= 0L) {
                powerFailureSince = now
                handleFailure(plugged, System.currentTimeMillis())
            }
        } else {
            stopAlarm()
        }

        when (plugged) {
            BatteryListener.BATTERY_PLUGGED_NONE -> showNotification(R.string.plugged_unplugged, R.drawable.stat_plug_disconnect)
            BatteryListener.BATTERY_PLUGGED_AC -> showNotification(R.string.plugged_ac, R.drawable.stat_plug_ac)
            BatteryListener.BATTERY_PLUGGED_USB -> showNotification(R.string.plugged_usb, R.drawable.stat_plug_usb)
            BatteryListener.BATTERY_PLUGGED_WIRELESS -> showNotification(R.string.plugged_wireless, R.drawable.stat_plug_wireless)
            else -> showNotification(R.string.plugged_unknown, R.drawable.stat_plug_ac)
        }

        notifyClients(MSG_BATTERY_CHANGED, plugged, 0)
    }

    private fun getRingtone(context: Context): Ringtone? {
        if (prefRingtone == null) {
            stopTone()
            prefRingtone = settings.ringtone
            this.ringtone = null
        }
        if ((ringtone == null) && (prefRingtone != null)) {
            this.ringtone = RingtoneManager.getRingtone(context, prefRingtone)
        }
        return ringtone
    }

    private fun playAlarm() {
        Timber.v("play alarm")
        playTone(context)
        vibrate(context, prefVibrate)
    }

    private fun playTone(context: Context) {
        val ringtone = getRingtone(context)
        try {
            Timber.v("play tone: %s", ringtone?.getTitle(context) ?: "(none)")
        } catch (e: Exception) {
            Timber.v("play tone: %s", ringtone)
        }
        if (ringtone != null && !ringtone.isPlaying) {
            ringtone.play()
        }
    }

    private fun stopAlarm() {
        Timber.v("stop alarm")
        stopTone()
        vibrate(context, false)
    }

    private fun stopTone() {
        Timber.v("stop tone")
        val ringtone = this.ringtone
        if (ringtone != null && ringtone.isPlaying) {
            ringtone.stop()
        }
    }

    /**
     * Show a notification while this service is running.
     *
     * @param textId      the text resource id.
     * @param largeIconId the large icon resource id.
     */
    private fun showNotification(textId: Int, largeIconId: Int) {
        // Are we already showing the notification?
        // Updating with same notification info causes flashing.
        if (notificationTextId == textId && notificationIconId == largeIconId) {
            return
        }
        this.notificationTextId = textId
        this.notificationIconId = largeIconId

        val res = context.resources

        val title = res.getText(R.string.title_service)
        val text = res.getText(textId)

        // The PendingIntent to launch our activity if the user selects this notification.
        val contentIntent = createActivityIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel: android.app.NotificationChannel? = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel == null) {
                channel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    getText(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Set the info for the views that show in the notification panel.
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setLargeIcon(BitmapFactory.decodeResource(res, largeIconId))
            .setSmallIcon(R.drawable.stat_launcher)  // the status icon
            .setTicker(text)  // the status text
            .setWhen(System.currentTimeMillis())  // the time stamp
            .setContentTitle(title)  // the label of the entry
            .setContentText(text)  // the contents of the entry
            .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
            .build()

        // Send the notification.
        startForeground(ID_NOTIFY, notification)
    }

    private fun createActivityIntent(context: Context): PendingIntent {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, ID_NOTIFY, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Cancel the persistent notification.
     */
    private fun hideNotification() {
        notificationManager.cancel(ID_NOTIFY)
        this.notificationTextId = 0
        this.notificationIconId = 0
    }

    private fun registerClient(client: Messenger) {
        if (!clients.contains(client)) {
            clients.add(client)
        }
        notifyClients(MSG_SET_STATUS_MONITOR, if (logging) 1 else 0, 0)
        startPolling()
    }

    private fun unregisterClient(client: Messenger) {
        clients.remove(client)
        if (clients.isEmpty() && !logging) {
            stopSelf()
        }
    }

    private fun notifyClients(command: Int, arg1: Int, arg2: Int) {
        var msg: Message
        for (i in clients.indices.reversed()) {
            try {
                msg = Message.obtain(null, command, arg1, arg2)
                clients[i].send(msg)
            } catch (e: RemoteException) {
                Timber.e(e, "Failed to send status update")
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                clients.removeAt(i)
            }
        }
    }

    private fun startLogging() {
        Timber.v("start logging")
        if (!logging) {
            logging = true
            showNotification(R.string.monitor_started, R.mipmap.ic_launcher)
            //TODO Write logs.
        }
    }

    private fun stopLogging() {
        Timber.v("stop logging")
        if (logging) {
            logging = false
            showNotification(R.string.monitor_stopped, R.mipmap.ic_launcher)
            //TODO Write logs.
        }
    }

    /**
     * Vibrate the device.
     *
     * @param context the context.
     * @param vibrate `true` to start vibrating - `false` to stop.
     */
    private fun vibrate(context: Context, vibrate: Boolean) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrate) {
            if (!vibrating && vibrator.hasVibrator()) {
                vibrating = true
                vibrator.vibrate(VIBRATE_PATTERN, 0)
            }
        } else if (vibrating) {
            vibrating = false
            vibrator.cancel()
        }
    }

    private fun onPreferencesChanged() {
        prefTimeDelay = settings.failureDelay
        prefRingtone = null
        prefVibrate = settings.isVibrate
        prefSmsRecipient = settings.smsRecipient
    }

    private fun handleFailure(plugged: Int, millis: Long) {
        settings.failureTime = millis
        playAlarm()
        notifyClients(MSG_ALARM, plugged, (millis / DateUtils.SECOND_IN_MILLIS).toInt())
        sendSMS(millis)
    }

    private fun sendSMS(millis: Long) {
        val destination = prefSmsRecipient
        if (destination.isEmpty()) return

        val dateTime = DateUtils.formatDateTime(this, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
        val text = getString(R.string.sms_message, dateTime)

        val smsManager = SmsManager.getDefault() ?: return
        smsManager.sendTextMessage(destination, null, text, null, null)
    }
}

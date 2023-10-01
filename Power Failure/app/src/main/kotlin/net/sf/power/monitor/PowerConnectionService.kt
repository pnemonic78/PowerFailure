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

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.PowerManager
import android.os.RemoteException
import android.text.format.DateUtils
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import java.lang.ref.WeakReference
import net.sf.power.monitor.notify.NotifyAlarm
import net.sf.power.monitor.notify.NotifySms
import net.sf.power.monitor.notify.NotifyVibrate
import net.sf.power.monitor.preference.PowerPreferences
import timber.log.Timber

/**
 * Power connection events service.
 *
 * `adb shell dumpsys battery unplug`
 * `adb shell dumpsys battery reset`
 *
 * @author Moshe Waisberg
 */
class PowerConnectionService : Service(), BatteryListener {

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
    private var powerSince: Long = NEVER
    private var powerFailureSince: Long = NEVER
    private var isLogging: Boolean = false
    private lateinit var settings: PowerPreferences
    private var notifyAlarm: NotifyAlarm? = null
    private var notifySms: NotifySms? = null
    private var notifyVibrate: NotifyVibrate? = null
    private var prefTimeDelay: Long = 0
    private var prefRingtone: Uri? = null
    private var prefVibrate: Boolean = false
    private var prefSmsEnabled: Boolean = false
    private var prefSmsRecipient: String = ""
    private var wakeLock: PowerManager.WakeLock? = null

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
        notificationManager = getNotificationManager()
        settings = PowerPreferences(context)

        val filter = IntentFilter()
        filter.addAction(PowerPreferences.ACTION_PREFERENCES_CHANGED)
        registerReceiver(receiver, filter, null, handler)
        onPreferencesChanged()

        stopAlarm()
        // Display a notification about us starting. We put an icon in the status bar.
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

    private fun getNotificationManager(): NotificationManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService(NotificationManager::class.java)
        } else {
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
    }

    private fun startPolling() {
        Timber.v("start polling")
        val context: Context = context
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
        val context: Context = context
        printBatteryStatus(context)

        val plugged = BatteryUtils.getPlugged(context)
        Message.obtain(handler, MSG_BATTERY_CHANGED, plugged, 0).sendToTarget()

        pollBattery()
    }

    private fun pollBattery() {
        handler.sendEmptyMessageDelayed(MSG_CHECK_BATTERY, POLL_RATE)
    }

    private fun printBatteryStatus(context: Context) {
        BatteryUtils.printStatus(context)
    }

    private class PowerConnectionHandler(service: PowerConnectionService) :
        Handler(Looper.getMainLooper()) {

        private val service: WeakReference<PowerConnectionService> = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = this.service.get() ?: return
            val client = msg.replyTo

            when (msg.what) {
                MSG_REGISTER_CLIENT -> service.registerClient(client)
                MSG_UNREGISTER_CLIENT -> service.unregisterClient(client)
                MSG_START_MONITOR -> service.startLogging()
                MSG_STOP_MONITOR -> service.stopLogging()
                MSG_GET_STATUS_MONITOR -> service.notifyClients(
                    MSG_SET_STATUS_MONITOR,
                    if (service.isLogging) 1 else 0
                )

                MSG_CHECK_BATTERY -> service.checkBatteryStatus()
                MSG_BATTERY_CHANGED -> service.onBatteryPlugged(msg.arg1)
                MSG_PREFERENCES_CHANGED -> service.onPreferencesChanged()
                MSG_FAILED -> service.handleFailure(msg.arg1, msg.obj as Long)
                else -> super.handleMessage(msg)
            }
        }
    }

    override fun onBatteryPlugged(plugged: Int) {
        val now = System.currentTimeMillis()

        if (plugged != BatteryListener.BATTERY_PLUGGED_NONE) {
            powerSince = now
            if (isLogging && (powerFailureSince > NEVER)) {
                if (now >= powerFailureSince + prefTimeDelay) {
                    powerFailureSince = NEVER
                    handleRestore(plugged, now)
                }
            } else {
                powerFailureSince = NEVER
                stopAlarm()
            }
        } else if (isLogging && (now >= powerSince + prefTimeDelay)) {
            if (powerFailureSince <= NEVER) {
                powerFailureSince = now
                handleFailure(plugged, now)
            }
        } else {
            stopAlarm()
        }

        when (plugged) {
            BatteryListener.BATTERY_PLUGGED_NONE -> showNotification(
                R.string.plugged_unplugged,
                R.drawable.plug_unplugged
            )

            BatteryListener.BATTERY_PLUGGED_AC -> showNotification(
                R.string.plugged_ac,
                R.drawable.plug_ac
            )

            BatteryListener.BATTERY_PLUGGED_DOCK -> showNotification(
                R.string.plugged_dock,
                R.drawable.plug_dock
            )

            BatteryListener.BATTERY_PLUGGED_USB -> showNotification(
                R.string.plugged_usb,
                R.drawable.plug_usb
            )

            BatteryListener.BATTERY_PLUGGED_WIRELESS -> showNotification(
                R.string.plugged_wireless,
                R.drawable.plug_wireless
            )

            else -> showNotification(R.string.plugged_unknown, R.drawable.plug_unknown)
        }

        notifyClients(MSG_BATTERY_CHANGED, plugged)
    }

    private fun playAlarm() {
        Timber.v("play alarm")
        val context: Context = context
        playSound(context)
        vibrate(context, prefVibrate)
    }

    private fun playSound(context: Context) {
        notifyAlarm = notifyAlarm ?: NotifyAlarm(context)
        var ringtoneUri = prefRingtone
        // Has the tone uri changed?
        if (ringtoneUri == null) {
            notifyAlarm?.stop()
            prefRingtone = settings.ringtone
            ringtoneUri = prefRingtone
        }
        if (ringtoneUri != null) {
            notifyAlarm?.play(ringtoneUri)
        }
    }

    private fun stopAlarm() {
        Timber.v("stop alarm")
        val context: Context = context
        stopSound()
        vibrate(context, false)
    }

    private fun stopSound() {
        notifyAlarm?.stop()
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

        val context: Context = context
        val res = context.resources

        val title = res.getText(R.string.title_service)
        val text = res.getText(textId)

        // The PendingIntent to launch our activity if the user selects this notification.
        val contentIntent = createActivityIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel == null) {
                channel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    res.getText(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Set the info for the views that show in the notification panel.
        val largeIcon = AppCompatResources.getDrawable(context, largeIconId)?.toBitmap()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setOngoing(true)
            .setSilent(true)
            .setLargeIcon(largeIcon)
            .setSmallIcon(R.drawable.ic_launcher_mono)  // the status icon
            .setTicker(text)  // the status text
            .setWhen(System.currentTimeMillis())  // the time stamp
            .setContentTitle(title)  // the label of the entry
            .setContentText(text)  // the contents of the entry
            .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
            .setLights(Color.RED, secondMs, secondMs)
            .build()

        // Send the notification.
        startForeground(ID_NOTIFY, notification)
    }

    private fun createActivityIntent(context: Context): PendingIntent? {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            ID_NOTIFY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
        notifyClients(MSG_SET_STATUS_MONITOR, if (isLogging) 1 else 0)
        startPolling()
    }

    private fun unregisterClient(client: Messenger) {
        clients.remove(client)
        if (clients.isEmpty() && !isLogging) {
            stopSelf()
        }
    }

    private fun notifyClients(command: Int, arg1: Int, arg2: Int = 0, arg3: Any? = null) {
        var msg: Message
        for (i in clients.indices.reversed()) {
            try {
                msg = Message.obtain(null, command, arg1, arg2, arg3)
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
        if (!isLogging) {
            isLogging = true
            showNotification(R.string.monitor_started, R.mipmap.ic_launcher)
            acquireWakeLock()
            //TODO Write logs.
        }
    }

    private fun stopLogging() {
        Timber.v("stop logging")
        if (isLogging) {
            isLogging = false
            showNotification(R.string.monitor_stopped, R.mipmap.ic_launcher)
            //TODO Write logs.
        }
        releaseWakeLock()
    }

    /**
     * Vibrate the device.
     *
     * @param context the context.
     * @param vibrate `true` to start vibrating - `false` to stop.
     */
    private fun vibrate(context: Context, vibrate: Boolean) {
        notifyVibrate = notifyVibrate ?: NotifyVibrate(context)
        notifyVibrate?.vibrate(vibrate)
    }

    private fun onPreferencesChanged() {
        prefTimeDelay = settings.failureDelay
        prefRingtone = null
        prefVibrate = settings.isVibrate
        prefSmsEnabled = settings.isSmsEnabled
        prefSmsRecipient = settings.smsRecipient
    }

    private fun handleFailure(plugged: Int, timeMillis: Long) {
        settings.failureTime = timeMillis
        settings.restoredTime = PowerPreferences.NEVER
        notifyClients(MSG_FAILED, plugged, 0, timeMillis)
        playAlarm()
        sendSMS(timeMillis)
    }

    private fun handleRestore(plugged: Int, timeMillis: Long) {
        settings.restoredTime = timeMillis
        notifyClients(MSG_RESTORED, plugged, 0, timeMillis)
        stopAlarm()
    }

    private fun sendSMS(timeMillis: Long) {
        if (!prefSmsEnabled) return
        val context: Context = context
        notifySms = notifySms ?: NotifySms(context)
        notifySms?.send(timeMillis, prefSmsRecipient)
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        var wakeLock = this.wakeLock
        if (wakeLock == null) {
            val powerManager = getPowerManager()
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_POWER)
            wakeLock.acquire()
            this.wakeLock = wakeLock
        } else {
            wakeLock.acquire()
        }
    }

    private fun getPowerManager(): PowerManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService(PowerManager::class.java)
        } else {
            getSystemService(Context.POWER_SERVICE) as PowerManager
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.release()
        wakeLock = null
    }

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
         * Command to the clients that the power failed.
         */
        const val MSG_FAILED = 12

        /**
         * Command to the clients that the power was restored.
         */
        const val MSG_RESTORED = 13

        /**
         * Command to the service that the shared preferences have changed.
         */
        const val MSG_PREFERENCES_CHANGED = 20

        private const val POLL_RATE = DateUtils.SECOND_IN_MILLIS
        private const val ID_NOTIFY = 1
        private const val CHANNEL_ID = "power-failure"
        private const val TAG_POWER = "power:lock"

        private const val secondMs = DateUtils.SECOND_IN_MILLIS.toInt()
        private const val NEVER = 0L
    }
}

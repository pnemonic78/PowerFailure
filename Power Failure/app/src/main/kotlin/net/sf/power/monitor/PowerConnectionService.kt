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
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.text.format.DateUtils
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.media.RingtoneManager
import kotlinx.coroutines.launch
import net.sf.power.monitor.model.BatteryListener
import net.sf.power.monitor.model.BatteryState
import net.sf.power.monitor.model.Plugged
import net.sf.power.monitor.notify.NotifyAlarm
import net.sf.power.monitor.notify.NotifySms
import net.sf.power.monitor.notify.NotifyVibrate
import net.sf.power.monitor.preference.PowerPreferences
import net.sf.power.monitor.preference.SimplePowerPreferences
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Power connection events service.
 *
 * `adb shell dumpsys battery unplug`
 * `adb shell dumpsys battery reset`
 *
 * @author Moshe Waisberg
 */
class PowerConnectionService : LifecycleService(), BatteryListener {

    private val messenger: LocalBroadcastManager by lazy {
        val context: Context = this@PowerConnectionService
        LocalBroadcastManager.getInstance(context)
    }

    /**
     * Keeps track of all current registered clients.
     */
    private val clients = CopyOnWriteArraySet<String>()
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(this@PowerConnectionService)
    }
    private val powerManager: PowerManager by lazy {
        getSystemService(PowerManager::class.java)
    }

    @StringRes
    private var notificationTextId: Int = 0

    @DrawableRes
    private var notificationIconId: Int = 0
    private var notificationChannel: NotificationChannelCompat? = null
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var isLogging: Boolean = false
    private val settings: PowerPreferences by lazy { SimplePowerPreferences(this) }
    private var notifyAlarm: NotifyAlarm? = null
    private var notifySms: NotifySms? = null
    private var notifyVibrate: NotifyVibrate? = null
    private var prefTimeDelay: TimeMillis = 0
    private var prefRingtone: Uri? = null
    private var prefVibrate: Boolean = false
    private var prefSmsEnabled: Boolean = false
    private var prefSmsRecipient: String = ""
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var poll: ChargerPoll

    private val receiver = object : BroadcastReceiver() {

        private val service: PowerConnectionService = this@PowerConnectionService
        private val target by lazy {
            val context: Context = this@PowerConnectionService
            ComponentName(context, PowerConnectionService::class.java)
        }

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.component != target) return
            Timber.d("onReceive $intent")
            val action = intent.action
            //val arg1 = intent.getIntExtra(EXTRA_ARG1, 0)
            val clientId = intent.getStringExtra(EXTRA_CLIENT)

            when (action) {
                ACTION_REGISTER_CLIENT -> service.registerClient(clientId!!)

                ACTION_UNREGISTER_CLIENT -> service.unregisterClient(clientId!!)

                ACTION_START_MONITOR -> {
                    service.registerClient(clientId!!)
                    service.startLogging()
                }

                ACTION_STOP_MONITOR -> {
                    service.stopLogging()
                    service.unregisterClient(clientId!!)
                }

                ACTION_GET_STATUS_MONITOR -> service.notifyClients(
                    ACTION_SET_STATUS_MONITOR,
                    if (service.isLogging) TRUE else FALSE
                )

                ACTION_PREFERENCES_CHANGED -> service.onPreferencesChanged()

                ACTION_FAILED -> service.poll.fail()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.i("Service create")

        hideNotification()
        // Display a notification about us starting. We put an icon in the status bar.
        showNotification(R.string.monitor_stopped, R.mipmap.ic_launcher)

        registerMessenger()
        stopAlarm()

        this.poll = (application as PowerMonitorApplication).poll
        lifecycleScope.launch {
            poll.state.collect {
                onBatteryState(it)
            }
        }
        lifecycleScope.launch {
            poll.failedTime.collect {
                handleFailure(it)
            }
        }
        lifecycleScope.launch {
            poll.restoredTime.collect {
                handleRestore(it)
            }
        }

        // Also starts polling.
        onPreferencesChanged()
    }

    override fun onDestroy() {
        Timber.i("Service destroy")
        stopLogging()
        stopPolling()
        stopAlarm()
        hideNotification()
        messenger.unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.v("onStartCommand $intent")
        if (intent != null) {
            receiver.onReceive(this, intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun registerMessenger() {
        val intentFilter = IntentFilter(Intent.ACTION_DEFAULT).apply {
            addAction(ACTION_FAILED)
            addAction(ACTION_GET_STATUS_MONITOR)
            addAction(ACTION_PREFERENCES_CHANGED)
            addAction(ACTION_REGISTER_CLIENT)
            addAction(ACTION_SET_STATUS_MONITOR)
            addAction(ACTION_START_MONITOR)
            addAction(ACTION_STOP_MONITOR)
            addAction(ACTION_UNREGISTER_CLIENT)
        }
        messenger.registerReceiver(receiver, intentFilter)
    }

    private fun startPolling() {
        Timber.v("start polling")
        poll.start(this, settings)
    }

    private fun stopPolling() {
        Timber.v("stop polling")
        poll.stop(this)
        if (clients.isEmpty()) {
            stopSelf()
        }
    }

    override fun onBatteryState(state: BatteryState) {
        val plugged = state.plugged

        when (plugged) {
            Plugged.None -> showNotification(
                R.string.plugged_unplugged,
                R.drawable.plug_unplugged
            )

            Plugged.AC -> showNotification(
                R.string.plugged_ac,
                R.drawable.plug_ac
            )

            Plugged.Dock -> showNotification(
                R.string.plugged_dock,
                R.drawable.plug_dock
            )

            Plugged.USB -> showNotification(
                R.string.plugged_usb,
                R.drawable.plug_usb
            )

            Plugged.Wireless -> showNotification(
                R.string.plugged_wireless,
                R.drawable.plug_wireless
            )

            else -> showNotification(R.string.plugged_unknown, R.drawable.plug_unknown)
        }
    }

    private fun playAlarm() {
        Timber.v("play alarm")
        val context: Context = this
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
            ringtoneUri = RingtoneManager.resolveUri(context, ringtoneUri)
            if (ringtoneUri != null) {
                notifyAlarm?.play(ringtoneUri)
            }
        }
    }

    private fun stopAlarm() {
        Timber.v("stop alarm")
        val context: Context = this
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
    private fun showNotification(@StringRes textId: Int, @DrawableRes largeIconId: Int) {
        // Are we already showing the notification?
        // Updating with same notification info causes flashing.
        if (notificationTextId == textId && notificationIconId == largeIconId) {
            return
        }
        this.notificationTextId = textId
        this.notificationIconId = largeIconId

        val context: Context = this

        val title = context.getText(R.string.title_service)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel: NotificationChannelCompat? =
                notificationChannel ?: notificationManager.getNotificationChannelCompat(CHANNEL_ID)
            if (channel == null) {
                channel = NotificationChannelCompat.Builder(
                    CHANNEL_ID,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT
                )
                    .setName(title)
                    .build()
                notificationManager.createNotificationChannel(channel)
                this.notificationChannel = channel
            }
        }

        // Set the info for the views that show in the notification panel.
        var builder = notificationBuilder
        if (builder == null) {
            // The PendingIntent to launch our activity if the user selects this notification.
            val contentIntent = createActivityIntent(context)

            builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setOngoing(true)
                .setSilent(true)
                .setSmallIcon(R.drawable.ic_launcher_mono)  // the status icon
                .setContentTitle(title)  // the label of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .setLights(Color.RED, SECOND_MS, SECOND_MS)
            this.notificationBuilder = builder
        }

        val largeIcon = AppCompatResources.getDrawable(context, largeIconId)?.toBitmap()
        val text = context.getText(textId)

        val notification = builder
            .setLargeIcon(largeIcon)
            .setTicker(text)  // the status text
            .setWhen(System.currentTimeMillis())  // the time stamp
            .setContentText(text)  // the contents of the entry
            .build()

        // Show with the notification.
        ServiceCompat.startForeground(
            this,
            ID_NOTIFY,
            notification,
            serviceType
        )
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
        notificationTextId = 0
        notificationIconId = 0
    }

    private fun registerClient(client: String) {
        Timber.v("register client $client isLogging=$isLogging")
        if (!clients.contains(client)) {
            clients.add(client)
        }
        notifyClient(ACTION_REGISTER_CLIENT, client, TRUE)
        notifyClients(ACTION_SET_STATUS_MONITOR, if (isLogging) TRUE else FALSE)
    }

    private fun unregisterClient(client: String) {
        Timber.v("unregister client $client")
        clients.remove(client)
        if (clients.isEmpty() && !isLogging) {
            stopSelf()
        }
    }

    private fun notifyClients(action: String, arg1: Int = 0, arg2: Int = 0) {
        for (client in clients) {
            val intent = Intent(action)
                .putExtra(EXTRA_ARG1, arg1)
                .putExtra(EXTRA_ARG2, arg2)
                .putExtra(EXTRA_CLIENT, client)
            messenger.sendBroadcastSync(intent)
        }
    }

    private fun notifyClient(action: String, client: String, arg1: Int = 0, arg2: Int = 0) {
        val intent = Intent(action)
            .putExtra(EXTRA_ARG1, arg1)
            .putExtra(EXTRA_ARG2, arg2)
            .putExtra(EXTRA_CLIENT, client)
        messenger.sendBroadcastSync(intent)
    }

    private fun startLogging() {
        Timber.v("start logging ($isLogging)")
        if (!isLogging) {
            isLogging = true
            showNotification(R.string.monitor_started, R.mipmap.ic_launcher)
            acquireWakeLock()
            //TODO Write logs.
            notifyClients(ACTION_SET_STATUS_MONITOR, TRUE)
        }
    }

    private fun stopLogging() {
        Timber.v("stop logging ($isLogging)")
        if (isLogging) {
            isLogging = false
            showNotification(R.string.monitor_stopped, R.mipmap.ic_launcher)
            //TODO Write logs.
            notifyClients(ACTION_SET_STATUS_MONITOR, FALSE)
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

        val alarm = notifyAlarm
        if (alarm != null) {
            val isPlaying = alarm.isPlaying
            stopSound()
            notifyAlarm = null
            if (isPlaying) {
                playSound(this)
            }
        }

        // Update the poller settings.
        startPolling()
    }

    private fun handleFailure(timeMillis: TimeMillis) {
        if (timeMillis <= NEVER) return
        if (!isLogging) return
        playAlarm()
        sendSMS(timeMillis)
    }

    private fun handleRestore(timeMillis: TimeMillis) {
        if (timeMillis <= NEVER) return
        if (!isLogging) return
        stopAlarm()
    }

    private fun sendSMS(timeMillis: TimeMillis) {
        if (!prefSmsEnabled) return
        val context: Context = this
        notifySms = notifySms ?: NotifySms(context)
        notifySms?.send(timeMillis, prefSmsRecipient)
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        var wakeLock = this.wakeLock
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_POWER)
            wakeLock.acquire()
            this.wakeLock = wakeLock
        } else {
            wakeLock.acquire()
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
        const val ACTION_REGISTER_CLIENT = "register_client"

        /**
         * Command to the service to unregister a client, ot stop receiving callbacks
         * from the service.  The Message's replyTo field must be a Messenger of
         * the client as previously given with `ACTION_REGISTER_CLIENT`.
         */
        const val ACTION_UNREGISTER_CLIENT = "unregister_client"

        /**
         * Command to the service to start monitoring.
         */
        const val ACTION_START_MONITOR = "start_monitor"

        /**
         * Command to the service to stop monitoring.
         */
        const val ACTION_STOP_MONITOR = "stop_monitor"

        /**
         * Command to the service to query the monitoring status.
         */
        const val ACTION_GET_STATUS_MONITOR = "get_status_monitor"

        /**
         * Command to the clients about the monitoring status.
         */
        const val ACTION_SET_STATUS_MONITOR = "set_status_monitor"

        /**
         * Command to the clients that the power failed.
         */
        const val ACTION_FAILED = "failed"

        const val ACTION_PREFERENCES_CHANGED = PowerPreferences.ACTION_PREFERENCES_CHANGED

        const val EXTRA_ARG1 = "arg1"
        const val EXTRA_ARG2 = "arg2"
        const val EXTRA_CLIENT = "client_id"

        private const val ID_NOTIFY = 1
        private const val CHANNEL_ID = "power-failure"
        private const val TAG_POWER = "power:lock"

        private const val SECOND_MS = DateUtils.SECOND_IN_MILLIS.toInt()

        private const val FALSE = 0
        private const val TRUE = 1

        private val serviceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
    }
}

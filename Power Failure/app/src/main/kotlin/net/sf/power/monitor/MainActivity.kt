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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Drawable
import android.os.*
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import net.sf.power.monitor.preference.PowerPreferenceActivity
import net.sf.power.monitor.preference.PowerPreferences
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Main activity.
 *
 * @author Moshe Waisberg
 */
class MainActivity : AppCompatActivity(), BatteryListener {

    companion object {
        private const val LEVEL_UNKNOWN = 0
        private const val LEVEL_UNPLUGGED = 1
        private const val LEVEL_PLUGGED_AC = 2
        private const val LEVEL_PLUGGED_USB = 3
        private const val LEVEL_PLUGGED_WIRELESS = 4
        private const val LEVEL_PLUGGED_UNKNOWN = LEVEL_PLUGGED_AC
    }

    private lateinit var mainBackground: Drawable
    private lateinit var pluggedView: ImageView
    private lateinit var timeView: TextView
    private var menuItemStart: MenuItem? = null
    private var menuItemStop: MenuItem? = null

    private lateinit var handler: Handler

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private lateinit var messenger: Messenger

    /**
     * Messenger for communicating with service.
     */
    private var service: Messenger? = null

    /**
     * Flag indicating whether we have called bind on the service.
     */
    private var serviceIsBound: Boolean = false

    private lateinit var settings: PowerPreferences

    /**
     * Class for interacting with the main interface of the service.
     */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            service = Messenger(binder)
            Timber.i("Service connected.")

            registerClient()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            service = null
            Timber.i("Service disconnected.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context: Context = this

        setContentView(R.layout.activity_main)
        val mainView = findViewById<View>(R.id.main)
        mainBackground = mainView.background
        mainBackground.level = LEVEL_UNKNOWN
        pluggedView = findViewById(R.id.plugged)
        pluggedView.setImageLevel(LEVEL_UNKNOWN)
        timeView = findViewById(R.id.time)

        handler = MainHandler(this)
        messenger = Messenger(handler)

        onBatteryPlugged(BatteryUtils.getPlugged(context))

        settings = PowerPreferences(context)
        val time = settings.failureTime
        if (time > 0L) {
            showFailureTime(time)
        }
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    override fun onStop() {
        super.onStop()
        unbindService()
    }

    private fun startMonitor() {
        // We want to monitor the service for as long as we are connected to it.
        try {
            notifyService(PowerConnectionService.MSG_START_MONITOR)
            setMonitorStatus(true)
        } catch (e: RemoteException) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
    }

    private fun stopMonitor() {
        try {
            notifyService(PowerConnectionService.MSG_STOP_MONITOR)
            setMonitorStatus(false)
        } catch (e: RemoteException) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
    }

    private fun setMonitorStatus(polling: Boolean) {
        mainBackground.level = LEVEL_UNKNOWN
        pluggedView.setImageLevel(LEVEL_UNKNOWN)
        if (menuItemStart != null) {
            menuItemStart!!.isVisible = !polling
            menuItemStop!!.isVisible = polling
        }
        onBatteryPlugged(BatteryUtils.getPlugged(this))
    }

    override fun onBatteryPlugged(plugged: Int) {
        when (plugged) {
            BatteryListener.BATTERY_PLUGGED_NONE -> {
                mainBackground.level = LEVEL_UNPLUGGED
                pluggedView.setImageLevel(LEVEL_UNPLUGGED)
                pluggedView.contentDescription = getText(R.string.plugged_unplugged)
            }
            BatteryListener.BATTERY_PLUGGED_AC -> {
                mainBackground.level = LEVEL_PLUGGED_AC
                pluggedView.setImageLevel(LEVEL_PLUGGED_AC)
                pluggedView.contentDescription = getText(R.string.plugged_ac)
            }
            BatteryListener.BATTERY_PLUGGED_USB -> {
                mainBackground.level = LEVEL_PLUGGED_USB
                pluggedView.setImageLevel(LEVEL_PLUGGED_USB)
                pluggedView.contentDescription = getText(R.string.plugged_usb)
            }
            BatteryListener.BATTERY_PLUGGED_WIRELESS -> {
                mainBackground.level = LEVEL_PLUGGED_WIRELESS
                pluggedView.setImageLevel(LEVEL_PLUGGED_WIRELESS)
                pluggedView.contentDescription = getText(R.string.plugged_wireless)
            }
            else -> {
                mainBackground.level = LEVEL_PLUGGED_UNKNOWN
                pluggedView.setImageLevel(LEVEL_PLUGGED_UNKNOWN)
                pluggedView.contentDescription = getText(R.string.plugged_unknown)
            }
        }
    }

    private class MainHandler(activity: MainActivity) : Handler() {

        companion object {
            internal const val MSG_STATUS_CHANGED = PowerConnectionService.MSG_BATTERY_CHANGED
            internal const val MSG_START_MONITOR = PowerConnectionService.MSG_START_MONITOR
            internal const val MSG_STOP_MONITOR = PowerConnectionService.MSG_STOP_MONITOR
            internal const val MSG_SET_STATUS_MONITOR = PowerConnectionService.MSG_SET_STATUS_MONITOR
            internal const val MSG_ALARM = PowerConnectionService.MSG_ALARM
            internal const val MSG_SETTINGS = 1000
        }

        private val activity: WeakReference<MainActivity> = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val activity = this.activity.get() ?: return

            when (msg.what) {
                MSG_STATUS_CHANGED -> activity.onBatteryPlugged(msg.arg1)
                MSG_START_MONITOR -> activity.startMonitor()
                MSG_STOP_MONITOR -> activity.stopMonitor()
                MSG_SET_STATUS_MONITOR -> activity.setMonitorStatus(msg.arg1 != 0)
                MSG_ALARM -> activity.showFailureTime(msg.obj as Long)
                MSG_SETTINGS -> activity.startActivity(Intent(activity, PowerPreferenceActivity::class.java))
                else -> super.handleMessage(msg)
            }
        }
    }

    private fun bindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        val context: Context = this
        val intent = Intent(context, PowerConnectionService::class.java)

        //This will keep service running even after activity destroyed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        serviceIsBound = true
        Timber.i("Service binding.")
    }

    private fun unbindService() {
        if (serviceIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            unregisterClient()

            // Detach our existing connection.
            unbindService(connection)
            serviceIsBound = false
            Timber.i("Service unbinding.")
        }
    }

    /**
     * Register this client with the service to receive commands.
     */
    private fun registerClient() {
        // We want to monitor the service for as long as we are connected to it.
        try {
            notifyService(PowerConnectionService.MSG_REGISTER_CLIENT)
            Timber.i("Registered with service.")
        } catch (e: RemoteException) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
    }

    /**
     * Unregister this client from the service to stop receiving commands.
     */
    private fun unregisterClient() {
        try {
            notifyService(PowerConnectionService.MSG_UNREGISTER_CLIENT)
            Timber.i("Unregistered from service.")
        } catch (e: RemoteException) {
            // There is nothing special we need to do if the service has crashed.
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val menuItemForce = menu.findItem(R.id.menu_force)
        if (BuildConfig.DEBUG) {
            menuItemForce.isEnabled = true
            menuItemForce.isVisible = true
        }
        menuItemStart = menu.findItem(R.id.menu_start)
        menuItemStop = menu.findItem(R.id.menu_stop)

        try {
            notifyService(PowerConnectionService.MSG_GET_STATUS_MONITOR)
        } catch (e: RemoteException) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_start -> {
                handler.sendEmptyMessage(MainHandler.MSG_START_MONITOR)
                return true
            }
            R.id.menu_stop -> {
                handler.sendEmptyMessage(MainHandler.MSG_STOP_MONITOR)
                return true
            }
            R.id.menu_settings -> {
                handler.sendEmptyMessage(MainHandler.MSG_SETTINGS)
                return true
            }
            R.id.menu_force -> {
                notifyService(MainHandler.MSG_ALARM, 0, 0, System.currentTimeMillis())
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    @Throws(RemoteException::class)
    private fun notifyService(command: Int, arg1: Int = 0, arg2: Int = 0, arg3: Any? = null) {
        val service = this.service ?: return
        if (serviceIsBound) {
            val msg = Message.obtain(null, command, arg1, arg2, arg3)
            msg.replyTo = messenger
            service.send(msg)
        }
    }

    private fun showFailureTime(millis: Long) {
        val dateTime = DateUtils.formatDateTime(this, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
        timeView.text = getString(R.string.sms_message, dateTime)
        timeView.visibility = View.VISIBLE
    }
}

package net.sf.power.monitor

import android.app.ForegroundServiceStartNotAllowedException
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_ADJUST_WITH_ACTIVITY
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import net.sf.power.monitor.model.BatteryState
import timber.log.Timber
import java.lang.ref.WeakReference

class PowerConnectionBinder(caller: BinderListener) {

    interface BinderListener {
        fun onBatteryState(state: BatteryState)
        fun onMonitorStatus(polling: Boolean)
        fun onPowerFailed(timeMillis: TimeMillis)
        fun onPowerRestored(timeMillis: TimeMillis)

    }

    private val handler: Handler = MainHandler(caller)

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private val messenger: Messenger = Messenger(handler)

    /**
     * Messenger for communicating with service.
     */
    private var service: Messenger? = null

    /**
     * Flag indicating whether we have called bind on the service.
     */
    private var serviceIsBound: Boolean = false

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

    @Throws(ForegroundServiceStartNotAllowedException::class)
    private fun bindService(context: Context) {
        Timber.i("Service binding.")
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        val intent = Intent(context, PowerConnectionService::class.java)

        // This will keep the service running even after activity destroyed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        context.bindService(intent, connection, BIND_AUTO_CREATE or BIND_ADJUST_WITH_ACTIVITY)
        serviceIsBound = true
    }

    private fun unbindService(context: Context) {
        if (serviceIsBound) {
            Timber.i("Service unbinding.")
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            unregisterClient()

            // Detach our existing connection.
            context.unbindService(connection)
            serviceIsBound = false
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
        } catch (_: RemoteException) {
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
        } catch (_: RemoteException) {
            // There is nothing special we need to do if the service has crashed.
        }
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

    fun startMonitor() {
        // We want to monitor the service for as long as we are connected to it.
        try {
            notifyService(PowerConnectionService.MSG_START_MONITOR)
        } catch (_: RemoteException) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
    }

    fun stopMonitor() {
        try {
            notifyService(PowerConnectionService.MSG_STOP_MONITOR)
        } catch (_: RemoteException) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
    }

    fun fetchState() {
        try {
            notifyService(PowerConnectionService.MSG_GET_STATUS_MONITOR)
        } catch (_: RemoteException) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
    }

    @Throws(ForegroundServiceStartNotAllowedException::class)
    fun onStart(context: Context) {
        bindService(context)
    }

    fun onStop(context: Context) {
        unbindService(context)
    }

    fun fail() {
        notifyService(PowerConnectionService.MSG_FAILED, 0, 0, System.currentTimeMillis())
    }

    private class MainHandler(caller: BinderListener) : Handler(Looper.getMainLooper()) {

        private val caller: WeakReference<BinderListener> = WeakReference(caller)

        override fun handleMessage(msg: Message) {
            val caller = this.caller.get() ?: return

            when (msg.what) {
                MSG_STATE_CHANGED -> caller.onBatteryState(msg.obj as BatteryState)

                MSG_SET_STATUS_MONITOR -> caller.onMonitorStatus(msg.arg1 != 0)

                MSG_FAILED -> caller.onPowerFailed(msg.obj as TimeMillis)

                MSG_RESTORED -> caller.onPowerRestored(msg.obj as TimeMillis)
            }
        }

        companion object {
            const val MSG_FAILED = PowerConnectionService.MSG_FAILED
            const val MSG_RESTORED = PowerConnectionService.MSG_RESTORED
            const val MSG_SET_STATUS_MONITOR = PowerConnectionService.MSG_SET_STATUS_MONITOR
            const val MSG_STATE_CHANGED = PowerConnectionService.MSG_BATTERY_CHANGED
        }
    }
}
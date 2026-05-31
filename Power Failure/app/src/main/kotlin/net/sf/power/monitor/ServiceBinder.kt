package net.sf.power.monitor

import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.sf.power.monitor.PowerConnectionService.Companion.ACTION_FAILED
import net.sf.power.monitor.PowerConnectionService.Companion.ACTION_GET_STATUS_MONITOR
import net.sf.power.monitor.PowerConnectionService.Companion.ACTION_PREFERENCES_CHANGED
import net.sf.power.monitor.PowerConnectionService.Companion.ACTION_REGISTER_CLIENT
import net.sf.power.monitor.PowerConnectionService.Companion.ACTION_SET_STATUS_MONITOR
import net.sf.power.monitor.PowerConnectionService.Companion.ACTION_START_MONITOR
import net.sf.power.monitor.PowerConnectionService.Companion.ACTION_STOP_MONITOR
import net.sf.power.monitor.PowerConnectionService.Companion.ACTION_UNREGISTER_CLIENT
import net.sf.power.monitor.PowerConnectionService.Companion.EXTRA_ARG1
import net.sf.power.monitor.PowerConnectionService.Companion.EXTRA_ARG2
import net.sf.power.monitor.PowerConnectionService.Companion.EXTRA_CLIENT
import timber.log.Timber

class ServiceBinder(private val context: Context, private val caller: BinderListener) {

    interface BinderListener {
        fun onClientRegistered(registered: Boolean)
        fun onMonitorStatus(polling: Boolean)
    }

    private val messenger: LocalBroadcastManager by lazy { LocalBroadcastManager.getInstance(context) }

    /**
     * Flag indicating whether the service is running in the foreground.
     */
    private var isServiceStarted: Boolean = false

    private val clientId: String = toString()
    private val target by lazy { ComponentName(context, PowerConnectionService::class.java) }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.component == target) return
            val client = intent.getStringExtra(EXTRA_CLIENT)
            if (client != clientId) return
            Timber.d("onReceive $intent")
            val action = intent.action
            val arg1 = intent.getIntExtra(EXTRA_ARG1, 0)

            // Any response from the service means that it is alive.
            isServiceStarted = true

            when (action) {
                ACTION_REGISTER_CLIENT -> caller.onClientRegistered(arg1 != 0)

                ACTION_SET_STATUS_MONITOR -> caller.onMonitorStatus(arg1 != 0)
            }
        }
    }

    @Throws(ForegroundServiceStartNotAllowedException::class)
    private fun startService(action: String = ACTION_START_MONITOR) {
        Timber.i("Start service.")

        val intent = Intent(action)
            .setComponent(target)
            .putExtra(EXTRA_CLIENT, clientId)

        // This will keep the service running even after activity destroyed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopService() {
        Timber.i("Stop service.")
        notifyService(ACTION_STOP_MONITOR)
        isServiceStarted = false
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

    /**
     * Register this client with the service to prevent `Service.stopSelf`.
     */
    private fun registerClient() {
        Timber.i("Register with service.")
        notifyService(ACTION_REGISTER_CLIENT)
    }

    private fun unregisterClient() {
        Timber.i("Unregister from service.")
        notifyService(ACTION_UNREGISTER_CLIENT)
    }

    private fun notifyService(action: String, arg1: Int = 0, arg2: Int = 0) {
        val intent = Intent(action)
            .setComponent(target)
            .putExtra(EXTRA_ARG1, arg1)
            .putExtra(EXTRA_ARG2, arg2)
            .putExtra(EXTRA_CLIENT, clientId)
        messenger.sendBroadcast(intent)
    }

    fun start() {
        Timber.v("binder start")
        startService()
    }

    fun stop() {
        Timber.v("binder stop")
        stopService()
    }

    fun fail() {
        if (isServiceStarted) {
            notifyService(ACTION_FAILED)
        } else {
            startService(ACTION_FAILED)
        }
    }

    fun bind() {
        registerMessenger()
        registerClient()
    }

    fun unbind() {
        unregisterClient()
        messenger.unregisterReceiver(receiver)
    }

    fun onStart() {
        bind()
    }

    fun onStop() {
        unbind()
    }
}
package net.sf.power.monitor

import android.app.Application
import com.github.util.LogTree
import timber.log.Timber

class PowerMonitorApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(LogTree(BuildConfig.DEBUG))
    }
}
package net.sf.power.monitor

import android.app.Application
import com.github.util.LogTree
import net.sf.power.monitor.log.CrashlyticsTree
import timber.log.Timber

class PowerMonitorApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val tree = if (BuildConfig.CRASHLYTICS) {
            CrashlyticsTree(BuildConfig.DEBUG)
        } else {
            LogTree(BuildConfig.DEBUG)
        }
        Timber.plant(tree)
    }
}
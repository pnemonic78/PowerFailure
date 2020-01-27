package net.sf.power.monitor

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.github.util.LogTree
import io.fabric.sdk.android.Fabric
import timber.log.Timber

class PowerMonitorApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Fabric.with(this, Crashlytics())
        Timber.plant(LogTree(BuildConfig.DEBUG))
    }
}
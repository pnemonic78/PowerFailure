package net.sf.power.monitor

import android.app.Application
import com.github.util.LogTree
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import net.sf.power.monitor.log.CrashlyticsTree
import timber.log.Timber

class PowerMonitorApplication : Application() {
    @OptIn(DelicateCoroutinesApi::class)
    val poll = ChargerPoll(this, GlobalScope)

    override fun onCreate() {
        super.onCreate()

        val tree = if (BuildConfig.CRASHLYTICS) {
            CrashlyticsTree(BuildConfig.DEBUG)
        } else {
            LogTree(BuildConfig.DEBUG)
        }
        Timber.plant(tree)
    }

    override fun onTerminate() {
        poll.kill()
        super.onTerminate()
    }
}
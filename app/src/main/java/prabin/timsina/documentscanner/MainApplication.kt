package prabin.timsina.documentscanner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader
import timber.log.Timber

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(timberDebugTree)
        }

        OpenCVLoader.initLocal()
    }
}

/**
 * Add a cutom log tag to make it easier to search in logcat, like:
 * (tag:DocScan_ | message:FATAL) & level:debug
 */
private val timberDebugTree = object : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority = priority, tag = "DocScan_$tag", message = message, t = t)
    }
}

package dev.tomerklein.holocron

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Provides the WorkManager [Configuration] backed by Hilt's
 * [HiltWorkerFactory], so `@HiltWorker`s (e.g. DispatchWorker) get their dependencies
 * injected. The default WorkManager initializer is removed in the manifest.
 */
@HiltAndroidApp
class HolocronApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

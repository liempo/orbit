package com.fourcode.tracking

import android.app.Application
import timber.log.Timber

@Suppress("unused")
class LoggableApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        // Plant a Debug Tree for logging
        Timber.plant(Timber.DebugTree())
    }
}
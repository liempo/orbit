package com.fourcode.tracking

import android.app.Application
import com.github.ajalt.timberkt.Timber

class TimberApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        // Plant a Debug Tree for logging
        Timber.plant(Timber.DebugTree())
    }
}
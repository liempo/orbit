package com.fourcode.tracking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mapbox.mapboxsdk.Mapbox
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Plant a timber debug tree
        Timber.plant(Timber.DebugTree())

        // Initialize Mapbox here because it will be called by
        // multiple fragments under this Activity
        Mapbox.getInstance(this, BuildConfig.MapboxApiKey)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

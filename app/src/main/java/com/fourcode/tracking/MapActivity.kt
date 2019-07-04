package com.fourcode.tracking

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mapbox.android.core.location.*
import com.mapbox.mapboxsdk.Mapbox
import java.lang.Exception

class MapActivity : AppCompatActivity(),
    LocationEngineCallback<LocationEngineResult> {

    private lateinit var locationEngine: LocationEngine

    override fun onCreate(savedInstanceState: Bundle?) {

        // Mapbox access token is configured here.
        Mapbox.getInstance(this, BuildConfig.MapboxApiKey)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

    }

    @SuppressLint("MissingPermission")
    private fun initializeLocationEngine() {
        // Intialize locationEngine
        locationEngine = LocationEngineProvider
            .getBestLocationEngine(this).also {
                it.requestLocationUpdates(

                    // Build a LocationEngineRequest
                    LocationEngineRequest.Builder(DEFAULT_INTERVAL_MS)
                        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                        .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                        .build(),

                    // Set callback to this class and pass activity's main looper
                    this@MapActivity, mainLooper)
            }
    }

    /* LocationEngineCallback methods */
    override fun onSuccess(result: LocationEngineResult?) {
        
    }

    override fun onFailure(exception: Exception) {

    }

    companion object {

        private const val DEFAULT_INTERVAL_MS = 1000L
        private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_MS * 5
    }
}

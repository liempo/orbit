package com.fourcode.tracking.ui.map

import android.content.Intent
import android.graphics.Color
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.fourcode.tracking.BuildConfig

import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.*
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.modes.*
import com.mapbox.mapboxsdk.maps.*
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

// Location Engine is too long for me i'm sorry guys
import com.mapbox.android.core.location.LocationEngineCallback as EngineCallback
import com.mapbox.android.core.location.LocationEngineResult as EngineResult
import com.mapbox.android.core.location.LocationEngineRequest as EngineRequest

import com.fourcode.tracking.R
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions

import kotlinx.android.synthetic.main.map_fragment.*

import java.lang.Exception

class MapFragment : Fragment(),
    EngineCallback<EngineResult>,
    PermissionsListener {

    // LiveData object (Android jetpack)
    private lateinit var model: MapViewModel

    // Main Mapbox Object
    private lateinit var map: MapboxMap

    // Will make things less complicated. /
    private lateinit var permissions: PermissionsManager

    // AUtocomplete Intent for opening mapbox's place picker
    private lateinit var autocompleteIntent: Intent

    // Location engine, retrieves device's location
    private lateinit var locationEngine: LocationEngine

    // Location request, for the engine parameters
    private val locationEngineRequest: EngineRequest by lazy {
        EngineRequest.Builder(DEFAULT_INTERVAL_MS)
            .setPriority(EngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mapbox access token is configured here. This needs to be called either
        // in your application object or in the same activity which contains the map_view.
        context?.let { Mapbox.getInstance(it, BuildConfig.MapboxApiKey) }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        model = ViewModelProviders.of(this)
            .get(MapViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.map_fragment,
        container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize and configure map_view
        map_view.onCreate(savedInstanceState)

        // Get MapboxMap object
        map_view.getMapAsync {

            // Use class attribute from now in
            map = it

            // Initialize a style then run callback
            map.setStyle(Style.TRAFFIC_DAY) { style ->

                // Initialize location layer, and polylines
                initializeMapComponents(style)

                // Check permission before enabling stuff
                if (PermissionsManager.areLocationPermissionsGranted(context))
                    initializeLocationEngine()
                else {
                    permissions = PermissionsManager(this@MapFragment)
                    permissions.requestLocationPermissions(requireActivity())
                }
            }
        }

        // Setup some pretty simple stuff
        add_destination_fab.setOnClickListener { startAutocomplete() }
    }

    private fun initializeMapComponents(style: Style) {
        context?.let {context ->

            // Initialize location component
            with(map.locationComponent) {
                // Activate location component with the following options
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, style)
                        .useDefaultLocationEngine(false)
                        .build())

                // Set component modes and enable
                renderMode = RenderMode.NORMAL
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }

            // Initialize route source
            style.addSource(
                GeoJsonSource(
                    ROUTE_SOURCE_ID,
                    FeatureCollection.fromFeatures(arrayOf())
                )
            )

            // Initialize route source
            style.addSource(
                GeoJsonSource(
                    DEST_SOURCE_ID,
                    FeatureCollection.fromFeatures(arrayOf())
                )
            )

            // Add route layer to style
            style.addLayer(
                LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND),
                    lineWidth(5f),
                    lineColor(Color.parseColor("#009688"))
                )
            )

            // Add icon to style
            style.addImage(
                DEST_ICON_ID, resources.getDrawable(
                    R.drawable.ic_place_primary_24dp, activity?.theme
                )
            )

            // Add symbol layer for the dest icon
            style.addLayer(
                SymbolLayer(DEST_LAYER_ID, DEST_SOURCE_ID).withProperties(
                    iconImage(DEST_ICON_ID),
                    iconIgnorePlacement(true),
                    iconIgnorePlacement(true),
                    iconOffset(arrayOf(0f, -4f))
                )
            )
        }
    }

    private fun initializeLocationEngine() {
        context?.let { context ->
            locationEngine = LocationEngineProvider
                .getBestLocationEngine(context)
            locationEngine.requestLocationUpdates(
                locationEngineRequest,
                this, context.mainLooper)
        }
    }

    private fun startAutocomplete(point: Point? = null) {
        // Initialize autocomplete if it ain't (if it ain't, if it ain't)
        if (::autocompleteIntent.isInitialized.not()) {
            // Build intent for autocomplete
            val builder = PlaceOptions.builder()
                .backgroundColor(ContextCompat.getColor(context!!,
                    R.color.colorAutocompleteBackground))
            if (point != null) builder.proximity(point)

            autocompleteIntent = PlaceAutocomplete.IntentBuilder()
                .accessToken(BuildConfig.MapboxApiKey)
                .placeOptions(builder.build())
                .build(requireActivity())
        }

        startActivity(autocompleteIntent)
    }

    /************ Mapbox permission listeners methods  ************/
    override fun onPermissionResult(granted: Boolean) {
        initializeLocationEngine()
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {}

    /************ Mapbox location engine callback methods  ************/
    override fun onSuccess(result: EngineResult?) {
        result?.lastLocation?.run {
            map.locationComponent.forceLocationUpdate(this)

            if (result.locations.size == 1) {
                // Move camera to center
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(latitude, longitude),  MAP_ZOOM_DEFAULT))
            }
        }

    }

    override fun onFailure(exception: Exception) {

    }



    /************* Mapbox lifecycle boiler plate alert!!!! **********/
    override fun onStart() {
        super.onStart()
        map_view.onStart()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onStop() {
        super.onStop()
        map_view.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_view.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    companion object {
        // constants for the LocationEngineResult
        private const val DEFAULT_INTERVAL_MS = 2000L
        private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_MS * 5

        // Mapbox style and layers ids
        private const val ROUTE_LAYER_ID = "route_layer"
        private const val ROUTE_SOURCE_ID = "route_source"
        private const val DEST_ICON_ID = "dest_icon_id"
        private const val DEST_LAYER_ID = "dest_layer_id"
        private const val DEST_SOURCE_ID = "dest_source_id"

        // Mapbox constants
        private const val MAP_ZOOM_DEFAULT = 15.0
    }

}

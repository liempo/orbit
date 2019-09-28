package com.fourcode.tracking.standard.home


import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.R
import com.fourcode.tracking.standard.home.WaypointsAdapter.Waypoint
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.services.android.navigation.v5.utils.time.TimeFormatter
import kotlinx.android.synthetic.main.fragment_standard.*
import timber.log.Timber

class HomeFragment : Fragment(), PermissionsListener {

    // Shared View model for standard log in
    private lateinit var model: StandardViewModel

    // Mapbox tool to retrieve device's location
    private lateinit var engine: LocationEngine

    // Adapter object for recycler view
    private lateinit var adapter: WaypointsAdapter

    // Intent for autocomplete (search)
    private val autocomplete: Intent
        get() {
            val options = PlaceOptions.builder()
                .backgroundColor(requireActivity()
                    .getColor(android.R.color.white))
            if (model.origin.value != null)
                options.proximity(model.origin.value!!.point)
            return PlaceAutocomplete.IntentBuilder()
                .accessToken(BuildConfig.MapboxApiKey)
                .placeOptions(options.build())
                .build(activity)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.fragment_standard,
        container, false
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize recycler view
        with (waypoints_recycler_view) {
            layoutManager = LinearLayoutManager(context)
            this@HomeFragment.adapter = WaypointsAdapter()
            adapter = this@HomeFragment.adapter
            setItemViewCacheSize(0)
        }

        bottom_app_bar.setNavigationOnClickListener {
            OptionsBottomDialogFragment().apply {
                show(this@HomeFragment.requireFragmentManager(), tag)
            }
        }

        add_destination_button.setOnClickListener {
            startActivityForResult(autocomplete, REQUEST_AUTOCOMPLETE)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Initialize view model
        model = ViewModelProviders.of(this)
            .get(StandardViewModel::class.java)

        model.origin.observe(this, Observer {
            Timber.d("Origin: ${it.name}, (${it.point})")
            adapter.origin(it)
        })

        model.route.observe(this, Observer {
            // return if null
            if (it == null) return@Observer

            // Update distance if it.distance() does not return null
            it.distance()?.let { distance ->
                val km = distance / 1000
                total_distance_text.text =
                    if (distance >= 1000)
                        getString(R.string.format_distance_kilometers, km)
                    else getString(R.string.format_distance_meters, distance)

                val fuelCost = (km / 25) * 3
                fuel_cost_text.text = getString(
                    R.string.format_fuel_cost, fuelCost)
            }

            // Update duration if it.duration() does not return null
            it.duration()?.let { duration ->
                travel_time_text.text = TimeFormatter.
                    formatTimeRemaining(context, duration)
            }
        })

        // Initialize location engine with context
        context?.let { ctx ->
            engine = LocationEngineProvider
                .getBestLocationEngine(ctx)

            // Quick permission check
            if (PermissionsManager.areLocationPermissionsGranted(ctx))
                // Get initial location
                engine.getLastLocation(model)
                // else ask for permissions
            else PermissionsManager(this).apply {
                requestLocationPermissions(requireActivity()) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_AUTOCOMPLETE && resultCode == RESULT_OK)
            with (PlaceAutocomplete.getPlace(data)) {
                // Get last item in recycler view
                val last = adapter.last()

                // Check if last item is same to new item
                if (last.point.latitude() == center()!!.latitude() &&
                        last.point.longitude() == center()!!.longitude())
                    Snackbar.make(container,
                        getString(R.string.error_already_added, text()),
                        Snackbar.LENGTH_SHORT)
                        .show()
                else {
                    adapter.add(Waypoint(text()!!, center()!!))
                    model.getBestRoute(adapter.items)
                }
            }
    }

    override fun onPermissionResult(granted: Boolean) {
        // Get initial location
        engine.getLastLocation(model)
    }

    override fun onExplanationNeeded(
        permissionsToExplain: MutableList<String>?
    ) {}

    companion object {
        private const val REQUEST_AUTOCOMPLETE = 5421
    }
}
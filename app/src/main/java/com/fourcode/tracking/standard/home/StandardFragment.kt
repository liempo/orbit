package com.fourcode.tracking.standard.home


import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager

import com.fourcode.tracking.R

import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import kotlinx.android.synthetic.main.fragment_standard.*

import timber.log.Timber

class StandardFragment : Fragment(), PermissionsListener {

    // Shared View model for standard log in
    private lateinit var model: StandardViewModel

    // Mapbox tool to retrieve device's location
    private lateinit var engine: LocationEngine

    // Adapter object for recycler view
    private lateinit var adapter: WaypointsAdapter

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
            this@StandardFragment.adapter = WaypointsAdapter()
            adapter = this@StandardFragment.adapter
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Intialize view model
        model = ViewModelProviders.of(this)
            .get(StandardViewModel::class.java)

        model.origin.observe(this, Observer {
            Timber.d("Origin: ${it.name}, (${it.point})")
            adapter.origin(it)
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

    override fun onPermissionResult(granted: Boolean) {
        // Get initial location
        engine.getLastLocation(model)
    }

    override fun onExplanationNeeded(
        permissionsToExplain: MutableList<String>?
    ) {}
}

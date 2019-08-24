package com.fourcode.tracking.ui.map

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.models.CarmenFeature

class MapViewModel : ViewModel() {

    /** Current updated position will be saved here. */
    val location: MutableLiveData<Location> = MutableLiveData()

    /** Recently added destination will be saved here.
     * A list of destinations will be saved in RecyclerView's adapter */
    val destinations: MutableLiveData<ArrayList<CarmenFeature>> = MutableLiveData()

    /** Contains data about the generated route from destinations */
    val route: MutableLiveData<DirectionsRoute> = MutableLiveData()

    companion object {
        // constants for the LocationEngineResult
        internal const val DEFAULT_INTERVAL_MS = 2000L
        internal const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_MS * 5

        // Mapbox style and layers ids
        internal const val ROUTE_LAYER_ID = "route_layer"
        internal const val ROUTE_SOURCE_ID = "route_source"
        internal const val DEST_ICON_ID = "dest_icon_id"
        internal const val DEST_LAYER_ID = "dest_layer_id"
        internal const val DEST_SOURCE_ID = "dest_source_id"
    }

}

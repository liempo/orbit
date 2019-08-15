package com.fourcode.tracking.ui.map

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.models.CarmenFeature

class MapViewModel: ViewModel() {

    /** Current updated position will be saved here. */
    val location: MutableLiveData<Location> = MutableLiveData()

    /** Recently added destination will be saved here.
     * A list of destinations will be saved in RecyclerView's adapter
     * TODO: Bind this to recycler view */
    val destinations: MutableLiveData<List<CarmenFeature>> = MutableLiveData()

    /** Contains data about the generated route from Mapbox*/
    val route: MutableLiveData<DirectionsRoute> = MutableLiveData()
}
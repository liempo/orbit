package com.fourcode.tracking.ui.map

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.api.geocoding.v5.models.CarmenFeature

class MapViewModel: ViewModel() {

    /** Current updated position will be saved here. */
    val location: MutableLiveData<Location> = MutableLiveData()

    /** Recently added destination will be saved here.
     * A list of destinations will be saved in RecyclerView's adapter
     * TODO: Bind this to recycler view */
    val destinations: MutableLiveData<List<CarmenFeature>> = MutableLiveData()

    /** Total distance of the generated route in meters */
    val distance: MutableLiveData<Double> = MutableLiveData()

    /** Total duration of the generated route in seconds */
    val duration: MutableLiveData<Double> = MutableLiveData()
}
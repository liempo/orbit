package com.fourcode.tracking.ui.map

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.models.CarmenFeature

class MapViewModel : ViewModel() {

    /** Recently added destination will be saved here.
     * A list of destinations will be saved in RecyclerView's adapter */
    val destinations: MutableLiveData<ArrayList<CarmenFeature>> = MutableLiveData()

}

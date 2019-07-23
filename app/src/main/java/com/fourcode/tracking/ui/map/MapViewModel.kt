package com.fourcode.tracking.ui.map

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.api.geocoding.v5.models.CarmenFeature

class MapViewModel: ViewModel() {

    /** Current updated position will be saved here. */
    val location: MutableLiveData<Location> = MutableLiveData()

    /** List of destinations set*/
    val destinations: MutableLiveData<ArrayList<CarmenFeature>> = MutableLiveData()
}
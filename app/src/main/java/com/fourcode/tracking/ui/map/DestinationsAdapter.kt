package com.fourcode.tracking.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.fourcode.tracking.R
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import kotlinx.android.synthetic.main.item_destinations.view.*

class DestinationsAdapter(val items: ArrayList<CarmenFeature>,
                          private val map: MapboxMap):
    RecyclerView.Adapter<DestinationsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_destinations,
                parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.destination.text = item.text()
        holder.card.setOnClickListener {
            item.center()?.let {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.latitude(), it.longitude()), 15.0))
            }
        }
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val destination: TextView = view.destination_text
        val card: CardView = view.card_view
    }
}
package com.fourcode.tracking.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fourcode.tracking.R
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import kotlinx.android.synthetic.main.destination_item.view.*

class DestinationAdapter(
    private val items: ArrayList<CarmenFeature> = arrayListOf()):
    RecyclerView.Adapter<DestinationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
            .inflate(R.layout.destination_item,
                parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.destination.text = items[position].text()
    }

    internal fun add(destination: CarmenFeature) {
        items.add(destination); notifyDataSetChanged()
    }

    internal fun delete(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)

    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val destination: TextView = view.destination_text
    }
}
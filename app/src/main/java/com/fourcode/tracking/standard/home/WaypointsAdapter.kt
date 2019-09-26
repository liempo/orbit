package com.fourcode.tracking.standard.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.fourcode.tracking.R
import com.mapbox.geojson.Point

import kotlinx.android.synthetic.main.item_waypoint.view.*
import timber.log.Timber

class WaypointsAdapter: RecyclerView.Adapter<WaypointsAdapter.ViewHolder>() {

    private val items = arrayListOf<Waypoint>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            ViewHolder = ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_waypoint,
                    parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get item
        val item = items[position]

        // Log all items' positions
        items.forEach {
            Timber.d("${it.name}, ${it.position}")
        }

        // Set name to item name
        holder.name.text = item.name

        holder.favorite.setOnClickListener {
            // TODO Add to room
        }

        holder.start.visibility = View.INVISIBLE
        holder.end.visibility = View.INVISIBLE

        when (item.position) {
            Waypoint.Position.START -> {
                if (itemCount > 1)
                    holder.start.visibility = View.VISIBLE
                holder.location.visibility = View.VISIBLE
            }

            Waypoint.Position.MIDDLE -> {
                holder.start.visibility = View.VISIBLE
                holder.end.visibility = View.VISIBLE
            }

            Waypoint.Position.END -> {
                holder.end.visibility = View.VISIBLE
            }
        }
    }

    internal fun origin(destination: Waypoint) {
        destination.position = Waypoint.Position.START

        if (items.size > 0) {
            items[0] = destination
            notifyItemChanged(0)
        } else {
            items.add(destination)
            notifyDataSetChanged()
        }
    }

    internal fun add(destination: Waypoint) {
        // Update previous item's icon
        if (items.lastIndex != 0) {
            items[items.lastIndex] = items[items.lastIndex]
                .apply { position = Waypoint.Position.MIDDLE }
            notifyItemChanged(items.lastIndex)
        }

        // Add new data
        items.add(destination)
        notifyDataSetChanged()
    }

    internal fun last() = items.last()

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {

        // For aesthetic feel
        val start: ImageView = view.left_line_icon_bottom
        val end: ImageView = view.left_line_icon_top

        val name: TextView = view.destination_name
        val location: ImageView = view.my_location
        val favorite: ImageButton = view.favorite_button

    }

    internal data class Waypoint(
        val name: String,
        val point: Point,
        var position: Position = Position.END
    ) {
        enum class Position {
            START, MIDDLE, END
        }
    }
}
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

        // Set name to item name
        holder.name.text = item.name

        when (item.position) {
            Waypoint.Position.START -> {
                if (items.size > 1)
                    holder.start.visibility = View.VISIBLE

                holder.name.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_my_location_primary_dark_24dp,
                    0, 0, 0
                )

                holder.delete.visibility = View.GONE
                holder.favorite.visibility = View.GONE
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
        // Set middle to true to enable full icon
        if (items.last().position != Waypoint.Position.START)
            items[items.lastIndex] = items[items.lastIndex]
                .apply {
                    position = Waypoint.Position.MIDDLE
                }; notifyItemChanged(items.lastIndex)

        // Add new data
        items.add(destination)
        notifyDataSetChanged()
    }

    internal fun delete(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    internal fun last() = items.last()


    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {

        // For aesthetic feel
        val start: ImageView = view.left_line_icon_bottom
        val end: ImageView = view.left_line_icon_top

        val name: TextView = view.destination_name
        val favorite: ImageButton = view.favorite_button
        val delete: ImageButton = view.delete_button

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
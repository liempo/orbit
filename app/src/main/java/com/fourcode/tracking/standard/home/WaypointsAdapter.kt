package com.fourcode.tracking.standard.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.fourcode.tracking.R

import kotlinx.android.synthetic.main.item_waypoint.view.*

class WaypointsAdapter: RecyclerView.Adapter<WaypointsAdapter.ViewHolder>() {

    private val items =
        arrayListOf<StandardViewModel.Waypoint>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            ViewHolder = ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_waypoint,
                    parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // Create item from position
        holder.name.text = items[position].name

        if (position == 0)
            holder.name.context.let { ctx ->
                if (items.size > 1)
                    holder.start.visibility = View.VISIBLE

                holder.name.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_my_location_primary_dark_24dp,
                    0, 0, 0)

                holder.delete.visibility = View.GONE
                holder.favorite.visibility = View.GONE
            }
        else if (position == items.lastIndex && items.size > 0)
            holder.end.visibility = View.VISIBLE

    }

    internal fun origin(destination: StandardViewModel.Waypoint) {
        if (items.size > 0) {
            items[0] = destination
            notifyItemChanged(0)
        } else {
            items.add(destination)
            notifyDataSetChanged()
        }
    }

    internal fun add(destination: StandardViewModel.Waypoint) {
        items.add(destination)
        notifyDataSetChanged()
    }

    internal fun delete(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }


    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {

        // For aesthetic feel
        val start: ImageView = view.left_line_icon_bottom
        val end: ImageView = view.left_line_icon_top

        val name: TextView = view.destination_name
        val favorite: ImageButton = view.favorite_button
        val delete: ImageButton = view.delete_button

    }
}
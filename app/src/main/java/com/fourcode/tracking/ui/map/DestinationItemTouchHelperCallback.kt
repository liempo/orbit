package com.fourcode.tracking.ui.map

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView

class DestinationItemTouchHelperCallback : ItemTouchHelper.SimpleCallback(
    UP or DOWN or START or END, 0) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val adapter = recyclerView.
            adapter as DestinationAdapter

        // Get positions
        val from = viewHolder.adapterPosition
        val to = target.adapterPosition

        // Move items
        adapter.notifyItemMoved(from, to)

        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
}
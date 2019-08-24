package com.fourcode.tracking.ui.map

import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView

class DestinationItemTouchHelperCallback(private val model: MapViewModel):
    ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, 0) {

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
        model.destinations.swap(from, to)

        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    private fun <T> MutableLiveData<ArrayList<T>>.swap(from: Int, to: Int) {
        // Crate an new list if list live data is null
        val newList =
            if (value == null)
                arrayListOf()
            else value!!

        // Swap elements
        val fromItem = newList[from]
        val toItem = newList[to]
        newList[to] = fromItem
        newList[from] = toItem

        // Change value (will notify observers)
        this.value = newList
    }
}
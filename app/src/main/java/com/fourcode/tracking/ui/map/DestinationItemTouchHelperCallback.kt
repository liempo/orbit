package com.fourcode.tracking.ui.map

import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView

class DestinationItemTouchHelperCallback(private val model: MapViewModel,
                                         private val adapter: DestinationAdapter):
    ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, LEFT or RIGHT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        // Get positions
        val from = viewHolder.adapterPosition
        val to = target.adapterPosition

        // Move items
        adapter.notifyItemMoved(from, to)
        model.destinations.swap(from, to)

        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val swipedPosition = viewHolder.adapterPosition
        model.destinations.delete(swipedPosition)
        adapter.delete(swipedPosition)
    }


    private fun <T> MutableLiveData<ArrayList<T>>.swap(from: Int, to: Int) {
        // Crate an new list if list live data is null
        val newList = value!!

        // Swap elements
        val fromItem = newList[from]
        val toItem = newList[to]
        newList[to] = fromItem
        newList[from] = toItem

        // Change value (will notify observers)
        this.value = newList
    }

    private fun <T> MutableLiveData<ArrayList<T>>.delete(position: Int) {
        // Crate an new list if list live data is null
        val newList = value!!

        // Delete item at position
        newList.removeAt(position)

        // Change value (will notify observers)
        this.value = newList
    }
}
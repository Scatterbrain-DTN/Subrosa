package net.ballmerlabs.subrosa.util

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class MapRecyclerViewAdapter<V: Any, T: HasKey<V>, U: RecyclerView.ViewHolder>: RecyclerView.Adapter<U>() {
    protected val valueMap = ConcurrentHashMap<V, T>()

    protected val values = mutableListOf<V>()


    fun addItem(item: T) {
        if(valueMap.putIfAbsent(item.hasKey(), item) == null) {
            val index = values.size
            values.add(index, item.hasKey())
            notifyItemChanged(index)
        }
    }

    fun addItems(items: List<T>) {
        val uuids = items.map { i -> i.hasKey() }
        val removes = valueMap.keys - uuids
        items.forEach { item -> addItem(item) }
        removes.forEach { item -> delItem(item) }
    }

    fun delItem(item: V) {
        if (valueMap.remove(item) != null) {
            Log.v("debug", "removeItem $item")
            val index = values.indexOf(item)
            values.removeAt(index)
            notifyItemRemoved(index)
        }
    }

}
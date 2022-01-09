package net.ballmerlabs.subrosa.listing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.GroupItemBinding
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.util.MapRecyclerViewAdapter
import java.util.*
import kotlin.collections.ArrayList

class GroupListRecyclerViewAdapter(private val itemClickListener: (group: NewsGroup) -> Unit) :
    MapRecyclerViewAdapter<UUID, NewsGroup, GroupListRecyclerViewAdapter.ViewHolder>() {

    inner class ViewHolder(view: View, private val listener: (group: NewsGroup) -> Unit):
        RecyclerView.ViewHolder(view) {
        private val binding: GroupItemBinding = GroupItemBinding.bind(view)

        var newsGroup = NewsGroup.empty()
        set(value) {
            binding.name.text = value.name
            binding.itemIdenticon.hash = value.hashCode()
            binding.root.setOnClickListener { listener(value) }
            field = value
        }

        init {
            binding.root.setOnClickListener { listener(newsGroup) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.group_item, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = valueMap[values[position]]!!
        holder.newsGroup = item
    }

    override fun getItemCount(): Int = values.size
}
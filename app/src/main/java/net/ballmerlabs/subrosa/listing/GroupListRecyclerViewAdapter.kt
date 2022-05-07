package net.ballmerlabs.subrosa.listing

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.GroupItemBinding
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.util.MapRecyclerViewAdapter
import java.util.*

class GroupListRecyclerViewAdapter(
    private val repository: NewsRepository? = null,
    private val itemClickListener: (group: NewsGroup) -> Unit
    ) :
    MapRecyclerViewAdapter<UUID, NewsGroup, GroupListRecyclerViewAdapter.ViewHolder>() {

    inner class ViewHolder(view: View, private val listener: (group: NewsGroup) -> Unit):
        RecyclerView.ViewHolder(view) {
        private val binding: GroupItemBinding = GroupItemBinding.bind(view)

        var newsGroup = NewsGroup.empty()
        set(value) {
            binding.name.text = value.groupName
            binding.itemIdenticon.hash = value.hash.contentHashCode()
            binding.root.setOnClickListener { listener(value) }
            field = value
        }

        var unread: Int = 0
        get() = Integer.parseInt(binding.unreadNum.text.toString())
        set(value) {
            binding.unreadNum.text = value.toString()
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
        repository?.coroutineScope?.launch(Dispatchers.Main) {
            val count = repository.countPost(item.uuid)
            Log.v("debug", "post count for ${item.uuid}: $count")
            holder.unread = count
        }
    }

    override fun getItemCount(): Int = values.size
}
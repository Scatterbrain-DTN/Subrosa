package net.ballmerlabs.subrosa.listing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.GroupItemBinding
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup

class GroupListRecyclerViewAdapter(private val itemClickListener: (group: NewsGroup) -> Unit) :
    RecyclerView.Adapter<GroupListRecyclerViewAdapter.ViewHolder>() {

    val values: ArrayList<NewsGroup> = arrayListOf()

    inner class ViewHolder(view: View, private val listener: (group: NewsGroup) -> Unit):
        RecyclerView.ViewHolder(view) {
        private val binding: GroupItemBinding = GroupItemBinding.bind(view)

        var newsGroup = NewsGroup.empty()
        set(value) {
            binding.name.text = value.name
            binding.itemIdenticon.hash = value.hashCode()
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
        val item = values[position]
        holder.newsGroup = item
    }

    override fun getItemCount(): Int = values.size
}
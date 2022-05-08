package net.ballmerlabs.subrosa.thread

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.ThreadCardBinding
import net.ballmerlabs.subrosa.scatterbrain.Post
import net.ballmerlabs.subrosa.util.MapRecyclerViewAdapter

class PostListRecylerViewAdapter(private val repository: NewsRepository? = null):
    MapRecyclerViewAdapter<String, Post, PostListRecylerViewAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        private val binding: ThreadCardBinding = ThreadCardBinding.bind(view)

        var fingerprint: CharSequence?
            get() = binding.fingerprint.text
            set(value) {
                binding.fingerprint.text = value?:"Anonymous"
            }

        var body: CharSequence
            get() = binding.postBody.text
            set(value) {
                binding.postBody.text = value
            }

        var name: CharSequence?
            get() = binding.senderName.text
            set(value) {
                binding.senderName.text = value?:"Anonymous"
            }
        var header: CharSequence
            get() = binding.header.text
            set(value) {
                binding.header.text = value
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.thread_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = valueMap[values[position]]
        if (item != null) {
            holder.body = item.body
            holder.fingerprint = item.author?.toString()
            holder.header = item.header
            holder.name = item.user?.userName?: "unknown"
        } else {
            Log.e("debug", "post at $position was null")
        }
    }

    override fun getItemCount(): Int = values.size
}
package net.ballmerlabs.subrosa.thread

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.ThreadCardBinding
import net.ballmerlabs.subrosa.scatterbrain.Post

class PostListRecylerViewAdapter():
    RecyclerView.Adapter<PostListRecylerViewAdapter.ViewHolder>() {

    val values: ArrayList<Post> = arrayListOf()
    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        private val binding: ThreadCardBinding = ThreadCardBinding.bind(view)

        var fingerprint: CharSequence
            get() = binding.fingerprint.text
            set(value) {
                binding.fingerprint.text = value
            }

        var body: CharSequence
            get() = binding.postBody.text
            set(value) {
                binding.postBody.text = value
            }

        var name: CharSequence
            get() = binding.senderName.text
            set(value) {
                binding.senderName.text = value
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
        val item = values[position]
        holder.body = item.body
        holder.fingerprint = item.author.toString()
        holder.header = item.header
        holder.name = "todo: default name"
    }

    override fun getItemCount(): Int = values.size
}
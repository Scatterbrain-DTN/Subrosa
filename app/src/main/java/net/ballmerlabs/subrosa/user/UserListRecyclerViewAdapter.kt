package net.ballmerlabs.subrosa.user

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.database.User
import net.ballmerlabs.subrosa.databinding.FragmentUserlistItemBinding

/**
 * [RecyclerView.Adapter] that can display users
 */
class UserListRecyclerViewAdapter(
    private val values: List<User>
) : RecyclerView.Adapter<UserListRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_userlist_item, parent, false)
        return ViewHolder(view, parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.name = item.name
        holder.uuid = item.identity.toString()
        if (item.owned) holder.setOwned()
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(view: View, private val context: Context) : RecyclerView.ViewHolder(view) {
        private val binding: FragmentUserlistItemBinding = FragmentUserlistItemBinding.bind(view)

        var uuid
        get() = binding.uuid.text
        set(value) { binding.uuid.text = value }

        var name
        get() = binding.name.text
        set(value) { binding.name.text = value }

        fun setOwned() {
            binding.name.setTextAppearance(R.style.TextAppearance_AppCompat_Large_Inverse)
            binding.itemLayout.setBackgroundColor(context.getColor(R.color.material_on_background_disabled))
        }

        init {
            binding.userIdenticon.hash = binding.uuid.hashCode()
        }

        override fun toString(): String {
            return super.toString() + " '" + binding.uuid.text + "'"
        }
    }
}
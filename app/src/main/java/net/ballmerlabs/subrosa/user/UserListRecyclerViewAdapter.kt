package net.ballmerlabs.subrosa.user

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.database.User
import net.ballmerlabs.subrosa.databinding.UserlistItemBinding
import net.ballmerlabs.subrosa.util.MapRecyclerViewAdapter
import java.util.*

/**
 * [RecyclerView.Adapter] that can display users
 */
class UserListRecyclerViewAdapter(
    val context: Context
) : MapRecyclerViewAdapter<UUID, User, UserListRecyclerViewAdapter.ViewHolder>() {

    private var onDeleteClickListener: (uuid: UUID) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.userlist_item, parent, false)
        return ViewHolder(view, parent.context)
    }

    fun setOnDeleteClickListener(func: (uuid: UUID) -> Unit) {
        onDeleteClickListener = func
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val key = values[position]
        val item = valueMap[key]!!
        holder.name = item.name
        holder.uuid = item.identity.toString()
        holder.setImage(item.image)
        if (item.owned) holder.setOwned()
    }

    override fun getItemCount(): Int = values.size

    companion object {
        const val TAG = "UserListRecyclerViewAdapter"
    }

    inner class ViewHolder(view: View, private val context: Context) : RecyclerView.ViewHolder(view) {
        private val binding: UserlistItemBinding = UserlistItemBinding.bind(view)

        var uuid
        get() = binding.uuid.text
        set(value) {
            binding.uuid.text = value
            binding.userIdenticon.hash = value.hashCode()
        }

        var name
        get() = binding.name.text
        set(value) { binding.name.text = value }

        fun setImage(image: Bitmap?) {
            if (image != null) {
                binding.userIdenticon.visibility = View.INVISIBLE
                binding.userImageview.setImageBitmap(image)
                binding.userImageview.visibility = View.VISIBLE
            }
        }

        fun setOwned() {
            binding.name.setTextAppearance(R.style.TextAppearance_AppCompat_Large_Inverse)
            binding.itemLayout.setBackgroundColor(context.getColor(R.color.material_on_background_disabled))
        }



        init {
            binding.menuButton.setOnClickListener { v ->
                val menu = PopupMenu(context, v)
                menu.inflate(R.menu.identity_options_menu)
                menu.setOnMenuItemClickListener { item ->
                    var status = false
                    try {
                        val uuid = UUID.fromString(binding.uuid.text.toString())
                        onDeleteClickListener(uuid)
                        status = true
                    } catch (exc: Exception) {
                        Log.e(TAG, "failed to parse uuid $exc")
                    }
                    status
                }
                menu.show()
            }
        }

        override fun toString(): String {
            return super.toString() + " '" + binding.uuid.text + "'"
        }
    }
}
package net.ballmerlabs.subrosa.user

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.database.User
import net.ballmerlabs.subrosa.databinding.UserlistItemBinding
import java.lang.Exception
import java.util.*

/**
 * [RecyclerView.Adapter] that can display users
 */
class UserListRecyclerViewAdapter(
    var values: List<User>,
    val context: Context
) : RecyclerView.Adapter<UserListRecyclerViewAdapter.ViewHolder>() {

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
        val item = values[position]
        holder.name = item.name
        holder.uuid = item.identity.toString()
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
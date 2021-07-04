package net.ballmerlabs.subrosa.listing

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.widget.ConstraintLayout
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.GroupItemBinding
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.util.toDp
import java.util.*

class GroupItem : ConstraintLayout  {

    private val binding: GroupItemBinding

    private var nameListener: (name: String) -> Unit = {}

    var isCreated = false
        private set

    var newsGroup: NewsGroup? = null
        private set

    val uuid: UUID?
        get() = newsGroup?.uuid

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)

    constructor(context: Context): super(context)

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int):
            super(context, attributeSet, defStyleAttr)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(8.toDp(), 16.toDp(), 8.toDp(), 16.toDp())

        inflate(context, R.layout.group_item, this)
        binding = GroupItemBinding.bind(this)
        binding.nameEdit.editText!!.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.nameEdit.editText!!.setOnEditorActionListener { v, actionId, event ->
            when(actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    nameListener(
                        binding.nameEdit.editText!!.text.toString()
                    )
                    Log.e("debug", "setting text")
                }
                else -> Log.v("debug", "unknown action")
            }
            true
        }
    }

    private fun toggleVisibility() {
        binding.nameEdit.visibility = View.GONE
        binding.name.visibility = View.VISIBLE
        binding.unreadNum.visibility = View.VISIBLE
        binding.itemIdenticon.visibility = View.VISIBLE
        binding.createButton.visibility = View.GONE
    }

    fun set(current: NewsGroup) {
        invalidate()
        binding.name.text = current.name
        newsGroup = current
        isCreated = true
        toggleVisibility()
    }

    fun setOnNameListener(listener: (name: String) -> Unit) {
        this.nameListener = listener
    }
}
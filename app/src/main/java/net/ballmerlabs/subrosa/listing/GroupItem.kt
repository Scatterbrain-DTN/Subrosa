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

    var newsGroup: NewsGroup = NewsGroup.empty()
        set(value) {
            binding.name.text = value.name
            field = value
        }

    val uuid: UUID
        get() = newsGroup.uuid

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)

    constructor(context: Context): super(context)

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int):
            super(context, attributeSet, defStyleAttr)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(8.toDp(), 16.toDp(), 8.toDp(), 16.toDp())

        inflate(context, R.layout.group_item, this)
        binding = GroupItemBinding.bind(this)
        binding.name.text = newsGroup.name
    }
}
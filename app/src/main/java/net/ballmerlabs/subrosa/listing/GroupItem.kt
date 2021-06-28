package net.ballmerlabs.subrosa.listing

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.GroupItemBinding
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import java.util.*

class GroupItem : ConstraintLayout  {

    private val binding: GroupItemBinding

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)

    constructor(context: Context): super(context)

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int):
            super(context, attributeSet, defStyleAttr)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        inflate(context, R.layout.group_item, this)
        binding = GroupItemBinding.bind(this)
    }


    fun create(parent: NewsGroup): NewsGroup {
        val text = binding.nameEdit.editText!!.text.toString()
        val uuid = UUID.randomUUID()
        binding.name.text = text
        binding.nameEdit.visibility = View.GONE
        binding.name.visibility = View.VISIBLE
        binding.unreadNum.visibility = View.VISIBLE
        binding.itemIdenticon.visibility = View.VISIBLE
        binding.createButton.height = 0
        return NewsGroup(
            uuid = uuid,
            parentCol = parent.uuid,
            name = text,
            parentHash = parent.hash
        )
    }
}
package net.ballmerlabs.subrosa.thread

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.ThreadCardBinding

class PostView : MaterialCardView {
    private val binding: ThreadCardBinding
    
    var body
    set(value) {
        binding.postBody.text = value
    }
    get() = binding.postBody.text

    var name
    set(value) {
        binding.senderName.text = value
    }
    get() = binding.senderName.text

    var fingerprint
    set(value) {
        binding.fingerprint.text = value
    }
    get() = binding.fingerprint.text

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)
    
    constructor(context: Context): super(context)

    init {
        inflate(context, R.layout.thread_card, this)
        binding = ThreadCardBinding.bind(this)
    }
}
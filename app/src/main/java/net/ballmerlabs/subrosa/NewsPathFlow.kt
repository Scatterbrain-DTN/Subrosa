package net.ballmerlabs.subrosa



import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams

class NewsPathFlow(context: Context, private val attributeSet: AttributeSet) : MotionLayout(context, attributeSet) {
    private val elements = ArrayList<NewsPathElement>()
    var below: Int? = null
    var spacing: Int = 32


    private fun createElement(name: String): NewsPathElement {
        val path = NewsPathElement(context, attributeSet)
        path.text = name
        path.id = View.generateViewId()
        addView(path)
        return path
    }

    private fun setParams(path: NewsPathElement, prev: Int? = null) {
        path.updateLayoutParams<LayoutParams> {
            if (prev != null) {
                startToEnd = prev
                marginStart = (path.cornerMargin.toInt() * -1) + spacing
            } else {
                startToStart =  LayoutParams.PARENT_ID
            }
            topToBottom =  below ?: View.NO_ID
            topToTop = if (below != null) View.NO_ID else LayoutParams.PARENT_ID
            bottomToBottom = LayoutParams.PARENT_ID
        }
    }  

    fun setPaths(names: Array<String>) {
        var prev = id
        for (i in names.indices) {
            val path = createElement(names[i])
            setParams(path, if (i > 1) prev else null)
            elements.add(path)
            prev = path.id
        }
    }
    
    fun addPath(name: String) {
        val path = createElement(name)
        setParams(path, if(elements.size > 1) elements[elements.size-1].id else null)
        elements.add(path)
    }

    fun removePath() {
        if (elements.size > 0) {
            val path = elements[elements.size - 1]
            removeView(path)
            elements.removeAt(elements.size-1)
        }
    }
}
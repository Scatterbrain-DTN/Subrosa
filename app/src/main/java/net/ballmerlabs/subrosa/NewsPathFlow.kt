package net.ballmerlabs.subrosa



import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams

class NewsPathFlow(context: Context, private val attributeSet: AttributeSet) : MotionLayout(context, attributeSet) {
    private val elements = ArrayList<NewsPathElement>()
    var below: Int? = null
    var spacing: Int = 32
    var removeDuration = 256

    private fun createElement(name: String, alpha: Float = 1F): NewsPathElement {
        val path = NewsPathElement(context, attributeSet)
        path.text = name
        path.alpha = alpha
        path.id = View.generateViewId()
        addView(path)
        return path
    }

    private fun removeParams(path: NewsPathElement) {
        path.updateLayoutParams<LayoutParams> {
            startToEnd = View.NO_ID
            startToStart = View.NO_ID
            endToEnd = View.NO_ID
            endToStart = View.NO_ID
            topToBottom = View.NO_ID
            topToTop = View.NO_ID
            bottomToBottom = View.NO_ID
            bottomToTop = View.NO_ID
        }
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
        elements.clear()
        removeAllViews()
        var prev = id
        for (i in names.indices) {
            val path = createElement(names[i])
            setParams(path, if (i > 0) prev else null)
            elements.add(path)
            prev = path.id
        }
    }
    
    fun addPath(name: String) {
        val path = createElement(name, alpha = 0F)
        setParams(path, if(elements.size > 1) elements[elements.size-1].id else null)
        elements.add(path)
        path.animate()
            .alpha(1F)
            .duration = removeDuration.toLong()
    }

    fun removePath() : String? {
        if (elements.size > 0) {
            val path = elements.removeAt(elements.size - 1)
            path.animate()
                .alpha(0F)
                .setDuration(removeDuration.toLong())
                .setListener(object: Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator) {
                    }

                    override fun onAnimationEnd(p0: Animator) {
                        removeParams(path)
                        removeView(path)
                    }

                    override fun onAnimationCancel(p0: Animator) {
                    }

                    override fun onAnimationRepeat(p0: Animator) {
                    }

                })

            return path.text
        }
        return null
    }
}
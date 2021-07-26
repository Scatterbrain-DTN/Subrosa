package net.ballmerlabs.subrosa

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class NewsPathElement(context: Context, attrs: AttributeSet) : View(context, attrs) {
    var text: String
    val cornerMargin: Float
    private val xpad = max(paddingLeft + paddingRight.toFloat(), 64.toFloat())
    private val ypad = max(paddingTop + paddingBottom.toFloat(), 64.toFloat())
    private val textx = (width - xpad) / 2.0.toFloat()
    private val texty = (height - ypad) / 2.0.toFloat()
    private val textBounds = Rect()
    private val textPaint = Paint().apply {
        color = Color.GRAY
        textAlign = Paint.Align.CENTER
        textSize = 60f
    }
    
    private val rectPaint = Paint().apply { 
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val leftDoinkPath = Path()
    
    private val rightDoinkPath = Path()
    
    init {
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.NewsPathElement,
                0,
                0
        ).apply {
            try {
                text = getString(R.styleable.NewsPathElement_text)?: "fmef"
                when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_NO -> {
                        textPaint.color = getColor(R.styleable.NewsPathElement_textColor, Color.GRAY)
                        rectPaint.color = getColor(R.styleable.NewsPathElement_backgroundColor, Color.BLACK)
                    }
                    Configuration.UI_MODE_NIGHT_YES -> {
                        textPaint.color = getColor(R.styleable.NewsPathElement_textColor, Color.WHITE)
                        rectPaint.color = getColor(R.styleable.NewsPathElement_backgroundColor, Color.LTGRAY)
                    } // Night mode is active, we're using dark theme
                }
                cornerMargin = getDimension(R.styleable.NewsPathElement_edgeRadius, 128F)
            } finally {
                recycle()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val w = textBounds.width() + xpad.toInt() + 256
        val h = textBounds.height() + ypad.toInt()
        setMeasuredDimension(
                MeasureSpec.makeMeasureSpec(w, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        leftDoinkPath.apply {
            moveTo(cornerMargin, measuredHeight.toFloat())
            lineTo(0F, measuredHeight.toFloat())
            lineTo(cornerMargin, measuredHeight/2F)
            lineTo(0F, 0F)
            lineTo(cornerMargin, 0F)
            close()
        }

        rightDoinkPath.apply {
            moveTo(measuredWidth-cornerMargin, measuredHeight.toFloat())
            lineTo(measuredWidth.toFloat(), measuredHeight/2F)
            lineTo(measuredWidth - cornerMargin, 0F)
            close()
        }
        canvas.apply {
            val x = width.toFloat()/2
            val y = (height.toFloat()/2.0.toFloat()) - (textPaint.descent() + textPaint.ascent())/2.0.toFloat()
            drawRect(cornerMargin, 0.toFloat(), width - cornerMargin, height.toFloat(), rectPaint)
            drawPath(leftDoinkPath, rectPaint)
            drawPath(rightDoinkPath, rectPaint)
            drawText(text, x, y, textPaint)
        }
    }
}
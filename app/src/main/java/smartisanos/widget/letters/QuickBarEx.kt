package smartisanos.widget.letters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.smartisanos.music.R
import kotlin.math.ceil
import kotlin.math.max

class QuickBarEx @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val backgroundDrawable: Drawable? = context.getDrawable(R.drawable.letters_bar_background)
    private val shadowDrawable: Drawable? = context.getDrawable(R.drawable.letters_bar_background_shadow)
    private val highlightIcon: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.letters_bar_highlight_icon)
    private val letterMargin = resources.getDimensionPixelSize(R.dimen.smartisan_quickbar_letterbar_margin)
    private val letterFontSize = resources.getDimension(R.dimen.letters_bar_letter_font_size)
    private val singleLetterMinHeight = resources.getDimensionPixelSize(R.dimen.letters_bar_single_letter_min_height)
    private val noChosenColor = context.getColor(R.color.no_chosen_letter_font_color)
    private val hasChosenColor = context.getColor(R.color.has_chosen_letter_font_color)
    private var listener: QBListener? = null
    private var letters: List<String> = DefaultLetters
    private var lastIndex = -1
    private var touchedIndex = -1

    init {
        isClickable = true
        setWillNotDraw(false)
    }

    fun setLongPressEnabled(enabled: Boolean) = Unit

    fun setQBListener(nextListener: QBListener?) {
        listener = nextListener
    }

    fun setLetters(nextLetters: List<String>) {
        letters = nextLetters.ifEmpty { DefaultLetters }
        touchedIndex = -1
        lastIndex = -1
        invalidate()
    }

    fun isLetterGridShown(): Boolean = touchedIndex >= 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val visibleWidth = max(
            resources.getDimensionPixelSize(R.dimen.letters_bar_width),
            (backgroundDrawable?.intrinsicWidth ?: 0) + (shadowDrawable?.intrinsicWidth ?: 0),
        )
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(visibleWidth, MeasureSpec.EXACTLY),
            heightMeasureSpec,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val index = indexForY(event.y)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                touchedIndex = index
                notifyLetter(index, LETTER_CHANGED_CLICK)
                listener?.onLetterGridShow()
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchedIndex = index
                notifyLetter(index, LETTER_CHANGED_SLIDE)
                invalidate()
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                touchedIndex = -1
                lastIndex = -1
                listener?.onLetterGridHidden()
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (letters.isEmpty()) {
            return
        }

        val shadowWidth = shadowDrawable?.intrinsicWidth?.coerceAtLeast(0) ?: 0
        val barLeft = shadowWidth
        val barWidth = (width - shadowWidth).coerceAtLeast(1)
        if (touchedIndex >= 0) {
            shadowDrawable?.setBounds(0, 0, shadowWidth, height)
            shadowDrawable?.draw(canvas)
            backgroundDrawable?.setBounds(barLeft, 0, width, height)
            backgroundDrawable?.draw(canvas)
        }

        val visibleIndexes = calculateVisibleLetterIndexes()
        val availableHeight = (height - letterMargin * 2).coerceAtLeast(1)
        val singleHeight = max(availableHeight.toFloat() / visibleIndexes.size, singleLetterMinHeight.toFloat())
        val centerX = barLeft + barWidth / 2f
        paint.textSize = letterFontSize

        visibleIndexes.forEachIndexed { visibleIndex, letterIndex ->
            val centerY = letterMargin + visibleIndex * singleHeight + singleHeight / 2f
            if (letterIndex == touchedIndex) {
                drawHighlight(canvas, centerX, centerY)
            }
            paint.color = when {
                letterIndex == touchedIndex -> android.graphics.Color.WHITE
                touchedIndex >= 0 -> hasChosenColor
                else -> noChosenColor
            }
            paint.isFakeBoldText = letterIndex == touchedIndex
            val fontMetrics = paint.fontMetricsInt
            val baseline = centerY - (fontMetrics.bottom - fontMetrics.top) / 2f - fontMetrics.top
            canvas.drawText(letters[letterIndex], centerX, baseline, paint)
        }
    }

    private fun drawHighlight(canvas: Canvas, centerX: Float, centerY: Float) {
        val icon = highlightIcon ?: return
        val left = centerX - icon.width / 2f
        val top = centerY - icon.height / 2f
        canvas.drawBitmap(icon, left, top, null)
    }

    private fun calculateVisibleLetterIndexes(): List<Int> {
        val letterCount = letters.size
        if (letterCount <= 1) {
            return listOf(0)
        }
        val availableHeight = (height - letterMargin * 2).coerceAtLeast(1)
        val step = if (availableHeight / letterCount < singleLetterMinHeight) {
            max(2, ceil(letterCount / ((availableHeight / singleLetterMinHeight).coerceAtLeast(1).toFloat() - 2f)).toInt()) * 2
        } else {
            1
        }
        val indexes = mutableListOf(0)
        if (step < letterCount / 2) {
            var index = step
            while (index < letterCount - 1) {
                indexes += index
                index += step
            }
        }
        if (step < letterCount) {
            indexes += letterCount - 1
        }
        return indexes.distinct()
    }

    private fun indexForY(y: Float): Int {
        return ((y / height.coerceAtLeast(1)) * letters.size)
            .toInt()
            .coerceIn(0, letters.lastIndex)
    }

    private fun notifyLetter(index: Int, action: Int) {
        if (index == lastIndex && action == LETTER_CHANGED_SLIDE) {
            return
        }
        lastIndex = index
        listener?.onLetterChanged(letters[index], action)
    }

    interface QBListener {
        fun onLetterChanged(letter: String, action: Int): Boolean

        fun onLetterGridHidden() = Unit

        fun onLetterGridShow() = Unit
    }

    companion object {
        const val LETTER_CHANGED_SLIDE = 0
        const val LETTER_CHANGED_CLICK = 1

        val DefaultLetters = (
            ('A'..'Z').map(Char::toString) + "#"
        )
    }
}

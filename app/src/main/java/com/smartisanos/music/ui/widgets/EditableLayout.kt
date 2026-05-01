package com.smartisanos.music.ui.widgets

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.CheckBox
import android.widget.RelativeLayout
import com.smartisanos.music.R

class EditableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {
    private var editMode = false
    private var checkboxShownMargin: Int? = null
    private var animator: Animator? = null

    fun bindLegacyEditState(
        enabled: Boolean,
        checked: Boolean,
        animate: Boolean,
    ) {
        val checkbox = findViewById<CheckBox>(R.id.cb_del) ?: return
        val params = checkbox.layoutParams as? LayoutParams ?: return
        val shownMargin = checkboxShownMargin ?: params.leftMargin.also {
            checkboxShownMargin = it
        }
        val hiddenMargin = -legacyCheckboxWidth(checkbox)
        val oldMode = editMode
        editMode = enabled
        checkbox.isChecked = checked
        checkbox.isClickable = false
        checkbox.isFocusable = false

        if (!animate || oldMode == enabled) {
            animator?.cancel()
            params.leftMargin = if (enabled) shownMargin else hiddenMargin
            checkbox.layoutParams = params
            checkbox.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
            return
        }

        animator?.cancel()
        checkbox.visibility = View.VISIBLE
        val nextAnimator = AnimatorSet().apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            playTogether(
                ValueAnimator.ofInt(params.leftMargin, if (enabled) shownMargin else hiddenMargin).apply {
                    addUpdateListener { valueAnimator ->
                        params.leftMargin = valueAnimator.animatedValue as Int
                        checkbox.layoutParams = params
                    }
                },
            )
            addListener(
                object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        params.leftMargin = if (enabled) shownMargin else hiddenMargin
                        checkbox.layoutParams = params
                        checkbox.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
                    }
                },
            )
        }
        animator = nextAnimator
        nextAnimator.start()
    }

    private fun legacyCheckboxWidth(checkbox: CheckBox): Int {
        if (checkbox.measuredWidth <= 0) {
            checkbox.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
        }
        return checkbox.measuredWidth
    }
}

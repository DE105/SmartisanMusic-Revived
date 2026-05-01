package com.smartisanos.music.ui.widgets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (getTag(R.id.legacy_folder_row_force_zero_height) == true) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), 0)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun bindLegacyEditState(
        enabled: Boolean,
        checked: Boolean,
        animate: Boolean,
    ) {
        val checkbox = findViewById<CheckBox>(R.id.cb_del) ?: return
        val params = checkbox.layoutParams as? LayoutParams ?: return
        val arrow = findViewById<View>(R.id.arrow)
        val rightView = findViewById<View>(R.id.iv_right_view)
        val actionMore = findViewById<View>(R.id.img_action_more)
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
            setStaticVisibility(arrow, visible = !enabled)
            setStaticVisibility(rightView, visible = enabled)
            setStaticVisibility(actionMore, visible = !enabled)
            requestLayout()
            return
        }

        animator?.cancel()
        checkbox.visibility = View.VISIBLE
        prepareTransitionVisibility(arrow, visibleAtStart = enabled)
        prepareTransitionVisibility(rightView, visibleAtStart = !enabled)
        prepareTransitionVisibility(actionMore, visibleAtStart = enabled)
        val animators = mutableListOf<Animator>(
            ValueAnimator.ofInt(params.leftMargin, if (enabled) shownMargin else hiddenMargin).apply {
                addUpdateListener { valueAnimator ->
                    params.leftMargin = valueAnimator.animatedValue as Int
                    checkbox.layoutParams = params
                    requestLayout()
                }
            },
        )
        arrow?.let { view ->
            animators += ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, if (enabled) 0f else 1f)
        }
        rightView?.let { view ->
            animators += ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, if (enabled) 1f else 0f)
        }
        actionMore?.let { view ->
            animators += ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, if (enabled) 0f else 1f)
        }
        val nextAnimator = AnimatorSet().apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            playTogether(animators)
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        params.leftMargin = if (enabled) shownMargin else hiddenMargin
                        checkbox.layoutParams = params
                        checkbox.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
                        setStaticVisibility(arrow, visible = !enabled)
                        setStaticVisibility(rightView, visible = enabled)
                        setStaticVisibility(actionMore, visible = !enabled)
                        requestLayout()
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

    private fun setStaticVisibility(view: View?, visible: Boolean) {
        view ?: return
        view.visibility = if (visible) View.VISIBLE else View.GONE
        view.alpha = if (visible) 1f else 0f
    }

    private fun prepareTransitionVisibility(view: View?, visibleAtStart: Boolean) {
        view ?: return
        if (visibleAtStart) {
            view.visibility = View.VISIBLE
            if (view.alpha == 0f) {
                view.alpha = 1f
            }
        } else {
            view.visibility = View.VISIBLE
            if (view.alpha == 1f) {
                view.alpha = 0f
            }
        }
    }
}

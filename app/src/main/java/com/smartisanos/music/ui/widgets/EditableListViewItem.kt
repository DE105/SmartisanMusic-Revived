package com.smartisanos.music.ui.widgets

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.CheckBox
import android.widget.ImageView
import com.smartisanos.music.R
import android.widget.RelativeLayout

class EditableListViewItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {
    private var editMode = false
    private var animator: Animator? = null

    fun bindLegacyEditState(
        enabled: Boolean,
        checked: Boolean,
        animate: Boolean,
    ) {
        val checkbox = findViewById<CheckBox>(R.id.cb_del) ?: return
        val content = findViewById<View>(R.id.relativeLayout1) ?: return
        val durationView = findViewById<View>(R.id.tv_duration)
        val more = findViewById<ImageView>(R.id.img_action_more)
        val offset = legacyCheckboxOffset(checkbox)
        val oldMode = editMode
        editMode = enabled
        checkbox.isChecked = checked
        checkbox.isClickable = false
        checkbox.isFocusable = false
        durationView?.visibility = if (enabled) View.GONE else View.VISIBLE
        more?.visibility = if (enabled) View.GONE else View.VISIBLE

        if (!animate || oldMode == enabled) {
            animator?.cancel()
            checkbox.visibility = if (enabled) View.VISIBLE else View.GONE
            checkbox.alpha = if (enabled) 1f else 0f
            checkbox.translationX = if (enabled) 0f else -offset
            content.translationX = if (enabled) offset else 0f
            return
        }

        animator?.cancel()
        checkbox.visibility = View.VISIBLE
        durationView?.visibility = View.GONE
        more?.visibility = View.GONE
        val nextAnimator = AnimatorSet().apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            playTogether(
                ObjectAnimator.ofFloat(checkbox, View.TRANSLATION_X, checkbox.translationX, if (enabled) 0f else -offset),
                ObjectAnimator.ofFloat(checkbox, View.ALPHA, checkbox.alpha, if (enabled) 1f else 0f),
                ObjectAnimator.ofFloat(content, View.TRANSLATION_X, content.translationX, if (enabled) offset else 0f),
            )
            addListener(
                object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        checkbox.visibility = if (enabled) View.VISIBLE else View.GONE
                        checkbox.alpha = if (enabled) 1f else 0f
                        checkbox.translationX = if (enabled) 0f else -offset
                        content.translationX = if (enabled) offset else 0f
                        durationView?.visibility = if (enabled) View.GONE else View.VISIBLE
                        more?.visibility = if (enabled) View.GONE else View.VISIBLE
                    }
                },
            )
        }
        animator = nextAnimator
        nextAnimator.start()
    }

    private fun legacyCheckboxOffset(checkbox: CheckBox): Float {
        if (checkbox.measuredWidth <= 0) {
            checkbox.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
        }
        val params = checkbox.layoutParams as? MarginLayoutParams
        return ((params?.leftMargin ?: 0) + checkbox.measuredWidth).toFloat()
    }
}

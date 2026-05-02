package smartisanos.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.smartisanos.music.R

class TitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {
    private val leftViews = mutableListOf<View>()
    private val rightViews = mutableListOf<View>()
    private val titleView: TextView
    private val shadowView: ImageView
    private var centerView: View? = null
    private var titleBarHeight = resources.getDimensionPixelSize(R.dimen.title_bar_height)
    private val imageViewSize = resources.getDimensionPixelSize(R.dimen.standard_icon_size)
    private val marginEdge = resources.getDimensionPixelSize(R.dimen.bar_margin_edge)
    private val marginView = resources.getDimensionPixelSize(R.dimen.title_bar_margin_view)

    init {
        setBackgroundColor(context.getColor(android.R.color.white))

        titleView = TextView(context).apply {
            id = View.generateViewId()
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            maxLines = 1
            setSingleLine(true)
            setTextColor(context.getColor(R.color.title_color))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_text_size))
            paint.isFakeBoldText = true
        }
        addView(
            titleView,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                addRule(CENTER_IN_PARENT)
            },
        )

        shadowView = ImageView(context).apply {
            id = View.generateViewId()
            setBackgroundResource(R.drawable.title_bar_shadow_standard)
        }
        addView(
            shadowView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.title_bar_shadow_height),
            ).apply {
                addRule(ALIGN_PARENT_BOTTOM)
            },
        )
    }

    fun setCenterText(text: String?) {
        clearCenterView()
        titleView.text = text.orEmpty()
    }

    fun setCenterText(resId: Int) {
        setCenterText(context.getString(resId))
    }

    fun setCenterTextColor(color: Int) {
        titleView.setTextColor(color)
    }

    fun getTitleView(): TextView = titleView

    fun setCenterView(view: View) {
        if (centerView === view) {
            return
        }
        clearCenterView()
        (view.parent as? ViewGroup)?.removeView(view)
        if (view.id == View.NO_ID) {
            view.id = View.generateViewId()
        }
        centerView = view
        titleView.visibility = View.GONE
        addView(
            view,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                addRule(CENTER_IN_PARENT)
            },
        )
    }

    fun getShadowView(): View = shadowView

    fun setShadowVisible(visible: Boolean) {
        shadowView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setTitleBarHeight(height: Int) {
        titleBarHeight = height
        requestLayout()
    }

    fun addLeftImageView(resId: Int): ImageView {
        return addLeftImageView(resId, -1)
    }

    fun addLeftImageView(resId: Int, index: Int): ImageView {
        val imageView = newIconView(resId)
        addSideView(imageView, leftViews, index, alignLeft = true)
        return imageView
    }

    fun addRightImageView(resId: Int): ImageView {
        return addRightImageView(resId, -1)
    }

    fun addRightImageView(resId: Int, index: Int): ImageView {
        val imageView = newIconView(resId)
        addSideView(imageView, rightViews, index, alignLeft = false)
        return imageView
    }

    fun addLeftView(view: View) {
        addSideView(view, leftViews, -1, alignLeft = true)
    }

    fun addRightView(view: View) {
        addSideView(view, rightViews, -1, alignLeft = false)
    }

    fun removeAllLeftViews() {
        removeSideViews(leftViews)
    }

    fun removeAllRightViews() {
        removeSideViews(rightViews)
    }

    fun getLeftViewCount(): Int = leftViews.count { it.visibility != View.GONE }

    fun getRightViewCount(): Int = rightViews.count { it.visibility != View.GONE }

    fun getLeftViewByIndex(index: Int): View? = leftViews.filter { it.visibility != View.GONE }.getOrNull(index)

    fun getRightViewByIndex(index: Int): View? = rightViews.filter { it.visibility != View.GONE }.getOrNull(index)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(titleBarHeight, MeasureSpec.EXACTLY),
        )
    }

    private fun newIconView(resId: Int): ImageView {
        return ImageView(context).apply {
            id = View.generateViewId()
            setImageResource(resId)
            scaleType = ImageView.ScaleType.CENTER
            isClickable = true
            isFocusable = true
        }
    }

    private fun clearCenterView() {
        centerView?.let { view ->
            removeView(view)
        }
        centerView = null
        titleView.visibility = View.VISIBLE
    }

    private fun addSideView(
        view: View,
        sideViews: MutableList<View>,
        index: Int,
        alignLeft: Boolean,
    ) {
        val insertIndex = index.takeIf { it in 0..sideViews.size } ?: sideViews.size
        if (view.id == View.NO_ID) {
            view.id = View.generateViewId()
        }
        sideViews.add(insertIndex, view)
        addView(view, sideLayoutParams())
        relayoutSideViews(sideViews, alignLeft)
    }

    private fun sideLayoutParams(): LayoutParams {
        return LayoutParams(imageViewSize, imageViewSize).apply {
            addRule(CENTER_VERTICAL)
        }
    }

    private fun relayoutSideViews(
        sideViews: List<View>,
        alignLeft: Boolean,
    ) {
        sideViews.forEachIndexed { index, view ->
            val params = (view.layoutParams as? LayoutParams) ?: sideLayoutParams()
            params.alignWithParent = true
            params.removeRule(ALIGN_PARENT_LEFT)
            params.removeRule(ALIGN_PARENT_RIGHT)
            params.removeRule(RIGHT_OF)
            params.removeRule(LEFT_OF)
            params.addRule(CENTER_VERTICAL)

            val visibleIndex = sideViews.take(index).count { it.visibility != View.GONE }
            if (alignLeft) {
                if (visibleIndex == 0) {
                    params.addRule(ALIGN_PARENT_LEFT)
                    params.leftMargin = marginEdge
                } else {
                    val previous = sideViews
                        .take(index)
                        .lastOrNull { it.visibility != View.GONE }
                    if (previous != null) {
                        params.addRule(RIGHT_OF, previous.id)
                    }
                    params.leftMargin = marginView
                }
            } else {
                if (visibleIndex == 0) {
                    params.addRule(ALIGN_PARENT_RIGHT)
                    params.rightMargin = marginEdge
                } else {
                    val previous = sideViews
                        .take(index)
                        .lastOrNull { it.visibility != View.GONE }
                    if (previous != null) {
                        params.addRule(LEFT_OF, previous.id)
                    }
                    params.rightMargin = marginView
                }
            }
            view.layoutParams = params
        }
    }

    private fun removeSideViews(sideViews: MutableList<View>) {
        sideViews.forEach { view ->
            removeView(view)
        }
        sideViews.clear()
    }
}

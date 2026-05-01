package smartisanos.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

private val SmartisanBlankAttributeNamespaces = arrayOf(
    "http://schemas.android.com/apk/res/com.smartisanos.music",
    "http://schemas.android.com/apk/res/smartisanos",
)

class SmartisanBlankView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER

        val drawableRes = attrs.resolveResource("emptyDrawable")
        if (drawableRes != 0) {
            addView(
                ImageView(context).apply {
                    setImageResource(drawableRes)
                },
            )
        }

        resolveText(attrs, "primaryHint")?.let { primary ->
            addView(
                TextView(context).apply {
                    text = primary
                    gravity = Gravity.CENTER
                    setTextColor(Color.rgb(0x99, 0x99, 0x99))
                    textSize = 17f
                },
            )
        }
        resolveText(attrs, "secondaryHint")?.let { secondary ->
            addView(
                TextView(context).apply {
                    text = secondary
                    gravity = Gravity.CENTER
                    setTextColor(Color.rgb(0xbb, 0xbb, 0xbb))
                    textSize = 13.5f
                },
            )
        }
    }

    private fun resolveText(attrs: AttributeSet?, name: String): CharSequence? {
        val resId = attrs.resolveResource(name)
        return if (resId != 0) {
            context.getText(resId)
        } else {
            attrs.resolveValue(name)
        }
    }
}

private fun AttributeSet?.resolveResource(name: String): Int {
    if (this == null) {
        return 0
    }
    SmartisanBlankAttributeNamespaces.forEach { namespace ->
        val value = getAttributeResourceValue(namespace, name, 0)
        if (value != 0) {
            return value
        }
    }
    return 0
}

private fun AttributeSet?.resolveValue(name: String): String? {
    if (this == null) {
        return null
    }
    SmartisanBlankAttributeNamespaces.forEach { namespace ->
        getAttributeValue(namespace, name)?.let { value ->
            return value
        }
    }
    return null
}

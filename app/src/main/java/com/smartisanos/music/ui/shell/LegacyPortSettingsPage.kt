package com.smartisanos.music.ui.shell

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.viewinterop.AndroidView
import com.smartisanos.music.R
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.ui.shell.titlebar.LegacyPortSmartisanTitleBar
import smartisanos.widget.SwitchEx
import smartisanos.widget.TitleBar

@Composable
internal fun LegacyPortSettingsPage(
    active: Boolean,
    playbackSettings: PlaybackSettings,
    onClose: () -> Unit,
    onScratchEnabledChange: (Boolean) -> Unit,
    onHidePlayerAxisEnabledChange: (Boolean) -> Unit,
    onPopcornSoundEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = active) {
        onClose()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor.White),
    ) {
        LegacyPortSmartisanTitleBar(modifier = Modifier.fillMaxWidth()) { titleBar ->
            titleBar.setupLegacySettingsTitleBar(onClose = onClose)
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { context ->
                LegacySettingsContentView(context)
            },
            update = { view ->
                view.visibility = if (active) View.VISIBLE else View.INVISIBLE
                view.bind(
                    settings = playbackSettings,
                    onScratchEnabledChange = onScratchEnabledChange,
                    onHidePlayerAxisEnabledChange = onHidePlayerAxisEnabledChange,
                    onPopcornSoundEnabledChange = onPopcornSoundEnabledChange,
                )
            },
        )
    }
}

private fun TitleBar.setupLegacySettingsTitleBar(
    onClose: () -> Unit,
) {
    removeAllLeftViews()
    removeAllRightViews()
    setShadowVisible(false)
    setCenterText(R.string.setting)
    addLeftImageView(R.drawable.standard_icon_back_selector).apply {
        setOnClickListener {
            onClose()
        }
    }
}

private class LegacySettingsContentView(context: Context) : ScrollView(context) {
    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    private val scratchRow = LegacySettingsSwitchRow(context, R.string.djing)
    private val axisRow = LegacySettingsSwitchRow(context, R.string.player_axis_and_needle_enabled)
    private val popcornRow = LegacySettingsSwitchRow(context, R.string.popcorn_sound)

    init {
        setBackgroundResource(R.drawable.account_background)
        isFillViewport = true
        isVerticalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_ALWAYS
        addView(
            content,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ),
        )
        content.addView(gapView(context))
        content.addView(scratchRow, rowLayoutParams(context))
        content.addView(gapView(context))
        content.addView(axisRow, rowLayoutParams(context))
        content.addView(gapView(context))
        content.addView(popcornRow, rowLayoutParams(context))
    }

    fun bind(
        settings: PlaybackSettings,
        onScratchEnabledChange: (Boolean) -> Unit,
        onHidePlayerAxisEnabledChange: (Boolean) -> Unit,
        onPopcornSoundEnabledChange: (Boolean) -> Unit,
    ) {
        scratchRow.bind(settings.scratchEnabled, onScratchEnabledChange)
        axisRow.bind(settings.hidePlayerAxisEnabled, onHidePlayerAxisEnabledChange)
        popcornRow.bind(settings.popcornSoundEnabled, onPopcornSoundEnabledChange)
    }

    private fun gapView(context: Context): View {
        return View(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.list_item_vertical_gap),
            )
        }
    }

    private fun rowLayoutParams(context: Context): LinearLayout.LayoutParams {
        val margin = context.resources.getDimensionPixelSize(R.dimen.list_item_left_right_margin)
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            context.resources.getDimensionPixelSize(R.dimen.list_item_min_height),
        ).apply {
            leftMargin = margin
            rightMargin = margin
        }
    }
}

private class LegacySettingsSwitchRow(
    context: Context,
    titleRes: Int,
) : RelativeLayout(context) {
    private var binding = false
    private var onCheckedChange: ((Boolean) -> Unit)? = null
    private val titleView = TextView(context).apply {
        id = View.generateViewId()
        gravity = Gravity.CENTER_VERTICAL
        setSingleLine(true)
        setText(titleRes)
        setTextColor(context.getColor(R.color.setting_item_text_color))
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.primary_text_size))
    }
    private val switchView = SwitchEx(context).apply {
        id = View.generateViewId()
        setDuplicateParentStateEnabled(true)
        setOnCheckedChangeListener { _, checked ->
            if (!binding) {
                onCheckedChange?.invoke(checked)
            }
        }
    }

    init {
        setBackgroundResource(R.drawable.group_list_item_bg_single)
        isClickable = true
        isFocusable = true
        addView(
            switchView,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                addRule(ALIGN_PARENT_RIGHT)
                addRule(CENTER_VERTICAL)
                rightMargin = dp(18)
            },
        )
        addView(
            titleView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                addRule(CENTER_VERTICAL)
                addRule(LEFT_OF, switchView.id)
                leftMargin = dp(18)
                rightMargin = dp(12)
            },
        )
        setOnClickListener {
            switchView.performClick()
        }
    }

    fun bind(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        this.onCheckedChange = onCheckedChange
        if (switchView.isChecked != checked) {
            binding = true
            switchView.isChecked = checked
            binding = false
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }
}

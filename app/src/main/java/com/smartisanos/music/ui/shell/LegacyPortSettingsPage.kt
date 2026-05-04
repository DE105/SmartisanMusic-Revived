package com.smartisanos.music.ui.shell

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.smartisanos.music.R
import com.smartisanos.music.data.settings.ArtistSettings
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.data.settings.parseArtistSeparatorInput
import com.smartisanos.music.ui.shell.titlebar.LegacyPortSmartisanTitleBar
import smartisanos.widget.SwitchEx
import smartisanos.widget.TitleBar

@Composable
internal fun LegacyPortSettingsPage(
    active: Boolean,
    playbackSettings: PlaybackSettings,
    artistSettings: ArtistSettings,
    onClose: () -> Unit,
    onScratchEnabledChange: (Boolean) -> Unit,
    onHidePlayerAxisEnabledChange: (Boolean) -> Unit,
    onPopcornSoundEnabledChange: (Boolean) -> Unit,
    onArtistSeparatorsChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var editingArtistSeparators by remember { mutableStateOf(false) }
    var artistSeparatorsInitialValues by remember { mutableStateOf(emptySet<String>()) }
    val latestOnArtistSeparatorsChange by rememberUpdatedState(onArtistSeparatorsChange)

    BackHandler(enabled = active) {
        onClose()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor.White),
    ) {
        LegacyPortSmartisanTitleBar(
            modifier = Modifier.fillMaxWidth(),
            showShadow = true,
        ) { titleBar ->
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
                    artistSettings = artistSettings,
                    onScratchEnabledChange = onScratchEnabledChange,
                    onHidePlayerAxisEnabledChange = onHidePlayerAxisEnabledChange,
                    onPopcornSoundEnabledChange = onPopcornSoundEnabledChange,
                    onArtistSeparatorsClick = {
                        artistSeparatorsInitialValues = artistSettings.separators
                        editingArtistSeparators = true
                    },
                )
            },
        )
    }

    if (editingArtistSeparators) {
        DisposableEffect(artistSeparatorsInitialValues) {
            val dialog = LegacyArtistSeparatorsDialog(
                context = context,
                initialSeparators = artistSeparatorsInitialValues,
                onDismiss = {
                    editingArtistSeparators = false
                },
                onConfirm = { separators ->
                    editingArtistSeparators = false
                    latestOnArtistSeparatorsChange(separators)
                },
            )
            dialog.show()
            onDispose {
                dialog.dismissIfShowing()
            }
        }
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
    private val axisRow = LegacySettingsSwitchRow(context, R.string.player_axis_enabled)
    private val popcornRow = LegacySettingsSwitchRow(context, R.string.popcorn_sound)
    private val artistSeparatorsRow = LegacySettingsValueRow(context, R.string.artist_separators)

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
        content.addView(gapView(context))
        content.addView(artistSeparatorsRow, rowLayoutParams(context))
    }

    fun bind(
        settings: PlaybackSettings,
        artistSettings: ArtistSettings,
        onScratchEnabledChange: (Boolean) -> Unit,
        onHidePlayerAxisEnabledChange: (Boolean) -> Unit,
        onPopcornSoundEnabledChange: (Boolean) -> Unit,
        onArtistSeparatorsClick: () -> Unit,
    ) {
        scratchRow.bind(settings.scratchEnabled, onScratchEnabledChange)
        axisRow.bind(settings.hidePlayerAxisEnabled, onHidePlayerAxisEnabledChange)
        popcornRow.bind(settings.popcornSoundEnabled, onPopcornSoundEnabledChange)
        artistSeparatorsRow.bind(
            value = artistSettings.separators.toSeparatorSummary(context),
            onClick = onArtistSeparatorsClick,
        )
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

private class LegacySettingsValueRow(
    context: Context,
    titleRes: Int,
) : RelativeLayout(context) {
    private val titleView = TextView(context).apply {
        id = View.generateViewId()
        gravity = Gravity.CENTER_VERTICAL
        setSingleLine(true)
        setText(titleRes)
        setTextColor(context.getColor(R.color.setting_item_text_color))
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.primary_text_size))
    }
    private val valueView = TextView(context).apply {
        id = View.generateViewId()
        gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        setSingleLine(true)
        setTextColor(context.getColor(R.color.setting_item_summary_text_color))
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.settings_item_tips_text_size))
    }

    init {
        setBackgroundResource(R.drawable.group_list_item_bg_single)
        isClickable = true
        isFocusable = true
        addView(
            valueView,
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
                addRule(LEFT_OF, valueView.id)
                leftMargin = dp(18)
                rightMargin = dp(12)
            },
        )
    }

    fun bind(
        value: String,
        onClick: () -> Unit,
    ) {
        valueView.text = value
        setOnClickListener { onClick() }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
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

private class LegacyArtistSeparatorsDialog(
    private val context: Context,
    initialSeparators: Set<String>,
    private val onDismiss: () -> Unit,
    private val onConfirm: (Set<String>) -> Unit,
) {
    private val dialog = Dialog(context)
    private val chipRow: LinearLayout
    private val input: EditText
    private var separators = initialSeparators.toCollection(linkedSetOf())

    init {
        val density = context.resources.displayMetrics.density
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 10f * density
            }
        }
        dialog.requestWindowFeature(1)
        dialog.setContentView(root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener {
            onDismiss()
        }

        root.addView(
            TextView(context).apply {
                text = context.getString(R.string.artist_separators)
                gravity = Gravity.CENTER
                setTextColor(context.getColor(R.color.title_color))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpPx(53)),
        )

        root.addView(
            TextView(context).apply {
                text = context.getString(R.string.artist_separators_hint)
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(0x99, 0x99, 0x99))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.DEFAULT_BOLD
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpPx(34)),
        )

        chipRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(
            HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                addView(
                    chipRow,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpPx(50)).apply {
                leftMargin = context.dpPx(18)
                rightMargin = context.dpPx(18)
                bottomMargin = context.dpPx(6)
            },
        )

        val addRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        input = EditText(context).apply {
            setSingleLine(true)
            hint = context.getString(R.string.artist_custom_separator_hint)
            setTextColor(PlaylistPrimaryTextColor)
            setHintTextColor(Color.rgb(0x99, 0x99, 0x99))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            background = null
        }
        val inputFrame = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpPx(12), 0, context.dpPx(8), 0)
            background = GradientDrawable().apply {
                setColor(Color.rgb(0xf7, 0xf8, 0xf9))
                setStroke(context.dpPx(1), Color.rgb(0xe2, 0xe2, 0xe2))
                cornerRadius = 6f * density
            }
            addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            addView(
                ImageView(context).apply {
                    setImageResource(R.drawable.clear_text)
                    setOnClickListener {
                        input.text = null
                    }
                },
                LinearLayout.LayoutParams(context.dpPx(32), LinearLayout.LayoutParams.MATCH_PARENT),
            )
        }
        addRow.addView(inputFrame, LinearLayout.LayoutParams(0, context.dpPx(44), 1f))
        addRow.addView(
            dialogButton(
                text = context.getString(R.string.add),
                textColor = context.getColor(R.color.btn_text_color_blue),
                backgroundColor = Color.rgb(0xee, 0xf4, 0xff),
                strokeColor = Color.rgb(0xa9, 0xc1, 0xf5),
            ).apply {
                setOnClickListener {
                    addSeparatorsFromInput()
                }
            },
            LinearLayout.LayoutParams(context.dpPx(78), context.dpPx(40)).apply {
                leftMargin = context.dpPx(10)
            },
        )
        root.addView(
            addRow,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpPx(44)).apply {
                leftMargin = context.dpPx(18)
                rightMargin = context.dpPx(18)
                bottomMargin = context.dpPx(24)
            },
        )

        root.addView(
            dialogButton(
                text = context.getString(R.string.done),
                textColor = Color.WHITE,
                backgroundColor = context.getColor(R.color.btn_text_color_blue),
                strokeColor = darken(context.getColor(R.color.btn_text_color_blue)),
            ).apply {
                setOnClickListener {
                    addSeparatorsFromInput()
                    onConfirm(separators)
                }
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpPx(44)).apply {
                leftMargin = context.dpPx(18)
                rightMargin = context.dpPx(18)
                bottomMargin = context.dpPx(18)
            },
        )

        renderChips()
    }

    fun show() {
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.54f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setLayout(context.dpPx(308), WindowManager.LayoutParams.WRAP_CONTENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        input.postDelayed(
            {
                input.requestFocus()
                (dialog.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            },
            300L,
        )
    }

    fun dismissIfShowing() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    private fun addSeparatorsFromInput() {
        val addedSeparators = parseArtistSeparatorInput(input.text?.toString().orEmpty())
        if (addedSeparators.isNotEmpty()) {
            separators += addedSeparators
            input.text = null
            renderChips()
        }
    }

    private fun renderChips() {
        chipRow.removeAllViews()
        separators.sorted().forEach { separator ->
            chipRow.addView(
                separatorChip(separator),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    context.dpPx(36),
                ).apply {
                    rightMargin = context.dpPx(8)
                },
            )
        }
        if (separators.isEmpty()) {
            chipRow.addView(
                TextView(context).apply {
                    text = context.getString(R.string.not_set)
                    gravity = Gravity.CENTER_VERTICAL
                    setTextColor(Color.rgb(0x99, 0x99, 0x99))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
    }

    private fun separatorChip(separator: String): TextView {
        return TextView(context).apply {
            text = "$separator  \u00D7"
            gravity = Gravity.CENTER
            setPadding(context.dpPx(16), 0, context.dpPx(14), 0)
            setTextColor(Color.rgb(0x99, 0x99, 0x99))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.rgb(0xf7, 0xf8, 0xf9))
                setStroke(context.dpPx(1), Color.rgb(0xe2, 0xe2, 0xe2))
                cornerRadius = 18f * context.resources.displayMetrics.density
            }
            setOnClickListener {
                separators -= separator
                renderChips()
            }
        }
    }

    private fun dialogButton(
        text: String,
        textColor: Int,
        backgroundColor: Int,
        strokeColor: Int,
    ): TextView {
        return TextView(context).apply {
            gravity = Gravity.CENTER
            this.text = text
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT
            isClickable = true
            isFocusable = true
            background = GradientDrawable().apply {
                setColor(backgroundColor)
                setStroke(context.dpPx(1), strokeColor)
                cornerRadius = 6f * context.resources.displayMetrics.density
            }
        }
    }

    private fun darken(color: Int): Int {
        return Color.rgb(
            (Color.red(color) * 0.85f).toInt(),
            (Color.green(color) * 0.85f).toInt(),
            (Color.blue(color) * 0.85f).toInt(),
        )
    }
}

private fun Set<String>.toSeparatorSummary(context: Context): String {
    return if (isEmpty()) {
        context.getString(R.string.not_set)
    } else {
        sorted().joinToString(" ")
    }
}

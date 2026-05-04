package com.smartisanos.music.ui.shell

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.smartisanos.music.R
import smartisanos.app.MenuDialog

@Composable
internal fun LegacyPlaylistNameDialogOverlay(
    request: LegacyPlaylistNameDialogRequest?,
    onDismiss: () -> Unit,
    onConfirm: (LegacyPlaylistNameDialogRequest, String) -> Unit,
) {
    val context = LocalContext.current
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestOnConfirm by rememberUpdatedState(onConfirm)
    DisposableEffect(request) {
        val activeRequest = request ?: return@DisposableEffect onDispose { }
        val dialog = LegacyPlaylistNameDialog(
            context = context,
            title = context.getString(
                when (activeRequest) {
                    is LegacyPlaylistNameDialogRequest.Create -> R.string.new_playlist
                    is LegacyPlaylistNameDialogRequest.Rename -> R.string.playlist_rename_title
                },
            ),
            initialName = activeRequest.initialName,
            confirmText = context.getString(
                when (activeRequest) {
                    is LegacyPlaylistNameDialogRequest.Create -> R.string.rename_continue
                    is LegacyPlaylistNameDialogRequest.Rename -> R.string.save
                },
            ),
            onDismiss = latestOnDismiss,
            onConfirm = { name ->
                latestOnConfirm(activeRequest, name)
            },
        )
        dialog.show()
        onDispose {
            dialog.dismissIfShowing()
        }
    }
}

@Composable
internal fun LegacyPlaylistDeleteDialog(
    request: LegacyPlaylistDeleteRequest?,
    onDismiss: () -> Unit,
    onConfirm: (LegacyPlaylistDeleteRequest) -> Unit,
) {
    val context = LocalContext.current
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestOnConfirm by rememberUpdatedState(onConfirm)
    DisposableEffect(request) {
        val activeRequest = request ?: return@DisposableEffect onDispose { }
        val dialog = MenuDialog(context).apply {
            setTitle(
                when (activeRequest) {
                    LegacyPlaylistDeleteRequest.RootSelected -> R.string.playlist_delete_confirm
                    LegacyPlaylistDeleteRequest.DetailPlaylist -> R.string.playlist_delete_single_confirm
                    LegacyPlaylistDeleteRequest.DetailTracks -> R.string.playlist_remove_song_confirm
                },
            )
            setPositiveButton(R.string.dialog_delete_conform) {
                latestOnConfirm(activeRequest)
            }
            setNegativeButton(
                View.OnClickListener {
                    latestOnDismiss()
                },
            )
            setOnCancelListener {
                latestOnDismiss()
            }
        }
        dialog.show()
        onDispose {
            dialog.setOnCancelListener(null)
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }
    }
}

private class LegacyPlaylistNameDialog(
    private val context: Context,
    title: String,
    initialName: String,
    confirmText: String,
    private val onDismiss: () -> Unit,
    private val onConfirm: (String) -> Unit,
) {
    private val dialog = Dialog(context)
    private val editText: EditText
    private val confirmButton: TextView

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
                text = title
                gravity = Gravity.CENTER
                setTextColor(context.getColor(R.color.title_color))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpPx(53)),
        )
        root.addView(divider(context), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpPx(1)))

        val editFrame = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpPx(12), 0, context.dpPx(8), 0)
            background = GradientDrawable().apply {
                setColor(Color.rgb(0xf7, 0xf8, 0xf9))
                setStroke(context.dpPx(1), Color.rgb(0xe2, 0xe2, 0xe2))
                cornerRadius = 6f * density
            }
        }
        editText = EditText(context).apply {
            setSingleLine(true)
            setText(initialName)
            selectAll()
            setTextColor(PlaylistPrimaryTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            background = null
        }
        editFrame.addView(editText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        editFrame.addView(
            ImageView(context).apply {
                setImageResource(R.drawable.clear_text)
                setOnClickListener {
                    editText.text = null
                }
            },
            LinearLayout.LayoutParams(context.dpPx(32), LinearLayout.LayoutParams.MATCH_PARENT),
        )
        root.addView(
            editFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpPx(44)).apply {
                leftMargin = context.dpPx(18)
                rightMargin = context.dpPx(18)
                topMargin = context.dpPx(18)
                bottomMargin = context.dpPx(18)
            },
        )
        root.addView(divider(context), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpPx(1)))

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val cancelButton = dialogButton(context, context.getString(android.R.string.cancel), Color.rgb(0x99, 0x99, 0x99)).apply {
            setOnClickListener {
                dialog.dismiss()
                onDismiss()
            }
        }
        confirmButton = dialogButton(context, confirmText, context.getColor(R.color.btn_text_color_blue)).apply {
            setOnClickListener {
                onConfirm(editText.text.toString())
            }
        }
        buttons.addView(cancelButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        buttons.addView(divider(context), LinearLayout.LayoutParams(context.dpPx(1), LinearLayout.LayoutParams.MATCH_PARENT))
        buttons.addView(confirmButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        root.addView(buttons, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpPx(53)))

        editText.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    confirmButton.isEnabled = !s.isNullOrBlank()
                    confirmButton.alpha = if (confirmButton.isEnabled) 1f else 0.35f
                }
                override fun afterTextChanged(s: Editable?) = Unit
            },
        )
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
        editText.postDelayed(
            {
                editText.requestFocus()
                (dialog.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            },
            300L,
        )
    }

    fun dismissIfShowing() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    private fun divider(context: Context): View {
        return View(context).apply {
            setBackgroundColor(Color.rgb(0xe8, 0xe8, 0xe8))
        }
    }

    private fun dialogButton(context: Context, text: String, color: Int): TextView {
        return TextView(context).apply {
            gravity = Gravity.CENTER
            this.text = text
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT
            isClickable = true
            isFocusable = true
        }
    }
}

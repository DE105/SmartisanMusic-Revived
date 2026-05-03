package com.smartisanos.music.ui.shell.dialogs

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.smartisanos.music.R
import smartisanos.app.MenuDialog

@Composable
internal fun LegacySongDeleteConfirmOverlay(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestOnConfirm by rememberUpdatedState(onConfirm)
    DisposableEffect(context) {
        val dialog = MenuDialog(context).apply {
            setTitle(R.string.delete_song_title_text)
            setNegativeButton(
                View.OnClickListener {
                    latestOnDismiss()
                },
            )
            setPositiveButton(R.string.dialog_delete_conform) {
                latestOnConfirm()
            }
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

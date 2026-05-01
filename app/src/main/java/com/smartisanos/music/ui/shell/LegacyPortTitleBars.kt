package com.smartisanos.music.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.viewinterop.AndroidView
import com.smartisanos.music.R
import smartisanos.widget.TitleBar

@Composable
internal fun LegacyPortSmartisanTitleBar(
    modifier: Modifier = Modifier,
    includeStatusBar: Boolean = true,
    update: (TitleBar) -> Unit,
) {
    val titleContentHeight = dimensionResource(R.dimen.title_bar_height)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ComposeColor.White),
    ) {
        if (includeStatusBar) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars),
            )
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(titleContentHeight),
            factory = { context ->
                TitleBar(context).apply {
                    setShadowVisible(false)
                }
            },
            update = { titleBar ->
                titleBar.setShadowVisible(false)
                update(titleBar)
            },
        )
    }
}

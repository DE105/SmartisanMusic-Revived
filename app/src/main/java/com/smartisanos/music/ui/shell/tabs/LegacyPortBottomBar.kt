package com.smartisanos.music.ui.shell.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.smartisanos.music.ui.navigation.MusicDestination
import smartisanos.widget.tabswitcher.TabSwitcher

@Composable
internal fun LegacyPortBottomBar(
    currentDestination: MusicDestination,
    onDestinationSelected: (MusicDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            factory = { viewContext ->
                TabSwitcher(viewContext)
            },
            update = { tabSwitcher ->
                tabSwitcher.setOnDestinationSelectedListener(onDestinationSelected)
                tabSwitcher.setCurrentDestination(currentDestination)
            },
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars),
        )
    }
}

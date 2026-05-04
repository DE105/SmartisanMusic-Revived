package com.smartisanos.music.ui.shell

import android.view.View

internal interface LegacySwipeDeleteRow {
    val legacyRemoveItemView: View
    val legacySlideLayout: View
    val legacyDeleteView: View
    val legacyLeftMargin: Int

    fun resetLegacySwipeDelete()
}

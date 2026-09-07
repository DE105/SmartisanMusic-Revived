package com.smartisan.music.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.smartisan.music.R
import com.smartisan.music.ui.components.SmartisanTitleBar
import com.smartisan.music.ui.components.SmartisanTitleBarAction
import com.smartisan.music.ui.shell.titlebar.TitleBarShadow
import com.smartisan.music.ui.shell.titlebar.TitleBarTransition

@Composable
internal fun SettingsTitleStack(
    page: SettingsSecondaryPage?,
    onClose: () -> Unit,
    onBack: () -> Unit,
) {
    val shadowHeight = dimensionResource(R.dimen.title_bar_shadow_height)
    Box(Modifier.fillMaxSize()) {
        TitleBarTransition(
            page,
            Modifier.fillMaxSize(),
            primaryContent = {
                SmartisanTitleBar(
                    stringResource(R.string.setting),
                    showShadow = false,
                    action =
                        SmartisanTitleBarAction(
                            R.drawable.standard_icon_complete_selector,
                            stringResource(R.string.done),
                            onClose,
                        ),
                )
            },
            secondaryContent = { target ->
                SmartisanTitleBar(
                    stringResource(
                        when (target) {
                            SettingsSecondaryPage.AudioFx -> R.string.audio_fx
                            SettingsSecondaryPage.Navigation -> R.string.bottom_tab_visibility
                            SettingsSecondaryPage.Theme -> R.string.theme_settings
                            SettingsSecondaryPage.AppIcon -> R.string.app_icon
                        }
                    ),
                    navigationIcon =
                        SmartisanTitleBarAction(
                            R.drawable.standard_icon_back_selector,
                            stringResource(R.string.back),
                            onBack,
                        ),
                    showShadow = false,
                )
            },
        )
        // The title transition clips moving icons; its shadow must live outside those bounds.
        TitleBarShadow(
            Modifier.align(Alignment.BottomCenter)
                .offset(y = shadowHeight)
                .fillMaxWidth()
                .height(shadowHeight)
        )
    }
}

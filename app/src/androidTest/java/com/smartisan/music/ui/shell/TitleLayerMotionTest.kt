package com.smartisan.music.ui.shell

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartisan.music.R
import com.smartisan.music.ui.components.SmartisanTitleBar
import com.smartisan.music.ui.components.SmartisanTitleBarAction
import com.smartisan.music.ui.shell.titlebar.TitleBarTransition
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TitleLayerMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun notesStyleTitleRestoresRootActionsAfterReturn() {
        val key = mutableStateOf<String?>(null)
        var rootClicks = 0
        compose.setContent {
            TitleBarTransition(
                key.value,
                Modifier.size(240.dp, 50.dp),
                primaryContent = {
                    SmartisanTitleBar(
                        "Root",
                        includeStatusBar = false,
                        navigationIcon =
                            SmartisanTitleBarAction(
                                R.drawable.standard_icon_settings_selector,
                                "Settings",
                                {
                                    rootClicks++
                                    key.value = "detail"
                                },
                            ),
                    )
                },
                secondaryContent = {
                    SmartisanTitleBar(
                        "Detail",
                        includeStatusBar = false,
                        navigationIcon =
                            SmartisanTitleBarAction(
                                R.drawable.standard_icon_back_selector,
                                "Back",
                                { key.value = null },
                            ),
                    )
                },
            )
        }
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Detail").assertDoesNotExist()
        compose.onNodeWithContentDescription("Back").assertDoesNotExist()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.runOnIdle { assertEquals(2, rootClicks) }
    }
}

package com.smartisan.music.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartisan.music.R
import com.smartisan.music.ui.components.SmartisanTitleBar
import com.smartisan.music.ui.components.SmartisanTitleBarAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmartisanPageStackTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val key = mutableStateOf<String?>(null)

    private fun show() {
        compose.setContent {
            BackHandler(key.value != null) { key.value = null }
            SmartisanPageStack(
                key.value,
                primaryTitle = {
                    SmartisanTitleBar(
                        "Root",
                        navigationIcon =
                            SmartisanTitleBarAction(
                                R.drawable.standard_icon_settings_selector,
                                "Open",
                                { key.value = "detail" },
                            ),
                    )
                },
                secondaryTitle = {
                    SmartisanTitleBar(
                        "Detail",
                        navigationIcon =
                            SmartisanTitleBarAction(
                                R.drawable.standard_icon_back_selector,
                                "Back",
                                { key.value = null },
                            ),
                    )
                },
                primaryContent = { BasicText("Root body") },
                secondaryContent = {
                    Column(Modifier.fillMaxSize()) {
                        SmartisanTitleBar("Nested duplicate")
                        BasicText("Detail body")
                    }
                },
                modifier = Modifier.size(360.dp, 600.dp),
            )
        }
    }

    @Test
    fun slotHostShowsOneTitleAndReturnsUsingItsButton() {
        show()
        compose.onNodeWithContentDescription("Open").performClick()
        compose.onAllNodesWithText("Detail").assertCountEquals(1)
        compose.onNodeWithText("Nested duplicate").assertDoesNotExist()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Detail body").assertDoesNotExist()
        compose.onAllNodesWithText("Root").assertCountEquals(1)
    }

    @Test
    fun systemBackAndTitleBackHaveTheSameFinalDestination() {
        show()
        compose.onNodeWithContentDescription("Open").performClick()
        compose.waitForIdle()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Detail body").assertDoesNotExist()
        compose.onAllNodesWithText("Root").assertCountEquals(1)
    }
}

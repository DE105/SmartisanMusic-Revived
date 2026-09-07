package com.smartisan.music.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartisan.music.ui.components.SmartisanTitleBar
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectedTitleLifetimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun fullScreenExitDoesNotDetachAndReregisterTheRootTitle() {
        val inline = mutableStateOf(false)
        val outlet = ProjectedNavigationTitle()
        compose.setContent {
            Box(Modifier.size(240.dp, 80.dp)) {
                CompositionLocalProvider(
                    LocalProjectedNavigationTitle provides outlet,
                    LocalNavigationTitleInline provides inline.value,
                ) {
                    SmartisanTitleBar("More", includeStatusBar = false)
                }
            }
        }
        val original = compose.runOnIdle { outlet.content }
        assertNotNull(original)
        repeat(3) {
            compose.runOnIdle { inline.value = true }
            compose.runOnIdle { assertSame(original, outlet.content) }
            compose.runOnIdle { inline.value = false }
            compose.runOnIdle { assertSame(original, outlet.content) }
        }
    }
}

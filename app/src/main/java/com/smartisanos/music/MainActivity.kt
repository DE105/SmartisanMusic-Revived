package com.smartisanos.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smartisanos.music.ui.shell.MusicApp
import com.smartisanos.music.ui.theme.MusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicTheme(darkTheme = false, dynamicColor = false) {
                MusicApp()
            }
        }
    }
}

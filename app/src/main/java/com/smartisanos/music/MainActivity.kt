package com.smartisanos.music

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.smartisanos.music.ui.shell.MusicApp
import com.smartisanos.music.ui.theme.MusicTheme

class MainActivity : ComponentActivity() {
    private var playbackLaunchRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeLaunchIntent(intent)
        enableEdgeToEdge()
        setContent {
            MusicTheme(darkTheme = false, dynamicColor = false) {
                RequestAudioPermissionOnLaunch()
                MusicApp(playbackLaunchRequest = playbackLaunchRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchIntent(intent)
    }

    private fun consumeLaunchIntent(intent: Intent?) {
        val launchIntent = intent ?: return
        if (isPlaybackLaunchIntent(launchIntent) && !isConsumedPlaybackLaunchIntent(launchIntent)) {
            playbackLaunchRequest += 1
            launchIntent.putExtra(ExtraOpenPlaybackConsumed, true)
        }
    }

    companion object {
        private const val ActionOpenPlayback = "com.smartisanos.music.action.OPEN_PLAYBACK"
        private const val ExtraOpenPlayback = "com.smartisanos.music.extra.OPEN_PLAYBACK"
        private const val ExtraOpenPlaybackConsumed = "com.smartisanos.music.extra.OPEN_PLAYBACK_CONSUMED"

        fun createOpenPlaybackIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                action = ActionOpenPlayback
                putExtra(ExtraOpenPlayback, true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }

        private fun isPlaybackLaunchIntent(intent: Intent?): Boolean {
            return intent?.action == ActionOpenPlayback ||
                intent?.getBooleanExtra(ExtraOpenPlayback, false) == true
        }

        private fun isConsumedPlaybackLaunchIntent(intent: Intent?): Boolean {
            return intent?.getBooleanExtra(ExtraOpenPlaybackConsumed, false) == true
        }
    }
}

@Composable
private fun RequestAudioPermissionOnLaunch() {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(permission) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        }
    }
}

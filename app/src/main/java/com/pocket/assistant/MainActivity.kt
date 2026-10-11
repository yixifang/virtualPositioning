package com.pocket.assistant

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import com.pocket.assistant.ui.AppRoot
import com.pocket.assistant.ui.theme.PocketAssistantTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

        setContent {
            PocketAssistantTheme {
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                AppRoot()
            }
        }
    }
}

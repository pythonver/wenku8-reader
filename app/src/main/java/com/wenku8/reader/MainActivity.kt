package com.wenku8.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.wenku8.reader.core.designsystem.theme.WenkuTheme
import com.wenku8.reader.feature.update.UpdateHost
import com.wenku8.reader.feature.update.UpdateViewModel
import com.wenku8.reader.ui.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Belt and suspenders: guarantee the decor does not consume the system bars,
        // so Compose receives real WindowInsets (otherwise insets read as 0).
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            WenkuTheme {
                // Activity-scoped: survives navigation/rotation, init auto-check runs once.
                val updateViewModel: UpdateViewModel = hiltViewModel()
                AppNavigation(updateViewModel)
                UpdateHost(updateViewModel)
            }
        }
    }
}

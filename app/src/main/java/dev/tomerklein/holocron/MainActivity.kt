package dev.tomerklein.holocron

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.tomerklein.holocron.ui.HolocronApp
import dev.tomerklein.holocron.ui.theme.HolocronTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HolocronTheme {
                HolocronApp()
            }
        }
    }
}

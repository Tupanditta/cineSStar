package com.cinesstar.cinetrack

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cinesstar.cinetrack.navigation.CineNavHost
import com.cinesstar.cinetrack.ui.theme.CineTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Barra de estado real, transparente y con iconos claros (tema solo oscuro, design.md §3.1).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            CineTrackTheme {
                CineNavHost()
            }
        }
    }
}

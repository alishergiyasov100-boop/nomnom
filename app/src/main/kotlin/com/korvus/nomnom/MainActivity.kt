package com.korvus.nomnom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.korvus.nomnom.ui.CaptureFlowScreen
import com.korvus.nomnom.ui.HistoryScreen
import com.korvus.nomnom.ui.HomeScreen
import com.korvus.nomnom.ui.SettingsScreen
import com.korvus.nomnom.ui.theme.NomNomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NomNomTheme { Surface(modifier = Modifier.fillMaxSize()) { App() } }
        }
    }
}

@Composable
private fun App() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home")    { HomeScreen(nav) }
        composable("capture") { CaptureFlowScreen(nav) }
        composable("history") { HistoryScreen(nav) }
        composable("settings") { SettingsScreen(nav) }
    }
}

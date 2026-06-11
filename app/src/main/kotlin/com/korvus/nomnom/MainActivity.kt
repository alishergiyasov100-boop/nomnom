package com.korvus.nomnom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.korvus.nomnom.ui.CaptureFlowScreen
import com.korvus.nomnom.ui.ChefChatScreen
import com.korvus.nomnom.ui.HistoryScreen
import com.korvus.nomnom.ui.HomeScreen
import com.korvus.nomnom.ui.RemindersScreen
import com.korvus.nomnom.ui.SettingsScreen
import com.korvus.nomnom.ui.theme.NomNomTheme
import com.korvus.nomnom.ui.theme.VioletDeep
import com.korvus.nomnom.ui.theme.VioletPrimary

private data class TabSpec(
    val route: String,
    val label: String,
    val outlined: ImageVector,
    val filled: ImageVector,
)

private val TABS_LEFT = listOf(
    TabSpec("home",    "Дневник",    Icons.Outlined.NoteAlt, Icons.Rounded.NoteAlt),
    TabSpec("history", "Статистика", Icons.Outlined.BarChart, Icons.Rounded.BarChart),
)
private val TABS_RIGHT = listOf(
    TabSpec("chef",     "Шеф", Icons.Outlined.ChatBubbleOutline, Icons.Rounded.ChatBubble),
    TabSpec("settings", "Профиль", Icons.Outlined.PersonOutline, Icons.Rounded.Person),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NomNomTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Root()
                }
            }
        }
    }
}

@Composable
private fun Root() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val mainRoutes = (TABS_LEFT + TABS_RIGHT).map { it.route }
    val showTabs = currentRoute in mainRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showTabs) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onTabClick = { route ->
                        if (currentRoute != route) {
                            nav.navigate(route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onCaptureClick = { nav.navigate("capture") }
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable("home")     { HomeScreen(onCapture = { nav.navigate("capture") }) }
            composable("history")  { HistoryScreen() }
            composable("chef")     { ChefChatScreen() }
            composable("settings") { SettingsScreen(onReminders = { nav.navigate("reminders") }) }
            composable("capture")  { CaptureFlowScreen(onBack = { nav.popBackStack() }) }
            composable("reminders"){ RemindersScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

@Composable
private fun BottomNavBar(
    currentRoute: String?,
    onTabClick: (String) -> Unit,
    onCaptureClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .height(78.dp),
    ) {
        // Тонкая верхняя полоска-разделитель
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TABS_LEFT.forEach { t ->
                NavItem(
                    spec = t,
                    selected = currentRoute == t.route,
                    onClick = { onTabClick(t.route) },
                    modifier = Modifier.weight(1f),
                )
            }
            // место под центральную круглую кнопку (она поверх)
            Spacer(Modifier.weight(1f))
            TABS_RIGHT.forEach { t ->
                NavItem(
                    spec = t,
                    selected = currentRoute == t.route,
                    onClick = { onTabClick(t.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // Центральная фиолетовая FAB-кнопка
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .size(56.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(50), clip = false)
                .clip(RoundedCornerShape(50))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(VioletPrimary, VioletDeep)
                    )
                )
                .clickable(onClick = onCaptureClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = "добавить блюдо",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun NavItem(
    spec: TabSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) VioletPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (selected) spec.filled else spec.outlined,
            contentDescription = spec.label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            spec.label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}


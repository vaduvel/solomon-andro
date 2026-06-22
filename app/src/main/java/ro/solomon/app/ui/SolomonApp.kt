package ro.solomon.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import ro.solomon.app.ui.screens.AnalysisScreen
import ro.solomon.app.ui.screens.SettingsScreen
import ro.solomon.app.ui.screens.TodayScreen
import ro.solomon.app.ui.screens.WalletScreen
import ro.solomon.app.ui.theme.SolomonTheme

data class TabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val tabs = listOf(
    TabItem("Azi", Icons.Filled.Home, Icons.Outlined.Home),
    TabItem("Analiză", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    TabItem("Portofel", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
    TabItem("Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
    TabItem("Setări", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolomonApp() {
    SolomonTheme {
        var selectedTab by remember { mutableIntStateOf(0) }
        val openChat = remember { mutableStateOf(false) }

        Scaffold(
            containerColor = ro.solomon.app.ui.theme.SolomonColors.Background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                ro.solomon.app.ui.components.MeshBackground()
                when (selectedTab) {
                    0 -> TodayScreen()
                    1 -> AnalysisScreen()
                    2 -> WalletScreen()
                    3 -> { openChat.value = true; selectedTab = 0 }
                    4 -> SettingsScreen()
                }
            }
        }

        if (openChat.value) {
            ro.solomon.app.ui.chat.ChatSheet(onDismiss = { openChat.value = false })
        }
    }
}

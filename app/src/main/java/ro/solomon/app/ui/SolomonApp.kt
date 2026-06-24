package ro.solomon.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ro.solomon.app.ui.screens.AnalysisScreen
import ro.solomon.app.ui.screens.BudgetsScreen
import ro.solomon.app.ui.screens.SettingsScreen
import ro.solomon.app.ui.screens.TodayScreen
import ro.solomon.app.ui.screens.WalletScreen
import ro.solomon.app.ui.theme.SolRadius
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.app.ui.theme.SolomonTheme

data class TabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val tabs = listOf(
    TabItem("Azi", Icons.Filled.Home, Icons.Outlined.Home),
    TabItem("Analiz\u0103", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    TabItem("Bugete", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    TabItem("Portofel", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
    TabItem("Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
    TabItem("Set\u0103ri", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolomonApp() {
    SolomonTheme {
        var selectedTab by remember { mutableIntStateOf(0) }
        val openChat = remember { mutableStateOf(false) }

        Scaffold(
            containerColor = SolomonColors.Background,
            bottomBar = {
                GlassBottomBar(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onSelect = { selectedTab = it }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                ro.solomon.app.ui.components.MeshBackground()
                when (selectedTab) {
                    0 -> TodayScreen()
                    1 -> AnalysisScreen()
                    2 -> BudgetsScreen()
                    3 -> WalletScreen()
                    4 -> { openChat.value = true; selectedTab = 0 }
                    5 -> SettingsScreen()
                }
            }
        }

        if (openChat.value) {
            ro.solomon.app.ui.chat.ChatSheet(onDismiss = { openChat.value = false })
        }
    }
}

@Composable
private fun GlassBottomBar(
    tabs: List<TabItem>,
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = SolSpacing.base, vertical = SolSpacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SolRadius.pill))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(SolRadius.pill))
                .padding(horizontal = SolSpacing.sm, vertical = SolSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val active = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(SolRadius.pill))
                        .background(if (active) SolomonColors.Primary.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (active) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = if (active) SolomonColors.Primary else SolomonColors.TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

package com.termuxagent.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.termuxagent.data.settings.AppSettings
import com.termuxagent.data.settings.SettingsStore
import com.termuxagent.ui.AppContext
import com.termuxagent.ui.chat.ChatScreen
import com.termuxagent.ui.settings.SettingsScreen
import com.termuxagent.ui.theme.TermuXagentTheme
import com.termuxagent.ui.workspace.WorkspaceScreen

object Routes {
    const val CHAT = "chat"
    const val WORKSPACE = "workspace"
    const val SETTINGS = "settings"
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current.applicationContext
    AppContext.bind(ctx)

    val settingsFlow = remember { SettingsStore.flow(ctx) }
    val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())

    TermuXagentTheme(
        themeMode = settings.themeMode,
        dynamicColor = settings.dynamicColor
    ) {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = Routes.CHAT) {
            composable(Routes.CHAT) {
                ChatScreen(
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenWorkspace = { nav.navigate(Routes.WORKSPACE) }
                )
            }
            composable(Routes.WORKSPACE) {
                WorkspaceScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

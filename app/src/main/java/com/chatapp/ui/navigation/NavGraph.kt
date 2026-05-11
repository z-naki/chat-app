package com.chatapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chatapp.domain.model.ProviderType
import com.chatapp.ui.chat.ChatScreen
import com.chatapp.ui.home.HomeScreen
import com.chatapp.ui.provideredit.ProviderEditScreen
import com.chatapp.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val CHAT = "chat/{conversationId}"
    const val SETTINGS = "settings"
    const val PROVIDER_EDIT = "provider_edit/{providerType}"

    fun chatRoute(conversationId: Long) = "chat/$conversationId"
    fun providerEditRoute(providerType: ProviderType) = "provider_edit/${providerType.name}"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onConversationClick = { convId ->
                    navController.navigate(Routes.chatRoute(convId))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: return@composable
            ChatScreen(
                conversationId = conversationId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onEditProvider = { provider ->
                    navController.navigate(Routes.providerEditRoute(provider))
                }
            )
        }

        composable(
            route = Routes.PROVIDER_EDIT,
            arguments = listOf(
                navArgument("providerType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val providerName = backStackEntry.arguments?.getString("providerType") ?: return@composable
            val providerType = ProviderType.fromStringOrDefault(providerName)
            ProviderEditScreen(
                providerType = providerType,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

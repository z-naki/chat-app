package com.chatapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chatapp.domain.model.ProviderType
import com.chatapp.ui.home.HomeScreen
import com.chatapp.ui.provideredit.ProviderEditScreen
import com.chatapp.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PROVIDER_EDIT = "provider_edit/{providerType}"

    fun providerEditRoute(providerType: ProviderType) = "provider_edit/${providerType.name}"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    var currentConversationId by rememberSaveable { mutableLongStateOf(-1L) }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        composable(Routes.HOME) {
            BackHandler(enabled = currentConversationId != -1L) {
                currentConversationId = -1L
            }

            HomeScreen(
                conversationId = currentConversationId,
                onConversationClick = { convId ->
                    currentConversationId = convId
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
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

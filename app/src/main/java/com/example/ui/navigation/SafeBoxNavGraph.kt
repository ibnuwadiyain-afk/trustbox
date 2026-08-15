package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.data.repository.SafeBoxRepository
import com.example.data.security.SecurityManager
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthViewModelFactory
import com.example.ui.clients.ClientListScreen
import com.example.ui.clients.ClientListViewModel
import com.example.ui.clients.ClientListViewModelFactory
import com.example.ui.details.ClientDetailScreen
import com.example.ui.details.ClientDetailViewModel
import com.example.ui.details.ClientDetailViewModelFactory
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.settings.SettingsViewModelFactory

object Destinations {
  const val AUTH = "auth"
  const val CLIENTS = "clients"
  const val CLIENT_DETAIL = "client_detail/{clientId}"
  const val SETTINGS = "settings"

  fun clientDetailRoute(clientId: Long): String = "client_detail/$clientId"
}

@Composable
fun SafeBoxNavGraph(
  navController: NavHostController,
  repository: SafeBoxRepository,
  securityManager: SecurityManager,
  modifier: Modifier = Modifier
) {
  val authViewModel: AuthViewModel = viewModel(
    factory = AuthViewModelFactory(securityManager)
  )

  NavHost(
    navController = navController,
    startDestination = Destinations.AUTH,
    modifier = modifier
  ) {
    composable(Destinations.AUTH) {
      AuthScreen(
        viewModel = authViewModel,
        onAuthSuccess = {
          navController.navigate(Destinations.CLIENTS) {
            popUpTo(Destinations.AUTH) { inclusive = true }
          }
        }
      )
    }

    composable(Destinations.CLIENTS) {
      val clientListViewModel: ClientListViewModel = viewModel(
        factory = ClientListViewModelFactory(repository)
      )

      ClientListScreen(
        viewModel = clientListViewModel,
        onClientClick = { clientId ->
          navController.navigate(Destinations.clientDetailRoute(clientId))
        },
        onSettingsClick = {
          navController.navigate(Destinations.SETTINGS)
        },
        onLockClick = {
          authViewModel.lockApp()
          navController.navigate(Destinations.AUTH) {
            popUpTo(0) { inclusive = true }
          }
        }
      )
    }

    composable(
      route = Destinations.CLIENT_DETAIL,
      arguments = listOf(
        navArgument("clientId") { type = NavType.LongType }
      )
    ) { backStackEntry ->
      val clientId = backStackEntry.arguments?.getLong("clientId") ?: 0L
      val clientDetailViewModel: ClientDetailViewModel = viewModel(
        factory = ClientDetailViewModelFactory(clientId, repository)
      )

      ClientDetailScreen(
        viewModel = clientDetailViewModel,
        onNavigateBack = {
          navController.popBackStack()
        }
      )
    }

    composable(Destinations.SETTINGS) {
      val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(securityManager, repository)
      )

      SettingsScreen(
        viewModel = settingsViewModel,
        onNavigateBack = {
          navController.popBackStack()
        }
      )
    }
  }
}

package fr.mangi.zendure.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.mangi.zendure.ui.screens.ConfigScreen
import fr.mangi.zendure.ui.screens.ResultScreen
import fr.mangi.zendure.ui.screens.ScanScreen
import fr.mangi.zendure.viewmodel.MainViewModel

object Routes {
    const val SCAN = "scan"
    const val CONFIG = "config"
    const val RESULT = "result"
}

@Composable
fun ZendureNavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Routes.SCAN) {
        composable(Routes.SCAN) {
            ScanScreen(
                viewModel = viewModel,
                onDeviceSelected = { navController.navigate(Routes.CONFIG) },
            )
        }
        composable(Routes.CONFIG) {
            ConfigScreen(
                viewModel = viewModel,
                onApply = {
                    viewModel.startProvisioning()
                    navController.navigate(Routes.RESULT)
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.RESULT) {
            ResultScreen(
                viewModel = viewModel,
                onRetry = {
                    viewModel.resetProvisioning()
                    navController.popBackStack()
                },
                onFinish = {
                    viewModel.resetProvisioning()
                    navController.popBackStack(Routes.SCAN, inclusive = false)
                },
            )
        }
    }
}

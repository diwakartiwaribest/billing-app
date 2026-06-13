package com.shop.billing.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shop.billing.ui.screens.auth.AuthScreen
import com.shop.billing.ui.screens.billdetail.BillDetailScreen
import com.shop.billing.ui.screens.dbmanager.DatabaseManagerScreen
import com.shop.billing.ui.screens.history.HistoryScreen
import com.shop.billing.ui.screens.home.HomeScreen
import com.shop.billing.ui.screens.items.ItemsScreen
import com.shop.billing.ui.screens.ledger.CustomerDetailScreen
import com.shop.billing.ui.screens.ledger.CustomerLedgerScreen
import com.shop.billing.ui.screens.newbill.NewBillScreen
import com.shop.billing.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = NavRoutes.Auth.route
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(NavRoutes.Auth.route) {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        composable(NavRoutes.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(NavRoutes.Items.route) {
            ItemsScreen(navController = navController)
        }
        composable(NavRoutes.NewBill.route) {
            NewBillScreen(navController = navController)
        }
        composable(NavRoutes.History.route) {
            HistoryScreen(navController = navController)
        }
        composable(NavRoutes.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(
            route = NavRoutes.DatabaseManager.route,
            arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getInt("tab") ?: 0
            DatabaseManagerScreen(navController = navController, initialTab = tab)
        }
        composable(
            route = NavRoutes.BillDetail.route,
            arguments = listOf(navArgument("billId") { type = NavType.StringType })
        ) { backStackEntry ->
            val billId = backStackEntry.arguments?.getString("billId") ?: return@composable
            BillDetailScreen(billId = billId, navController = navController)
        }
        composable(NavRoutes.CustomerLedger.route) {
            CustomerLedgerScreen(navController = navController)
        }
        composable(
            route = NavRoutes.CustomerDetail.route,
            arguments = listOf(navArgument("mobile") { type = NavType.StringType })
        ) { backStackEntry ->
            val mobile = backStackEntry.arguments?.getString("mobile") ?: return@composable
            CustomerDetailScreen(mobile = mobile, navController = navController)
        }
    }
}

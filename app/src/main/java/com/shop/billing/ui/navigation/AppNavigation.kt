package com.shop.billing.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shop.billing.ui.screens.auth.AuthScreen
import com.shop.billing.ui.screens.billdetail.BillDetailScreen
import com.shop.billing.ui.screens.customers.CustomersScreen
import com.shop.billing.ui.screens.history.HistoryScreen
import com.shop.billing.ui.screens.history.HistoryViewModel
import com.shop.billing.ui.screens.home.HomeScreen
import com.shop.billing.ui.screens.investment.InvestmentScreen
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
        composable(NavRoutes.DailySales.route) {
            val viewModel: HistoryViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                viewModel.startDate.value = cal.timeInMillis
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                cal.set(java.util.Calendar.MILLISECOND, 999)
                viewModel.endDate.value = cal.timeInMillis
            }
            HistoryScreen(navController = navController)
        }
        composable(NavRoutes.WeeklySales.route) {
            val viewModel: HistoryViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                viewModel.startDate.value = cal.timeInMillis
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                cal.set(java.util.Calendar.MILLISECOND, 999)
                viewModel.endDate.value = cal.timeInMillis
            }
            HistoryScreen(navController = navController)
        }
        composable(NavRoutes.MonthlySales.route) {
            val viewModel: HistoryViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                viewModel.startDate.value = cal.timeInMillis
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                cal.set(java.util.Calendar.MILLISECOND, 999)
                viewModel.endDate.value = cal.timeInMillis
            }
            HistoryScreen(navController = navController)
        }
        composable(NavRoutes.Investment.route) {
            InvestmentScreen(navController = navController)
        }
        composable(NavRoutes.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(
            route = NavRoutes.StockFilteredItems.route,
            arguments = listOf(navArgument("filter") { type = NavType.StringType })
        ) { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filter") ?: ""
            ItemsScreen(navController = navController, stockFilter = filter)
        }
        composable(NavRoutes.Customers.route) {
            CustomersScreen(navController = navController)
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

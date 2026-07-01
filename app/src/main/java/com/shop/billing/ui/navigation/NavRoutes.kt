package com.shop.billing.ui.navigation

sealed class NavRoutes(val route: String) {
    data object Auth : NavRoutes("auth")
    data object Home : NavRoutes("home")
    data object Items : NavRoutes("items")
    data object NewBill : NavRoutes("new_bill")
    data object History : NavRoutes("history")
    data object Settings : NavRoutes("settings")
    data object DailySales : NavRoutes("daily_sales")
    data object WeeklySales : NavRoutes("weekly_sales")
    data object MonthlySales : NavRoutes("monthly_sales")
    data object Customers : NavRoutes("customers")
    data object BillDetail : NavRoutes("bill_detail/{billId}") {
        fun createRoute(billId: String) = "bill_detail/$billId"
    }
    data object CustomerLedger : NavRoutes("customer_ledger")
    data object CustomerDetail : NavRoutes("customer_detail/{mobile}") {
        fun createRoute(mobile: String) = "customer_detail/$mobile"
    }
    data object StockFilteredItems : NavRoutes("stock_filtered/{filter}") {
        fun createRoute(filter: String) = "stock_filtered/$filter"
    }
    data object Investment : NavRoutes("investment")
}

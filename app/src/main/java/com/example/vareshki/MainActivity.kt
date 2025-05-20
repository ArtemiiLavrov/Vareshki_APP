package com.example.vareshki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VareshkiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: LoginViewModel = LoginViewModel(this)

                    NavHost(
                        navController = navController,
                        startDestination = if (viewModel.isUserLoggedIn()) "mainScreen" else "loginScreen"
                    ) {
                        composable("loginScreen") {
                            LoginScreen(viewModel = viewModel, navController = navController)
                        }
                        composable("registrationScreen") {
                            RegistrationScreen(viewModel = viewModel, navController = navController)
                        }
                        composable("mainScreen") {
                            MainScreen(viewModel = viewModel, navController = navController)
                        }
                        composable("adminMenuScreen") {
                            AdminMenuScreen(
                                onCanteensClick = { navController.navigate("canteensScreen") },
                                onEmployeesClick = { navController.navigate("employeesScreen") },
                                onStatusesClick = { navController.navigate("statusesScreen") },
                                onProductsClick = { navController.navigate("productsScreen") },
                                onOrderSelectionClick = { navController.navigate("orderSelectionScreen") },
                                onShowProfile = { navController.navigate("profileScreen") },
                                onSelectOrdersForInvoiceClick = { navController.navigate("selectOrdersForInvoiceScreen") },
                                onViewInvoicesClick = { navController.navigate("invoicesScreen") }
                            )
                        }
                        composable("invoicesScreen") {
                            InvoicesScreen(
                                onBack = { navController.popBackStack() },
                                onShowProfile = { navController.navigate("profileScreen") }
                            )
                        }
                    }
                }
            }
        }
    }
}
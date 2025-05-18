package com.example.vareshki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VareshkiTheme {
                Surface {
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
                    }
                }
            }
        }
    }
}
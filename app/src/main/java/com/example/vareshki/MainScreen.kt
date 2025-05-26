package com.example.vareshki

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import android.os.Parcelable
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.rememberDateRangePickerState
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.parcelize.Parcelize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.*
import com.google.gson.Gson
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.delay
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavController
import kotlinx.coroutines.delay

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LoginViewModel, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    var isLoggedIn by remember { mutableStateOf(viewModel.isUserLoggedIn()) }
    var showProfile by remember { mutableStateOf(false) }
    val invoiceGenerator = remember { InvoiceGenerator(viewModel) }
    val context = LocalContext.current
    val invoiceStorageManager = remember { InvoiceStorageManager(context) }

    println("MainScreen - isLoggedIn: $isLoggedIn, showProfile: $showProfile") // Лог для отладки

    if (!isLoggedIn) {
        // Отображаем отдельный LoginScreen
        LoginScreen(viewModel = viewModel, navController = navController)
    } else if (showProfile) {
        ProfileScreen(
            viewModel = viewModel,
            onBack = { showProfile = false },
            onLogout = {
                coroutineScope.launch {
                    viewModel.logout()
                    isLoggedIn = false
                }
            }
        )
    } else {
        MainNavGraph(
            viewModel = viewModel,
            onShowProfile = { showProfile = true },
            onLogout = {
                coroutineScope.launch {
                    viewModel.logout()
                    isLoggedIn = false
                }
            },
            invoiceGenerator = invoiceGenerator,
            invoiceStorageManager = invoiceStorageManager
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph(
    viewModel: LoginViewModel, 
    onShowProfile: () -> Unit, 
    onLogout: () -> Unit,
    invoiceGenerator: InvoiceGenerator,
    invoiceStorageManager: InvoiceStorageManager
) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()
    val gson = Gson()
    val isAdmin = viewModel.isAdmin()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Заказы") },
                    label = { Text("Заказы") },
                    selected = currentRoute?.destination?.route == "ordersScreen",
                    onClick = {
                        println("Navigating to ordersScreen")
                        navController.navigate("ordersScreen") {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Создать заказ") },
                    label = { Text("Создать заказ") },
                    selected = currentRoute?.destination?.route == "createOrderScreen",
                    onClick = {
                        println("Navigating to createOrderScreen")
                        navController.navigate("createOrderScreen") {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                if (isAdmin) {
                    // Кнопка меню для администратора
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Menu, contentDescription = "Меню администратора") },
                        label = { Text("Меню") },
                        selected = currentRoute?.destination?.route == "adminMenuScreen",
                        onClick = {
                            println("Navigating to adminMenuScreen")
                            navController.navigate("adminMenuScreen") {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                } else {
                    // Для обычного пользователя отображаем текущую кнопку "Продукты"
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Продукты") },
                        label = { Text("Продукты") },
                        selected = currentRoute?.destination?.route == "productsScreen",
                        onClick = {
                            println("Navigating to productsScreen")
                            navController.navigate("productsScreen") {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "ordersScreen",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("ordersScreen") {
                var filters by remember { mutableStateOf(FilterParams()) }

                LaunchedEffect(navController.currentBackStackEntry?.savedStateHandle) {
                    val filtersJson = navController.currentBackStackEntry?.savedStateHandle?.get<String>("filters_applied")
                    filtersJson?.let { json ->
                        try {
                            val appliedFilters = gson.fromJson(json, FilterParams::class.java)
                            filters = appliedFilters
                            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("filters_applied")
                        } catch (e: Exception) {
                            println("Failed to deserialize filters: ${e.message}")
                        }
                    }
                }

                OrdersScreen(
                    viewModel = viewModel,
                    onOrderSelected = { orderId ->
                        navController.navigate("orderDetailsScreen/$orderId")
                    },
                    onShowFilters = { filters ->
                        val filtersJson = gson.toJson(filters)
                        navController.navigate("filterScreen/$filtersJson")
                    },
                    /*onFiltersApplied = { filters ->
                        val filtersJson = gson.toJson(filters)
                        navController.previousBackStackEntry?.savedStateHandle?.set("filters_applied", filtersJson)
                        navController.popBackStack()
                    },*/
                    filters = filters
                )
            }

            composable(
                route = "filterScreen/{filtersJson}",
                arguments = listOf(navArgument("filtersJson") { type = NavType.StringType })
            ) { backStackEntry ->
                val filtersJson = backStackEntry.arguments?.getString("filtersJson")
                val initialFilters = try {
                    if (filtersJson != null) {
                        gson.fromJson(filtersJson, FilterParams::class.java)
                    } else {
                        FilterParams()
                    }
                } catch (e: Exception) {
                    println("Failed to deserialize initial filters: "+e.message)
                    FilterParams()
                }

                FilterScreen(
                    viewModel = viewModel,
                    initialFilters = initialFilters,
                    onApplyFilters = { newFilters ->
                        val newFiltersJson = gson.toJson(newFilters)
                        navController.previousBackStackEntry?.savedStateHandle?.set("filters_applied", newFiltersJson)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("createOrderScreen") {
                CreateOrderScreen(
                    viewModel = viewModel,
                    onOrderCreated = { navController.popBackStack() },
                    onSelectProducts = { }
                )
            }

            composable("productsScreen") {
                ProductsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onShowProfile = onShowProfile,
                    onAddProduct = { navController.navigate("addProductScreen") },
                    onEditProduct = { product ->
                        try {
                            val productJson = gson.toJson(product)
                            val encodedProductJson = URLEncoder.encode(productJson, StandardCharsets.UTF_8.toString())
                            navController.navigate("editProductScreen/$encodedProductJson")
                        } catch (e: Exception) {
                            println("Navigation to editProductScreen failed: ${e.message}")
                        }
                    }
                )
            }

            composable("addProductScreen") {
                AddProductScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onProductAdded = { navController.popBackStack() }
                )
            }

            composable(
                route = "editProductScreen/{productJson}",
                arguments = listOf(navArgument("productJson") { type = NavType.StringType })
            ) { backStackEntry ->
                val productJson = backStackEntry.arguments?.getString("productJson")
                val product = try {
                    productJson?.let { gson.fromJson(URLDecoder.decode(it, StandardCharsets.UTF_8.toString()), Product::class.java) }
                        ?: throw IllegalArgumentException("Product data is missing")
                } catch (e: Exception) {
                    println("Failed to deserialize product: ${e.message}")
                    null
                }

                if (product != null) {
                    EditProductScreen(
                        viewModel = viewModel,
                        product = product,
                        onBack = { navController.popBackStack() },
                        onProductUpdated = { navController.popBackStack() }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ошибка загрузки продукта",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            composable(
                route = "orderDetailsScreen/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.IntType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getInt("orderId") ?: 0
                OrderDetailsScreen(
                    viewModel = viewModel,
                    orderId = orderId,
                    onBack = { navController.popBackStack() },
                    onShowProfile = onShowProfile
                )
            }

            composable("adminMenuScreen") {
                AdminMenuScreen(
                    onCanteensClick = { navController.navigate("canteensScreen") },
                    onEmployeesClick = { navController.navigate("employeesScreen") },
                    onStatusesClick = { navController.navigate("statusesScreen") },
                    onProductsClick = { navController.navigate("productsScreen") },
                    onOrderSelectionClick = { navController.navigate("orderSelectionScreen") },
                    onShowProfile = onShowProfile,
                    onSelectOrdersForInvoiceClick = { navController.navigate("selectOrdersForInvoiceScreen") },
                    onViewInvoicesClick = { navController.navigate("invoicesScreen") }
                )
            }

            composable("canteensScreen") {
                CanteensScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAddCanteen = { navController.navigate("addCanteenScreen") },
                    onEditCanteen = { canteen ->
                        val canteenJson = URLEncoder.encode(gson.toJson(canteen), StandardCharsets.UTF_8.toString())
                        navController.navigate("editCanteenScreen/$canteenJson")
                    },
                    onShowProfile = onShowProfile
                )
            }

            composable("addCanteenScreen") {
                AddCanteenScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onCanteenAdded = { navController.popBackStack() }
                )
            }

            composable(
                route = "editCanteenScreen/{canteenJson}",
                arguments = listOf(navArgument("canteenJson") { type = NavType.StringType })
            ) { backStackEntry ->
                val canteenJson = backStackEntry.arguments?.getString("canteenJson")
                val canteen = try {
                    canteenJson?.let { gson.fromJson(URLDecoder.decode(it, StandardCharsets.UTF_8.toString()), Canteen::class.java) }
                        ?: throw IllegalArgumentException("Canteen data is missing")
                } catch (e: Exception) {
                    println("Failed to deserialize canteen: ${e.message}")
                    null
                }

                if (canteen != null) {
                    EditCanteenScreen(
                        viewModel = viewModel,
                        canteen = canteen,
                        onBack = { navController.popBackStack() },
                        onCanteenUpdated = { navController.popBackStack() }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ошибка загрузки столовой",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            composable("employeesScreen") {
                EmployeesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAddEmployee = { navController.navigate("addEmployeeScreen") },
                    onEditEmployee = { employee ->
                        val employeeJson = URLEncoder.encode(gson.toJson(employee), StandardCharsets.UTF_8.toString())
                        navController.navigate("editEmployeeScreen/$employeeJson")
                    },
                    onShowProfile = onShowProfile
                )
            }

            composable("addEmployeeScreen") {
                AddEmployeeScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEmployeeAdded = { navController.popBackStack() },
                    onShowProfile = onShowProfile
                )
            }

            composable(
                route = "editEmployeeScreen/{employeeJson}",
                arguments = listOf(navArgument("employeeJson") { type = NavType.StringType })
            ) { backStackEntry ->
                val employeeJson = backStackEntry.arguments?.getString("employeeJson")
                val employee = try {
                    employeeJson?.let { gson.fromJson(URLDecoder.decode(it, StandardCharsets.UTF_8.toString()), Employee::class.java) }
                        ?: throw IllegalArgumentException("Employee data is missing")
                } catch (e: Exception) {
                    println("Failed to deserialize employee: ${e.message}")
                    null
                }

                if (employee != null) {
                    EditEmployeeScreen(
                        viewModel = viewModel,
                        employee = employee,
                        onBack = { navController.popBackStack() },
                        onEmployeeUpdated = { navController.popBackStack() },
                        onShowProfile = onShowProfile
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ошибка загрузки сотрудника",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            composable("statusesScreen") {
                StatusesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAddStatus = { navController.navigate("addStatusScreen") },
                    onEditStatus = { status ->
                        val statusJson = URLEncoder.encode(gson.toJson(status), StandardCharsets.UTF_8.toString())
                        navController.navigate("editStatusScreen/$statusJson")
                    },
                    onShowProfile = onShowProfile
                )
            }

            composable("addStatusScreen") {
                AddStatusScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onStatusAdded = { navController.popBackStack() }
                )
            }

            composable(
                route = "editStatusScreen/{statusJson}",
                arguments = listOf(navArgument("statusJson") { type = NavType.StringType })
            ) { backStackEntry ->
                val statusJson = backStackEntry.arguments?.getString("statusJson")
                val status = try {
                    statusJson?.let { gson.fromJson(URLDecoder.decode(it, StandardCharsets.UTF_8.toString()), OrderStatus::class.java) }
                        ?: throw IllegalArgumentException("Status data is missing")
                } catch (e: Exception) {
                    println("Failed to deserialize status: ${e.message}")
                    null
                }

                if (status != null) {
                    EditStatusScreen(
                        viewModel = viewModel,
                        status = status,
                        onBack = { navController.popBackStack() },
                        onStatusUpdated = { navController.popBackStack() }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ошибка загрузки статуса",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            composable("orderSelectionScreen") {
                OrderSelectionScreen(
                    viewModel = viewModel,
                    onOrderSelected = { orderId -> navController.navigate("statusHistoryScreen/$orderId") },
                    onBack = { navController.popBackStack() },
                    onShowProfile = onShowProfile
                )
            }

            composable(
                route = "statusHistoryScreen/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.IntType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getInt("orderId") ?: 0
                OrderStatusHistoryScreen(
                    viewModel = viewModel,
                    orderId = orderId,
                    onBack = { navController.popBackStack() },
                    onShowProfile = onShowProfile
                )
            }

            @Composable
            fun InvoiceProcessingScreen(
                selectedOrders: List<Order>,
                onComplete: () -> Unit
            ) {
                var progress by remember { mutableStateOf(0f) }
                var currentOrder by remember { mutableStateOf<Order?>(null) }
                var error by remember { mutableStateOf<String?>(null) }
                
                LaunchedEffect(selectedOrders) {
                    try {
                        val totalOrders = selectedOrders.size
                        
                        // Создаем накладные для каждого выбранного заказа
                        selectedOrders.forEachIndexed { index, order ->
                            currentOrder = order
                            try {
                                // Получаем детали заказа
                                val orderDetails = viewModel.fetchOrderDetails(order.orderId)
                                val invoiceXml = invoiceGenerator.generateInvoiceXml(order, orderDetails)
                                invoiceStorageManager.saveInvoice(order.orderId.toString(), invoiceXml)
                                progress = (index + 1f) / totalOrders
                            } catch (e: Exception) {
                                error = "Ошибка при создании накладной для заказа ${order.orderId}: ${e.message}"
                                // Продолжаем с следующим заказом
                            }
                        }
                        // Небольшая задержка, чтобы пользователь увидел завершение
                        kotlinx.coroutines.delay(500)
                        onComplete()
                    } catch (e: Exception) {
                        error = "Общая ошибка при создании накладных: ${e.message}"
                        // Небольшая задержка перед возвратом
                        kotlinx.coroutines.delay(2000)
                        onComplete()
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (error != null) {
                            Text(
                                text = error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                progress = progress,
                                modifier = Modifier.size(64.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            currentOrder?.let { order ->
                                Text(
                                    text = "Формирование накладной для заказа №${order.orderId}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            composable("selectOrdersForInvoiceScreen") {
                var selectedOrders by remember { mutableStateOf<List<Order>?>(null) }
                
                if (selectedOrders != null) {
                    InvoiceProcessingScreen(
                        selectedOrders = selectedOrders!!,
                        onComplete = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    SelectOrdersForInvoiceScreen(
                        viewModel = viewModel,
                        onFormInvoices = { orders ->
                            selectedOrders = orders
                        }
                    )
                }
            }

            composable("invoicesScreen") {
                InvoicesScreen(
                    onBack = { navController.popBackStack() },
                    onShowProfile = onShowProfile
                )
            }
        }
    }
}
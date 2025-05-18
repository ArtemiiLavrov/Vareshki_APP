package com.example.vareshki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier,
    onOrderSelected: (Int) -> Unit,
    onShowFilters: (FilterParams) -> Unit,
    onFiltersApplied: (FilterParams) -> Unit,
    filters: FilterParams
) {
    val coroutineScope = rememberCoroutineScope()
    val orders by viewModel.orders.collectAsState()
    val orderViewStatus by viewModel.orderViewStatus.collectAsState() // Подписываемся на статус просмотра
    val errorMessage by viewModel.errorMessage.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST_FIRST) }
    var expandedSort by remember { mutableStateOf(false) }
    var localFilters by remember { mutableStateOf(FilterParams()) }

    // Обновляем локальные фильтры при изменении переданных фильтров
    LaunchedEffect(filters) {
        if (filters != FilterParams()) {
            println("Filters updated in OrdersScreen: $filters")
            localFilters = filters
            // onFiltersApplied(filters) убран для предотвращения ошибки Coroutine scope left the composition
        }
    }

    // Функция загрузки заказов с учётом фильтров
    fun loadOrders() {
        coroutineScope.launch {
            try {
                isLoading = true
                println("Starting to fetch orders with filters: $localFilters, sortOrder: $sortOrder")
                println(
                    "Filter details - senderCanteenId: ${localFilters.senderCanteen?.canteenId}, " +
                            "receiverCanteenId: ${localFilters.receiverCanteen?.canteenId}, " +
                            "startDate: ${localFilters.startDate}, endDate: ${localFilters.endDate}, " +
                            "statusId: ${localFilters.status?.statusId}"
                )
                viewModel.fetchOrders(
                    sortOrder = sortOrder,
                    senderCanteenId = localFilters.senderCanteen?.canteenId,
                    receiverCanteenId = localFilters.receiverCanteen?.canteenId,
                    startDate = localFilters.startDate,
                    endDate = localFilters.endDate,
                    statusId = localFilters.status?.statusId
                )
                println("Orders loaded in UI: $orders")
            } catch (e: Exception) {
                viewModel.setErrorMessage(
                    when {
                        e.message?.contains("Неверный формат даты") == true -> e.message
                        else -> "Не удалось загрузить заказы: ${e.message}"
                    }
                )
                println("Error loading orders in UI: ${e.message}")
            } finally {
                isLoading = false
                println("Finished loading orders, isLoading: $isLoading, orders size: ${orders.size}")
            }
        }
    }

    // Загружаем заказы при изменении фильтров или сортировки
    LaunchedEffect(sortOrder, localFilters) {
        println("Loading orders with filters: $localFilters, sortOrder: $sortOrder")
        loadOrders()
    }

    // Дополнительная загрузка, если заказы пусты
    LaunchedEffect(Unit) {
        if (orders.isEmpty() && !isLoading) {
            println("Orders are empty, reloading with filters: $localFilters")
            loadOrders()
        }
    }

    // UI
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок и кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            println("Filter button clicked, current filters: $localFilters")
                            onShowFilters(localFilters)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Фильтры",
                            tint = if (localFilters.senderCanteen != null || localFilters.receiverCanteen != null || localFilters.startDate != null || localFilters.status != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text("Заказы", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Box {
                    IconButton(
                        onClick = { expandedSort = true },
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Сортировка",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expandedSort,
                        onDismissRequest = { expandedSort = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Более новые") },
                            onClick = {
                                sortOrder = SortOrder.NEWEST_FIRST
                                expandedSort = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Более старые") },
                            onClick = {
                                sortOrder = SortOrder.OLDEST_FIRST
                                expandedSort = false
                            }
                        )
                    }
                }
            }

            // Логи для отладки
            println("Rendering UI: isLoading=$isLoading, errorMessage=$errorMessage, orders size=${orders.size}")

            // Состояния UI
            when {
                isLoading -> {
                    println("Showing loading indicator")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    println("Showing error message: $errorMessage")
                    Text(
                        text = errorMessage ?: "Неизвестная ошибка",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
                orders.isEmpty() -> {
                    println("Showing 'No orders available' message")
                    Text(
                        text = "Нет доступных заказов",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    println("Showing orders list with ${orders.size} items")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(orders) { order ->
                            println("Rendering order: ${order.orderId}")
                            val isViewed = orderViewStatus[order.orderId] ?: order.isViewed ?: false
                            OrderItem(
                                order = order,
                                isViewed = isViewed,
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            viewModel.markOrderAsViewed(order.orderId)
                                            onOrderSelected(order.orderId)
                                        } catch (e: Exception) {
                                            println("Error marking order as viewed: ${e.message}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItem(
    order: Order,
    isViewed: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isViewed) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Заказ №${order.orderId}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isViewed) "Просмотрено" else "Не прочитано",
                    color = if (isViewed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp
                )
            }
            Text("Дата и время создания: ${order.creationDate} ${order.creationTime}")
            Text("Отправитель: ${order.canteenSenderAddress}")
            Text("Получатель: ${order.canteenReceiverAddress}")
            Text("Статус: ${order.status.statusName}")
        }
    }
}
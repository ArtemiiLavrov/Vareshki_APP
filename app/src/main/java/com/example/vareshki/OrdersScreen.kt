package com.example.vareshki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier,
    onOrderSelected: (Int) -> Unit,
    onShowFilters: (FilterParams) -> Unit,
    //onFiltersApplied: () -> Unit,
    filters: FilterParams
) {
    val orders by viewModel.orders.collectAsState()
    val orderViewStatus by viewModel.orderViewStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST_FIRST) }
    var expandedSort by remember { mutableStateOf(false) }
    var localFilters by remember { mutableStateOf(FilterParams()) }
    var selectedStatus by remember { mutableStateOf("Все") }
    val statusList = listOf("Все", "Создан", "Собирается", "Исполнен", "Отменён")
    val scrollState = rememberScrollState()

    // Обновляем локальные фильтры при изменении переданных фильтров
    LaunchedEffect(filters) {
        if (filters != FilterParams()) {
            println("Filters updated in OrdersScreen: $filters")
            localFilters = filters
        }
    }

    // Загружаем заказы при изменении фильтров или сортировки с дебонсингом
    LaunchedEffect(sortOrder, localFilters) {
        println("Triggering loadOrders with filters: $localFilters, sortOrder: $sortOrder")
        //delay(300) // Дебонсинг 300 мс
        viewModel.loadOrders(
            sortOrder = sortOrder,
            senderCanteenId = localFilters.senderCanteen?.canteenId,
            receiverCanteenId = localFilters.receiverCanteen?.canteenId,
            startDate = localFilters.startDate,
            endDate = localFilters.endDate,
            statusId = localFilters.status?.statusId
        )
    }

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
            // Фильтр-бар статусов
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusList.forEach { status ->
                    if (status == selectedStatus) {
                        Card(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(top = 4.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                ) { selectedStatus = status },
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            ),
                                            radius = 90f
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = status,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selectedStatus = status },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = SolidColor(MaterialTheme.colorScheme.onSurface)
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Text(status)
                        }
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
                    // Фильтрация заказов по выбранному статусу
                    val filteredOrders = if (selectedStatus == "Все") orders else orders.filter { it.status.statusName.equals(selectedStatus, ignoreCase = true) }
                    if (filteredOrders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Заказов с указанным статусом нет",
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredOrders) { order ->
                                    println("Rendering order: ${order.orderId}")
                                    val isViewed = orderViewStatus[order.orderId] ?: order.isViewed ?: false
                                    OrderItem(
                                        order = order,
                                        isViewed = isViewed,
                                        onClick = {
                                            viewModel.markOrderAsViewedAsync(order.orderId)
                                            onOrderSelected(order.orderId)
                                        }
                                    )
                                }
                            }
                            // Верхний туман
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.background,
                                                MaterialTheme.colorScheme.background.copy(alpha = 0f)
                                            )
                                        )
                                    )
                            )
                            // Нижний туман
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.background.copy(alpha = 0f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
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
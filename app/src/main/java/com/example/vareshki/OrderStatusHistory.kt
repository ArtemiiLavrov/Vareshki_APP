package com.example.vareshki

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Модель данных для изменений статуса
data class StatusChange(
    val changeId: Int,
    val orderId: Int,
    val oldStatusId: Int,
    val newStatusId: Int,
    val changedByCanteenId: Int,
    val changeTimestamp: LocalDateTime
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderStatusHistoryScreen(
    viewModel: LoginViewModel,
    orderId: Int,
    onBack: () -> Unit,
    onShowProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val history by viewModel.getOrderStatusHistory(orderId).collectAsState(initial = emptyList())
    val statusCache by viewModel.statusCache.collectAsState()
    val canteenCache by viewModel.canteenCache.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var isHistoryLoaded by remember { mutableStateOf(false) }

    // Запускаем загрузку данных при первом рендере
    LaunchedEffect(Unit) {
        println("Starting data load for orderId=$orderId")
        viewModel.loadStatuses()
        viewModel.loadCanteens()
        while (statusCache.isEmpty() || canteenCache.isEmpty()) {
            println("Waiting for caches: statusCache.size=${statusCache.size}, canteenCache.size=${canteenCache.size}")
            delay(100)
        }
        println("Data load completed for orderId=$orderId")
    }

    // Отслеживаем загрузку history
    LaunchedEffect(history) {
        isHistoryLoaded = true
    }

    // Управляем состоянием загрузки
    LaunchedEffect(isHistoryLoaded, statusCache, canteenCache, errorMessage) {
        if (isHistoryLoaded && (statusCache.isNotEmpty() && canteenCache.isNotEmpty() || errorMessage != null)) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История статусов заказа №$orderId") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { onShowProfile() }) {
                        Icon(Icons.Default.Person, contentDescription = "Профиль")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(top = 16.dp)
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Произошла ошибка",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textAlign = TextAlign.Center
                )
            } else if (history.isEmpty()) {
                Text(
                    text = "Нет изменений статуса для этого заказа",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { change ->
                        StatusChangeCard(change, viewModel, statusCache, canteenCache)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChangeCard(
    change: StatusChange,
    viewModel: LoginViewModel,
    statusCache: Map<Int, String>,
    canteenCache: Map<Int, String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Время: ${change.changeTimestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Старый статус: ${statusCache[change.oldStatusId] ?: "Неизвестный статус"}",
                fontSize = 14.sp
            )
            Text(
                text = "Новый статус: ${statusCache[change.newStatusId] ?: "Неизвестный статус"}",
                fontSize = 14.sp
            )
            Text(
                text = "Изменила столовая: ${canteenCache[change.changedByCanteenId] ?: "Неизвестная столовая"}",
                fontSize = 14.sp
            )
        }
    }
}
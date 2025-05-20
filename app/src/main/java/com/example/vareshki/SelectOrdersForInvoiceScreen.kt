package com.example.vareshki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Calendar
import androidx.compose.foundation.interaction.MutableInteractionSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectOrdersForInvoiceScreen(
    viewModel: LoginViewModel,
    onFormInvoices: (List<Order>) -> Unit,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.orders.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var sortDescending by remember { mutableStateOf(true) }
    var filterDialogOpen by remember { mutableStateOf(false) }
    var filterStartDate by remember { mutableStateOf<String?>(null) }
    var filterEndDate by remember { mutableStateOf<String?>(null) }
    val selectedOrders = remember { mutableStateListOf<Int>() }
    var isSaving by remember { mutableStateOf(false) }

    // Фильтрация по статусу "Исполнен"
    val executedOrders = orders.filter { it.status.statusName.equals("Исполнен", ignoreCase = true) }
        .filter { order ->
            (searchQuery.isBlank() || order.orderId.toString().contains(searchQuery)) &&
            (filterStartDate.isNullOrBlank() || order.creationDate >= filterStartDate.orEmpty()) &&
            (filterEndDate.isNullOrBlank() || order.creationDate <= filterEndDate.orEmpty())
        }
        .sortedBy { it.creationDate }
        .let { if (sortDescending) it.reversed() else it }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { filterDialogOpen = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Фильтр")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск по номеру заказа") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { sortDescending = !sortDescending }) {
                    Icon(Icons.Default.Sort, contentDescription = "Сортировка по дате")
                }
            }
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() - 30.dp, top = 0.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = selectedOrders.isNotEmpty() && !isSaving
                        ) {
                            val selected = executedOrders.filter { selectedOrders.contains(it.orderId) }
                            onFormInvoices(selected)
                        },
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSaving) "Сохранение..." else "Сформировать накладные",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (executedOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет заказов со статусом 'Исполнен'")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(executedOrders) { order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Заказ №${order.orderId}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Дата создания: ${order.creationDate}",
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(onClick = {
                                if (selectedOrders.contains(order.orderId)) {
                                    selectedOrders.remove(order.orderId)
                                } else {
                                    selectedOrders.add(order.orderId)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Выбрать заказ",
                                    tint = if (selectedOrders.contains(order.orderId)) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (filterDialogOpen) {
        InvoiceFilterDialog(
            startDate = filterStartDate,
            endDate = filterEndDate,
            onApply = { start, end ->
                filterStartDate = start
                filterEndDate = end
                filterDialogOpen = false
            },
            onDismiss = { filterDialogOpen = false }
        )
    }
}
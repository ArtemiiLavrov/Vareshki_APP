package com.example.vareshki

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OrderDetailsScreen(
    viewModel: LoginViewModel,
    orderId: Int,
    onBack: () -> Unit,
    onShowProfile: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val orderDetailsState = remember { mutableStateOf<OrderDetails?>(null) }
    val isLoadingState = remember { mutableStateOf(true) }
    val errorMessageState = remember { mutableStateOf<String?>(null) }
    val isEditingState = remember { mutableStateOf(false) }
    val canteensState = remember { mutableStateOf<List<Canteen>>(emptyList()) }
    val showSelectProductsScreenState = remember { mutableStateOf(false) }

    // Состояния для чекбокса и диалога
    val showActualQuantityDialog = remember { mutableStateOf(false) }
    val selectedProductForActualQuantity = remember { mutableStateOf<OrderProduct?>(null) }
    val actualQuantity = remember { mutableStateOf("") }
    val productsWithActualQuantity = remember { mutableStateOf<Map<Int, Double>>(emptyMap()) }
    val editingProductId = remember { mutableStateOf<Int?>(null) }

    // Состояния для редактирования
    val selectedSenderCanteenState = remember { mutableStateOf<Canteen?>(null) }
    val selectedReceiverCanteenState = remember { mutableStateOf<Canteen?>(null) }
    val editedProductsState = remember { mutableStateOf<List<OrderProduct>>(emptyList()) }
    val customerExpandedState = remember { mutableStateOf(false) }
    val executorExpandedState = remember { mutableStateOf(false) }

    // Состояния для редактирования статуса
    val statusesState = remember { mutableStateOf<List<OrderStatus>>(emptyList()) }
    val selectedStatusState = remember { mutableStateOf<OrderStatus?>(null) }
    val initialStatusState = remember { mutableStateOf<OrderStatus?>(null) }
    val statusExpandedState = remember { mutableStateOf(false) }
    val isStatusChangedState = remember { mutableStateOf(false) }
    val pendingStatusChange = remember { mutableStateOf<OrderStatus?>(null) }

    // Добавляем состояние для хранения фактических количеств
    val actualQuantities = remember { mutableStateMapOf<Int, Double>() }

    // Состояние для подтверждения удаления
    val showDeleteDialog = remember { mutableStateOf(false) }
    val productToDelete = remember { mutableStateOf<OrderProduct?>(null) }

    // Загрузка данных и отметка заказа как просмотренного
    LaunchedEffect(orderId) {
        try {
            viewModel.markOrderAsViewed(orderId)
            canteensState.value = viewModel.fetchCanteens()
            statusesState.value = viewModel.fetchStatuses()
            orderDetailsState.value = viewModel.fetchOrderDetails(orderId)
            // Загружаем фактические количества
            productsWithActualQuantity.value = viewModel.fetchActualQuantities(orderId)
        } catch (e: Exception) {
            errorMessageState.value = "Не удалось загрузить данные: ${e.message}"
        } finally {
            isLoadingState.value = false
        }
    }

    // Инициализация выбранных столовых и статуса после загрузки данных
    LaunchedEffect(canteensState.value, orderDetailsState.value, statusesState.value) {
        if (canteensState.value.isNotEmpty() && orderDetailsState.value != null && selectedSenderCanteenState.value == null) {
            val order = orderDetailsState.value!!
            selectedSenderCanteenState.value = canteensState.value.find { it.address == order.canteenSenderAddress }
            selectedReceiverCanteenState.value = canteensState.value.find { it.address == order.canteenReceiverAddress }
            editedProductsState.value = order.products.toList()

            val currentStatusId = order.status.statusId
            val currentStatus = statusesState.value.find { it.statusId == currentStatusId }
            selectedStatusState.value = currentStatus
            initialStatusState.value = currentStatus
            pendingStatusChange.value = currentStatus
        }
    }

    // Сумма заказанного
    val totalOrdered = editedProductsState.value.sumOf { it.quantity * it.product.priceOfUnit }
    // Сумма отправленного
    val totalSent = editedProductsState.value.sumOf { orderProduct ->
        val sent = productsWithActualQuantity.value[orderProduct.product.productId]
        (sent ?: 0.0) * orderProduct.product.priceOfUnit
    }

    // Проверка, изменился ли статус
    LaunchedEffect(selectedStatusState.value) {
        isStatusChangedState.value = selectedStatusState.value != initialStatusState.value
        if (isEditingState.value) {
            pendingStatusChange.value = selectedStatusState.value
        }
    }

    // Обработка возврата с экрана выбора продуктов
    if (showSelectProductsScreenState.value) {
        SelectProductsScreen(
            viewModel = viewModel,
            onBack = { showSelectProductsScreenState.value = false },
            onProductsSelected = { selectedProducts ->
                val currentProducts = editedProductsState.value.toMutableList()
                selectedProducts.forEach { newProduct ->
                    val existingProduct = currentProducts.find { it.product.productId == newProduct.product.productId }
                    if (existingProduct != null) {
                        currentProducts[currentProducts.indexOf(existingProduct)] =
                            existingProduct.copy(quantity = existingProduct.quantity + newProduct.quantity)
                    } else {
                        currentProducts.add(newProduct)
                    }
                }
                editedProductsState.value = currentProducts.filter { it.quantity > 0 }
                showSelectProductsScreenState.value = false
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Детали заказа", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isEditingState.value) {
                            val saveInteractionSource = remember { MutableInteractionSource() }
                            Card(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable(
                                        interactionSource = saveInteractionSource,
                                        indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                    ) {
                                        coroutineScope.launch {
                                            try {
                                                if (selectedSenderCanteenState.value == null || selectedReceiverCanteenState.value == null) {
                                                    errorMessageState.value = "Выберите обе столовые"
                                                    return@launch
                                                }
                                                if (selectedSenderCanteenState.value == selectedReceiverCanteenState.value) {
                                                    errorMessageState.value = "Столовая-отправитель и столовая-получатель не могут быть одинаковыми"
                                                    return@launch
                                                }
                                                val productsToSave = editedProductsState.value.filter { it.quantity > 0 }
                                                if (productsToSave.isEmpty()) {
                                                    errorMessageState.value = "Выберите хотя бы один продукт с количеством больше 0"
                                                    return@launch
                                                }
                                                val updatedOrder = orderDetailsState.value!!.copy(
                                                    canteenSenderAddress = selectedSenderCanteenState.value!!.address,
                                                    canteenReceiverAddress = selectedReceiverCanteenState.value!!.address,
                                                    products = productsToSave,
                                                    status = pendingStatusChange.value ?: orderDetailsState.value!!.status
                                                )
                                                val success = viewModel.updateOrder(
                                                    orderId = orderId,
                                                    canteenCustomerId = selectedSenderCanteenState.value!!.canteenId,
                                                    canteenExecutorId = selectedReceiverCanteenState.value!!.canteenId,
                                                    products = productsToSave
                                                )
                                                if (success) {
                                                    if (pendingStatusChange.value != initialStatusState.value) {
                                                        val statusSuccess = viewModel.updateOrderStatus(
                                                            orderId = orderId,
                                                            newStatusId = pendingStatusChange.value!!.statusId,
                                                            changedByCanteenId = selectedReceiverCanteenState.value!!.canteenId
                                                        )
                                                        if (!statusSuccess) {
                                                            errorMessageState.value = "Не удалось обновить статус заказа"
                                                            return@launch
                                                        }
                                                    }
                                                    orderDetailsState.value = updatedOrder
                                                    isEditingState.value = false
                                                    initialStatusState.value = pendingStatusChange.value
                                                    isStatusChangedState.value = false
                                                    errorMessageState.value = null
                                                } else {
                                                    errorMessageState.value = "Не удалось обновить заказ"
                                                }
                                            } catch (e: Exception) {
                                                errorMessageState.value = "Ошибка при обновлении заказа: ${e.message}"
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(32.dp), // Закругление 32.dp
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
                                                radius = 80f // Радиус градиента 130f
                                            )
                                        ).padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Сохранить",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            val cancelInteractionSource = remember { MutableInteractionSource() }
                            Card(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable(
                                        interactionSource = cancelInteractionSource,
                                        indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                    ) {
                                        isEditingState.value = false
                                        orderDetailsState.value?.let { order ->
                                            selectedSenderCanteenState.value = canteensState.value.find { it.address == order.canteenSenderAddress }
                                            selectedReceiverCanteenState.value = canteensState.value.find { it.address == order.canteenReceiverAddress }
                                            editedProductsState.value = order.products.toList()
                                            selectedStatusState.value = initialStatusState.value
                                            pendingStatusChange.value = initialStatusState.value
                                            isStatusChangedState.value = false
                                        }
                                    },
                                shape = RoundedCornerShape(32.dp), // Закругление 32.dp
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
                                                radius = 80f // Радиус градиента 130f
                                            )
                                        ).padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Отмена",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            val editInteractionSource = remember { MutableInteractionSource() }
                            Card(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable(
                                        interactionSource = editInteractionSource,
                                        indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                    ) { isEditingState.value = true },
                                shape = RoundedCornerShape(32.dp),
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
                                                radius = 130f
                                            )
                                        ).padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Редактировать",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        if (!isEditingState.value) {
                            IconButton(onClick = { onBack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                            }
                        }
                    }
                }

                if (isLoadingState.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (orderDetailsState.value == null) {
                    Text(
                        text = "Заказ не найден",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Информация о заказе
                    Text("Заказ №${orderDetailsState.value!!.orderId}", fontWeight = FontWeight.Bold)
                    Text("Дата и время создания: ${orderDetailsState.value!!.creationDate} ${orderDetailsState.value!!.creationTime}")

                    // Отображение и редактирование статуса
                    if (!isEditingState.value) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Статус заказа:", modifier = Modifier.weight(1f))
                        ExposedDropdownMenuBox(
                            expanded = statusExpandedState.value,
                            onExpandedChange = { statusExpandedState.value = !statusExpandedState.value },
                            modifier = Modifier
                                .weight(2f)
                                .padding(start = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedStatusState.value?.statusName ?: "Выберите статус",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .height(52.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpandedState.value)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpandedState.value,
                                onDismissRequest = { statusExpandedState.value = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                statusesState.value.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status.statusName, fontSize = 14.sp) },
                                        onClick = {
                                            selectedStatusState.value = status
                                            statusExpandedState.value = false
                                        }
                                    )
                                    }
                                }
                            }
                        }
                    }

                    // Кнопки "Сохранить" и "Отмена" для статуса только на информационном экране
                    if (isStatusChangedState.value && !isEditingState.value) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val saveStatusInteractionSource = remember { MutableInteractionSource() }
                            Card(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable(
                                        interactionSource = saveStatusInteractionSource,
                                        indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                    ) {
                                        coroutineScope.launch {
                                            try {
                                                val success = viewModel.updateOrderStatus(
                                                    orderId = orderId,
                                                    newStatusId = selectedStatusState.value!!.statusId,
                                                    changedByCanteenId = selectedReceiverCanteenState.value!!.canteenId
                                                )
                                                if (success) {
                                                    initialStatusState.value = selectedStatusState.value
                                                    isStatusChangedState.value = false
                                                    orderDetailsState.value = orderDetailsState.value!!.copy(status = selectedStatusState.value!!)
                                                    errorMessageState.value = null
                                                } else {
                                                    errorMessageState.value = "Не удалось обновить статус заказа"
                                                }
                                            } catch (e: Exception) {
                                                errorMessageState.value = "Ошибка при обновлении статуса: ${e.message}"
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(32.dp), // Закругление 32.dp
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
                                                radius = 130f // Радиус градиента 130f
                                            )
                                        ).padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Сохранить",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            val cancelStatusInteractionSource = remember { MutableInteractionSource() }
                            Card(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable(
                                        interactionSource = cancelStatusInteractionSource,
                                        indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                    ) {
                                        selectedStatusState.value = initialStatusState.value
                                        isStatusChangedState.value = false
                                    },
                                shape = RoundedCornerShape(32.dp), // Закругление 32.dp
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
                                                radius = 130f // Радиус градиента 130f
                                            )
                                        ).padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Отмена",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Информация о столовых
                    if (isEditingState.value) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Отправитель:", modifier = Modifier.weight(1f))
                            ExposedDropdownMenuBox(
                                expanded = customerExpandedState.value,
                                onExpandedChange = { customerExpandedState.value = !customerExpandedState.value },
                                modifier = Modifier
                                    .weight(2f)
                                    .padding(start = 8.dp)
                            ) {
                                OutlinedTextField(
                                    value = selectedSenderCanteenState.value?.address ?: "Выберите столовую",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .height(52.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpandedState.value)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = customerExpandedState.value,
                                    onDismissRequest = { customerExpandedState.value = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    canteensState.value.forEach { canteen ->
                                        DropdownMenuItem(
                                            text = { Text(canteen.address, fontSize = 14.sp) },
                                            onClick = {
                                                selectedSenderCanteenState.value = canteen
                                                customerExpandedState.value = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Получатель:", modifier = Modifier.weight(1f))
                            ExposedDropdownMenuBox(
                                expanded = executorExpandedState.value,
                                onExpandedChange = { executorExpandedState.value = !executorExpandedState.value },
                                modifier = Modifier
                                    .weight(2f)
                                    .padding(start = 8.dp)
                            ) {
                                OutlinedTextField(
                                    value = selectedReceiverCanteenState.value?.address ?: "Выберите столовую",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .height(52.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = executorExpandedState.value)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = executorExpandedState.value,
                                    onDismissRequest = { executorExpandedState.value = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    canteensState.value.forEach { canteen ->
                                        DropdownMenuItem(
                                            text = { Text(canteen.address, fontSize = 14.sp) },
                                            onClick = {
                                                selectedReceiverCanteenState.value = canteen
                                                executorExpandedState.value = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Отправитель: ${orderDetailsState.value!!.canteenSenderAddress}")
                        Text("Получатель: ${orderDetailsState.value!!.canteenReceiverAddress}")
                    }

                    // Обёртка для шапки и списка продуктов
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        // Липкая шапка и список продуктов
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stickyHeader {
                            Row(
                                modifier = Modifier
                                        .fillMaxWidth()
                                    .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Продукт",
                                        modifier = Modifier.weight(2f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Заказано",
                                        modifier = Modifier.weight(1.5f),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Box(
                                        modifier = Modifier.weight(1.5f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isEditingState.value) {
                                            IconButton(
                                                onClick = { showSelectProductsScreenState.value = true },
                                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить продукты")
                                            }
                                        } else if (!isEditingState.value) {
                                Text(
                                                text = "Отправлено",
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                            // Список продуктов
                        items(editedProductsState.value) { orderProduct ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                    // Название продукта
                                Text(
                                    text = "${orderProduct.product.name} (${orderProduct.product.priceOfUnit} за ед.)",
                                        modifier = Modifier.weight(2f)
                                    )
                                    
                                    // Заказанное количество
                                    Text(
                                        text = orderProduct.quantity.toString(),
                                        modifier = Modifier.weight(1.5f),
                                        textAlign = TextAlign.Center
                                    )
                                    
                                if (isEditingState.value) {
                                        // Только иконка удаления
                                        IconButton(
                                            onClick = {
                                                productToDelete.value = orderProduct
                                                showDeleteDialog.value = true
                                            },
                                            modifier = Modifier.weight(1.5f)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить позицию")
                                        }
                                    } else {
                                        // Отправленное количество (старая логика)
                                        Box(
                                            modifier = Modifier.weight(1.5f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val sentQuantity = productsWithActualQuantity.value[orderProduct.product.productId]
                                            if (sentQuantity != null) {
                                                if (editingProductId.value == orderProduct.product.productId) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(80.dp)
                                                            .clickable { editingProductId.value = null }
                                                    ) {
                                    OutlinedTextField(
                                                            value = sentQuantity.toString(),
                                        onValueChange = { newValue ->
                                            val quantity = newValue.toDoubleOrNull() ?: 0.0
                                                                        if (quantity > 0) {
                                                                            coroutineScope.launch {
                                                                                val success = viewModel.updateActualQuantity(
                                                                                    orderId = orderId,
                                                                                    productId = orderProduct.product.productId,
                                                                                    actualQuantity = quantity,
                                                                                    employeeId = viewModel.getEmployeeId()
                                                                                )
                                                                                if (success) {
                                                                                    productsWithActualQuantity.value = productsWithActualQuantity.value +
                                                                                        (orderProduct.product.productId to quantity)
                                                                                } else {
                                                                                    errorMessageState.value = "Не удалось сохранить фактическое количество"
                                                                                }
                                                                            }
                                                                        }
                                                                    },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textStyle = TextStyle(textAlign = TextAlign.Center),
                                                            singleLine = true,
                                                            enabled = true,
                                                            readOnly = false,
                                                            isError = false,
                                                            colors = TextFieldDefaults.colors(
                                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                                disabledTextColor = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = sentQuantity.toString(),
                                                        modifier = Modifier
                                                            .clickable { editingProductId.value = orderProduct.product.productId }
                                                            .padding(8.dp),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            } else {
                                                Checkbox(
                                                    checked = false,
                                                    onCheckedChange = { checked ->
                                                        if (checked) {
                                                            selectedProductForActualQuantity.value = orderProduct
                                                            actualQuantity.value = orderProduct.quantity.toString()
                                                            showActualQuantityDialog.value = true
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

                    // Диалог для ввода фактического количества
                    if (showActualQuantityDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showActualQuantityDialog.value = false },
                            title = { Text("Введите фактическое количество") },
                            text = {
                                Column {
                                    Text("Продукт: ${selectedProductForActualQuantity.value?.product?.name}")
                                    Text("Заказанное количество: ${selectedProductForActualQuantity.value?.quantity}")
                                    OutlinedTextField(
                                        value = actualQuantity.value,
                                        onValueChange = { actualQuantity.value = it },
                                        label = { Text("Фактическое количество") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(textAlign = TextAlign.Center),
                                        singleLine = true,
                                        enabled = true,
                                        readOnly = false,
                                        isError = false,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                        onClick = {
                                        val quantity = actualQuantity.value.toDoubleOrNull() ?: 0.0
                                        if (quantity > 0) {
                                            selectedProductForActualQuantity.value?.product?.productId?.let { productId ->
                                                coroutineScope.launch {
                                                    val success = viewModel.updateActualQuantity(
                                                        orderId = orderId,
                                                        productId = productId,
                                                        actualQuantity = quantity,
                                                        employeeId = viewModel.getEmployeeId()
                                                    )
                                                    if (success) {
                                                        productsWithActualQuantity.value = productsWithActualQuantity.value + (productId to quantity)
                                                    } else {
                                                        errorMessageState.value = "Не удалось сохранить фактическое количество"
                                                    }
                                                }
                                            }
                                        }
                                        showActualQuantityDialog.value = false
                                    }
                                ) {
                                    Text("Сохранить")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showActualQuantityDialog.value = false
                                    }
                                ) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }

                    // Диалог подтверждения удаления
                    if (showDeleteDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog.value = false },
                            title = { Text("Удалить позицию?") },
                            text = { Text("Вы действительно хотите удалить продукт из заказа?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    val product = productToDelete.value
                                    if (product != null) {
                                        val updated = editedProductsState.value.toMutableList()
                                        updated.remove(product)
                                        editedProductsState.value = updated
                                        coroutineScope.launch {
                                            viewModel.deleteActualQuantity(orderId, product.product.productId)
                                        }
                                    }
                                    showDeleteDialog.value = false
                                }) {
                                    Text("Удалить")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog.value = false }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }

                    // Сумма заказанного
                    Text(
                        text = "Сумма заказанного: $totalOrdered",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Сумма отправленного
                    Text(
                        text = "Сумма отправленного: $totalSent",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Кнопка редактирования профиля
            /*IconButton(
                onClick = { onShowProfile() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = "Редактировать профиль")
            }*/
        }
    }
}
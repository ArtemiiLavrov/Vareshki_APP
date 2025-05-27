package com.example.vareshki

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PanoramaFishEye
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.ripple.rememberRipple

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OrderDetailsScreen(
    context: Context,
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

    // Состояние для хранения фактических количеств и статуса принятия
    val productStatuses = remember { mutableStateMapOf<Int, ProductStatus>() }

    // Состояние для подтверждения удаления
    val showDeleteDialog = remember { mutableStateOf(false) }
    val productToDelete = remember { mutableStateOf<OrderProduct?>(null) }

    // Проверка, является ли сотрудник заказчиком
    val isCustomerEmployee = remember { mutableStateOf(false) }

    // Отображение Toast при изменении errorMessageState
    LaunchedEffect(errorMessageState.value) {
        errorMessageState.value?.let { message ->
            ToastUtils.showToast(context, message)
            errorMessageState.value = null
        }
    }

    // Загрузка данных и отметка заказа как просмотренного
    LaunchedEffect(orderId) {
        try {
            viewModel.markOrderAsViewed(orderId)
            canteensState.value = viewModel.fetchCanteens()
            statusesState.value = viewModel.fetchStatuses()
            orderDetailsState.value = viewModel.fetchOrderDetails(orderId)
            val actualData = viewModel.fetchActualQuantitiesWithAcceptance(orderId)
            println("Fetched actual quantities: $actualData") // Логирование
            productsWithActualQuantity.value = actualData.mapValues { it.value.quantity }
            actualData.forEach { (productId, status) ->
                productStatuses[productId] = status
                println("Product $productId: quantity=${status.quantity}, isAccepted=${status.isAccepted}")
            }
            isCustomerEmployee.value = viewModel.isEmployeeFromCustomerCanteen(orderId, viewModel.getEmployeeId())
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
    val totalOrdered by remember { derivedStateOf {
        editedProductsState.value.sumOf { it.quantity * it.product.priceOfUnit }
    } }
    // Сумма отправленного
    val totalSent by remember { derivedStateOf {
        editedProductsState.value.sumOf { orderProduct ->
            val sent = productsWithActualQuantity.value[orderProduct.product.productId]
            (sent ?: 0.0) * orderProduct.product.priceOfUnit
        }
    } }

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
                            Card(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(),
                                        onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    if (selectedSenderCanteenState.value == null || selectedReceiverCanteenState.value == null) {
                                                        errorMessageState.value = "Выберите обе столовые"
                                                        return@launch
                                                    }
                                                    if (selectedSenderCanteenState.value == selectedReceiverCanteenState.value) {
                                                        errorMessageState.value = "Столовая-заказчик и столовая-исполнитель не могут быть одинаковыми"
                                                        return@launch
                                                    }
                                                    val productsToSave = editedProductsState.value.filter { it.quantity > 0 }
                                                    if (productsToSave.isEmpty()) {
                                                        errorMessageState.value = "Выберите хотя бы один продукт с количеством больше 0"
                                                        return@launch
                                                    }
                                                    val updatedOrder = (pendingStatusChange.value ?: orderDetailsState.value?.status ?: initialStatusState.value)?.let {
                                                        orderDetailsState.value?.copy(
                                                            canteenSenderAddress = selectedSenderCanteenState.value!!.address,
                                                            canteenReceiverAddress = selectedReceiverCanteenState.value!!.address,
                                                            products = productsToSave,
                                                            status = it
                                                        )
                                                    } ?: return@launch
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
                                                    } else {
                                                        errorMessageState.value = "Не удалось обновить заказ"
                                                    }
                                                } catch (e: Exception) {
                                                    errorMessageState.value = "Ошибка при обновлении заказа: ${e.message}"
                                                }
                                            }
                                        }
                                    ),
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
                                                radius = 80f
                                            )
                                        )
                                        .padding(8.dp),
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

                            Card(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(),
                                        onClick = {
                                            isEditingState.value = false
                                            orderDetailsState.value?.let { order ->
                                                selectedSenderCanteenState.value = canteensState.value.find { it.address == order.canteenSenderAddress }
                                                selectedReceiverCanteenState.value = canteensState.value.find { it.address == order.canteenReceiverAddress }
                                                editedProductsState.value = order.products.toList()
                                                selectedStatusState.value = initialStatusState.value
                                                pendingStatusChange.value = initialStatusState.value
                                                isStatusChangedState.value = false
                                            }
                                        }
                                    ),
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
                                                radius = 80f
                                            )
                                        )
                                        .padding(8.dp),
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
                            Card(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(),
                                        onClick = { isEditingState.value = true }
                                    ),
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
                                        )
                                        .padding(8.dp),
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
                    Text("Заказ №${orderDetailsState.value!!.orderId}", fontWeight = FontWeight.Bold)
                    Text("Дата и время создания: ${orderDetailsState.value!!.creationDate} ${orderDetailsState.value!!.creationTime}")

                    // Отображение и редактирование статуса
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
                                val filteredStatuses = if (isCustomerEmployee.value) {
                                    statusesState.value
                                } else {
                                    statusesState.value.filter { it.statusName != "Исполнен" }
                                }
                                filteredStatuses.forEach { status ->
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

                    if (isStatusChangedState.value && !isEditingState.value) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val saveInteractionSource = remember { MutableInteractionSource() }
                            Card(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clickable(
                                        interactionSource = saveInteractionSource,
                                        indication = ripple(),
                                        onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    val success = viewModel.updateOrderStatus(
                                                        orderId = orderId,
                                                        newStatusId = selectedStatusState.value?.statusId ?: 0,
                                                        changedByCanteenId = selectedReceiverCanteenState.value?.canteenId ?: 0
                                                    )
                                                    if (success) {
                                                        initialStatusState.value = selectedStatusState.value
                                                        isStatusChangedState.value = false
                                                        orderDetailsState.value =
                                                            selectedStatusState.value?.let { orderDetailsState.value?.copy(status = it) }
                                                    } else {
                                                        errorMessageState.value = "Не удалось обновить статус заказа"
                                                    }
                                                } catch (e: Exception) {
                                                    errorMessageState.value = "Ошибка при обновлении статуса: ${e.message}"
                                                }
                                            }
                                        }
                                    ),
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
                                                radius = 80f
                                            )
                                        )
                                        .padding(8.dp),
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
                                        indication = ripple(),
                                        onClick = {
                                            selectedStatusState.value = initialStatusState.value
                                            isStatusChangedState.value = false
                                        }
                                    ),
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
                                                )
                                            )
                                        )
                                        .padding(8.dp),
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

                    if (isEditingState.value) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Заказчик:", modifier = Modifier.weight(1f))
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
                            Text("Исполнитель:", modifier = Modifier.weight(1f))
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
                                            IconButton(
                                                onClick = { showSelectProductsScreenState.value = true },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Добавить продукты")
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
                                            text = "${orderProduct.product.name}",
                                            modifier = Modifier.weight(2f)
                                        )

                                        // Заказанное количество
                                        Text(
                                            text = orderProduct.quantity.toString(),
                                            modifier = Modifier.weight(1.5f),
                                            textAlign = TextAlign.Center
                                        )
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
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Заказчик: ${orderDetailsState.value!!.canteenSenderAddress}")
                        Text("Исполнитель: ${orderDetailsState.value!!.canteenReceiverAddress}")
                        // Таблица продуктов
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
                            val scrollState = rememberScrollState()
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                stickyHeader {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "Продукт",
                                            modifier = Modifier
                                                .weight(2f)
                                                .padding(end = 8.dp),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            modifier = Modifier
                                                .weight(3f)
                                                .horizontalScroll(scrollState),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Заказано",
                                                modifier = Modifier.width(80.dp),
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = "Отправлено",
                                                modifier = Modifier.width(100.dp),
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                            if (isCustomerEmployee.value && !isEditingState.value) {
                                                Text(
                                                    text = "Принято",
                                                    modifier = Modifier.width(80.dp),
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                        if (isEditingState.value) {
                                            Box(
                                                modifier = Modifier.width(40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(
                                                    onClick = { showSelectProductsScreenState.value = true },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Добавить продукты")
                                                }
                                            }
                                        }
                                    }
                                }
                                items(editedProductsState.value) { orderProduct ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${orderProduct.product.name}",
                                            modifier = Modifier
                                                .weight(2f)
                                                .padding(end = 8.dp),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            modifier = Modifier
                                                .weight(3f)
                                                .horizontalScroll(scrollState),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = orderProduct.quantity.toString(),
                                                modifier = Modifier.width(80.dp).padding(top = 14.dp),
                                                textAlign = TextAlign.Center
                                            )
                                            Box(
                                                modifier = Modifier.width(100.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val sentQuantity = productsWithActualQuantity.value[orderProduct.product.productId]
                                                if (sentQuantity != null) {
                                                    if (editingProductId.value == orderProduct.product.productId) {
                                                        OutlinedTextField(
                                                            value = sentQuantity.toString(),
                                                            onValueChange = { newValue ->
                                                                val quantity = newValue.toDoubleOrNull() ?: 0.0
                                                                if (quantity >= 0) {
                                                                    coroutineScope.launch {
                                                                        val success = viewModel.updateActualQuantity(
                                                                            orderId = orderId,
                                                                            productId = orderProduct.product.productId,
                                                                            actualQuantity = quantity,
                                                                            employeeId = viewModel.getEmployeeId()
                                                                        )
                                                                        if (success) {
                                                                            // Перезагружаем данные после успешного обновления
                                                                            val updatedData = viewModel.fetchActualQuantitiesWithAcceptance(orderId)
                                                                            productsWithActualQuantity.value = updatedData.mapValues { it.value.quantity }
                                                                            updatedData.forEach { (productId, status) ->
                                                                                productStatuses[productId] = status
                                                                            }
                                                                        } else {
                                                                            errorMessageState.value = "Не удалось сохранить фактическое количество"
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textStyle = TextStyle(textAlign = TextAlign.Center),
                                                            singleLine = true
                                                        )
                                                    } else {
                                                        Text(
                                                            text = sentQuantity.toString(),
                                                            modifier = Modifier
                                                                .clickable { editingProductId.value = orderProduct.product.productId }
                                                                .padding(start = 8.dp, end = 8.dp, top = 14.dp),
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
                                            if (isCustomerEmployee.value && !isEditingState.value) {
                                                val status = productStatuses[orderProduct.product.productId]
                                                IconButton(
                                                    onClick = {
                                                        val newStatus = when (status?.isAccepted) {
                                                            null -> true
                                                            true -> false
                                                            false -> null
                                                        }
                                                        coroutineScope.launch {
                                                            val success = viewModel.updateAcceptanceStatus(
                                                                orderId = orderId,
                                                                productId = orderProduct.product.productId,
                                                                isAccepted = newStatus,
                                                                employeeId = viewModel.getEmployeeId()
                                                            )
                                                            if (success) {
                                                                productStatuses[orderProduct.product.productId] = ProductStatus(
                                                                    orderProduct.product.productId,
                                                                    productsWithActualQuantity.value[orderProduct.product.productId] ?: 0.0,
                                                                    newStatus
                                                                )
                                                            } else {
                                                                errorMessageState.value = "Не удалось обновить статус принятия"
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.width(80.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = when (status?.isAccepted) {
                                                            true -> Icons.Default.Check
                                                            false -> Icons.Default.Close
                                                            null -> Icons.Default.PanoramaFishEye
                                                        },
                                                        contentDescription = "Статус принятия",
                                                        tint = when (status?.isAccepted) {
                                                            true -> Color.Green
                                                            false -> MaterialTheme.colorScheme.error
                                                            null -> MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                }
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
                                        singleLine = true
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val quantity = actualQuantity.value.toDoubleOrNull() ?: 0.0
                                        if (quantity >= 0) {
                                            selectedProductForActualQuantity.value?.product?.productId?.let { productId ->
                                                coroutineScope.launch {
                                                    val success = viewModel.updateActualQuantity(
                                                        orderId = orderId,
                                                        productId = productId,
                                                        actualQuantity = quantity,
                                                        employeeId = viewModel.getEmployeeId()
                                                    )
                                                    if (success) {
                                                        // Локально обновляем состояние для немедленного отображения
                                                        productsWithActualQuantity.value = productsWithActualQuantity.value + (productId to quantity)
                                                        productStatuses[productId] = ProductStatus(productId, quantity, null)
                                                        // Перезагружаем данные с сервера
                                                        val updatedData = viewModel.fetchActualQuantitiesWithAcceptance(orderId)
                                                        productsWithActualQuantity.value = updatedData.mapValues { it.value.quantity }
                                                        updatedData.forEach { (productId, status) ->
                                                            productStatuses[productId] = status
                                                        }
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
                                    onClick = { showActualQuantityDialog.value = false }
                                ) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }

                    // Диалог для удаления продукта
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
                                            productStatuses.remove(product.product.productId)
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
                }
            }
        }
    }
}
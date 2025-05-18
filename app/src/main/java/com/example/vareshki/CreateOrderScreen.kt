package com.example.vareshki

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
private fun QuantityEditDialog(
    product: Product,
    currentQuantity: Double,
    onDismiss: () -> Unit,
    onQuantityChanged: (Double) -> Unit
) {
    var newQuantity by remember { mutableStateOf(currentQuantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить количество") },
        text = {
            Column {
                Text(product.name)
                OutlinedTextField(
                    value = newQuantity,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.toDoubleOrNull() != null) {
                            newQuantity = value
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val quantity = newQuantity.toDoubleOrNull() ?: 0.0
                    if (quantity > 0) {
                        onQuantityChanged(quantity)
                    }
                    onDismiss()
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun ProductQuantity(
    orderProduct: OrderProduct,
    modifier: Modifier = Modifier,
    onQuantityChanged: (List<OrderProduct>) -> Unit
) {
    var quantity by remember { mutableStateOf(orderProduct.quantity.toString()) }
    OutlinedTextField(
        value = quantity,
        onValueChange = { value ->
            if (value.isEmpty() || value.toDoubleOrNull() != null) {
                quantity = value
                val newQuantity = value.toDoubleOrNull() ?: 0.0
                onQuantityChanged(listOf(orderProduct.copy(quantity = newQuantity)))
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        ),
        interactionSource = remember { MutableInteractionSource() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    viewModel: LoginViewModel,
    onOrderCreated: () -> Unit,
    onSelectProducts: (List<OrderProduct>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var canteens by remember { mutableStateOf<List<Canteen>>(emptyList()) }
    var selectedCustomerCanteen by remember { mutableStateOf<Canteen?>(null) }
    var selectedExecutorCanteen by remember { mutableStateOf<Canteen?>(null) }
    var orderProducts by remember { mutableStateOf<List<OrderProduct>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var customerExpanded by remember { mutableStateOf(false) }
    var executorExpanded by remember { mutableStateOf(false) }
    var showSelectProductsScreen by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Int?>(null) }

    val totalPrice by remember { derivedStateOf {
        orderProducts.sumOf { it.quantity * it.product.priceOfUnit }
    } }

    val visibleProducts = orderProducts.filter { it.quantity > 0 }

    LaunchedEffect(Unit) {
        try {
            canteens = viewModel.fetchCanteens()
        } catch (e: Exception) {
            errorMessage = "Не удалось загрузить данные: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    if (showSelectProductsScreen) {
        SelectProductsScreen(
            viewModel = viewModel,
            onBack = { showSelectProductsScreen = false },
            onProductsSelected = { selectedProducts ->
                val mergedProducts = orderProducts.toMutableList()
                selectedProducts.forEach { newProduct ->
                    val existingProductIndex = mergedProducts.indexOfFirst { it.product.productId == newProduct.product.productId }
                    if (existingProductIndex != -1) {
                        mergedProducts[existingProductIndex] = mergedProducts[existingProductIndex].copy(
                            quantity = mergedProducts[existingProductIndex].quantity + newProduct.quantity
                        )
                    } else {
                        mergedProducts.add(newProduct)
                    }
                }
                orderProducts = mergedProducts
                showSelectProductsScreen = false
                onSelectProducts(mergedProducts)
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                    //.padding(bottom = 50.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Создать заказ", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Заказчик:",
                            modifier = Modifier.weight(0.35f),
                            maxLines = 2,
                            softWrap = true,
                            fontSize = 16.sp
                        )
                        ExposedDropdownMenuBox(
                            expanded = customerExpanded,
                            onExpandedChange = { customerExpanded = !customerExpanded },
                            modifier = Modifier
                                .weight(0.65f)
                                .padding(start = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedCustomerCanteen?.address ?: "Выберите столовую",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor(),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded)
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
                                expanded = customerExpanded,
                                onDismissRequest = { customerExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                canteens.forEach { canteen ->
                                    DropdownMenuItem(
                                        text = { Text(canteen.address) },
                                        onClick = {
                                            selectedCustomerCanteen = canteen
                                            customerExpanded = false
                                            if (selectedExecutorCanteen == canteen) {
                                                Toast.makeText(context, "Нельзя выбрать одинаковые столовые", Toast.LENGTH_SHORT).show()
                                                selectedCustomerCanteen = null
                                            }
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
                        Text(
                            text = "Исполнитель:",
                            modifier = Modifier.weight(0.35f),
                            maxLines = 2,
                            softWrap = true,
                            fontSize = 16.sp
                        )
                        ExposedDropdownMenuBox(
                            expanded = executorExpanded,
                            onExpandedChange = { executorExpanded = !executorExpanded },
                            modifier = Modifier
                                .weight(0.65f)
                                .padding(start = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedExecutorCanteen?.address ?: "Выберите столовую",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor(),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = executorExpanded)
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
                                expanded = executorExpanded,
                                onDismissRequest = { executorExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                canteens.forEach { canteen ->
                                    DropdownMenuItem(
                                        text = { Text(canteen.address) },
                                        onClick = {
                                            selectedExecutorCanteen = canteen
                                            executorExpanded = false
                                            if (selectedCustomerCanteen == canteen) {
                                                Toast.makeText(context, "Нельзя выбрать одинаковые столовые", Toast.LENGTH_SHORT).show()
                                                selectedExecutorCanteen = null
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (visibleProducts.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Продукт",
                                modifier = Modifier.weight(0.565f),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.weight(0.435f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Количество",
                                    modifier = Modifier.weight(0.2f),
                                    textAlign = TextAlign.Start,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { showSelectProductsScreen = true },
                                    modifier = Modifier.size(24.dp).weight(0.085f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Добавить продукты",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.heightIn(max = 420.dp)) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 396.dp)
                            ) {
                                items(visibleProducts) { orderProduct ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(0.6f)
                                                .fillMaxWidth()
                                        ) {
                                            Text(
                                                text = orderProduct.product.name,
                                                maxLines = 2,
                                                overflow = TextOverflow.Visible
                                            )
                                            Text(
                                                text = "${orderProduct.product.priceOfUnit} за ед.",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
                                        ProductQuantity(
                                            orderProduct = orderProduct,
                                            modifier = Modifier.weight(0.3f),
                                            onQuantityChanged = { updatedProduct ->
                                                val updatedProducts = orderProducts.map {
                                                    if (it.product.productId == updatedProduct[0].product.productId) {
                                                        updatedProduct[0]
                                                    } else {
                                                        it
                                                    }
                                                }
                                                orderProducts = updatedProducts
                                                onSelectProducts(updatedProducts)
                                            }
                                        )
                                        IconButton(
                                            onClick = {
                                                showDeleteDialog = orderProduct.product.productId
                                            },
                                            modifier = Modifier.padding(start = 12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Удалить продукт",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    if (showDeleteDialog == orderProduct.product.productId) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteDialog = null },
                                            title = { Text("Удалить продукт?") },
                                            text = { Text("Вы уверены, что хотите удалить этот продукт из заказа?") },
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    orderProducts = orderProducts.filter { it.product.productId != orderProduct.product.productId }
                                                    showDeleteDialog = null
                                                }) {
                                                    Text("Да")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showDeleteDialog = null }) {
                                                    Text("Нет")
                                                }
                                            }
                                        )
                                    }
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
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 140.dp)
                                .wrapContentHeight()
                                .align(Alignment.CenterHorizontally),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Здесь пока нет продуктов",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Самое время их добавить",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                            Card(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(top = 8.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                    ) { showSelectProductsScreen = true },
                                shape = RoundedCornerShape(16.dp),
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
                                                radius = 300f
                                            )
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Добавить продукты",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Добавить продукты",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (visibleProducts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom=16.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Сумма: $totalPrice",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Card(
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        ),
                                        radius = 300f
                                    )
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                ) {
                                    if (selectedCustomerCanteen == null || selectedExecutorCanteen == null) {
                                        errorMessage = "Выберите обе столовые"
                                        return@clickable
                                    }
                                    if (selectedCustomerCanteen == selectedExecutorCanteen) {
                                        errorMessage = "Столовая-отправитель и столовая-получатель не могут быть одинаковыми"
                                        return@clickable
                                    }
                                    val productsToOrder = visibleProducts
                                    if (productsToOrder.isEmpty()) {
                                        errorMessage = "Выберите хотя бы один продукт с количеством больше 0"
                                        return@clickable
                                    }
                                    coroutineScope.launch {
                                        try {
                                            val success = viewModel.createOrder(
                                                canteenCustomerId = selectedCustomerCanteen!!.canteenId,
                                                canteenExecutorId = selectedExecutorCanteen!!.canteenId,
                                                products = productsToOrder
                                            )
                                            if (success) {
                                                onOrderCreated()
                                            } else {
                                                errorMessage = "Не удалось создать заказ"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Ошибка при создании заказа: ${e.message}"
                                        }
                                    }
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Создать заказ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
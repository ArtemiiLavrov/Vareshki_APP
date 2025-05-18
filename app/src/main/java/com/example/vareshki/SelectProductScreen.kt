package com.example.vareshki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectProductsScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onProductsSelected: (List<OrderProduct>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var orderProducts by remember { mutableStateOf<List<OrderProduct>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    var selectedLetter by remember { mutableStateOf<String?>(null) }

    // Загрузка продуктов
    LaunchedEffect(Unit) {
        try {
            products = viewModel.fetchProducts().sortedBy { it.name.lowercase() }
            orderProducts = products.map { OrderProduct(it, 0.0) }
        } catch (e: Exception) {
            errorMessage = "Не удалось загрузить продукты: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Фильтрация продуктов по поисковому запросу
    val filteredProducts = if (searchQuery.isEmpty()) {
        orderProducts
    } else {
        orderProducts.filter { it.product.name.lowercase().contains(searchQuery.lowercase()) }
    }

    // Группировка продуктов по первой букве
    val groupedProducts = filteredProducts.groupBy { product ->
        product.product.name.firstOrNull()?.uppercase() ?: ""
    }.filter { it.key.isNotEmpty() }.toSortedMap()

    // Русский алфавит для указателя
    val russianAlphabet = ('А'..'Я').map { it.toString() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Прокручиваемый контент
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .padding(top = 40.dp, bottom = 30.dp) // Отступ снизу для кнопки "Готово"
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Строка поиска
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск по названию") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                }
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Неизвестная ошибка",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (groupedProducts.isEmpty()) {
                Text(
                    text = "Нет доступных продуктов",
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                // Список продуктов с разделителями
                LazyColumn(
                    state = scrollState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp) // Ограничиваем высоту списка
                ) {
                    groupedProducts.forEach { (letter, productsInGroup) ->
                        // Разделитель для буквы
                        item {
                            Text(
                                text = letter,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(8.dp)
                            )
                        }
                        // Продукты в группе
                        items(productsInGroup, key = { it.product.productId }) { orderProduct ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${orderProduct.product.name} (${orderProduct.product.priceOfUnit} руб. за ${orderProduct.product.unitOfMeasurement.lowercase()})",
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = if (orderProduct.quantity == 0.0) "" else orderProduct.quantity.toString(),
                                    onValueChange = { newValue ->
                                        val quantity = newValue.toDoubleOrNull() ?: 0.0
                                        orderProducts = orderProducts.map {
                                            if (it.product.productId == orderProduct.product.productId) {
                                                it.copy(quantity = quantity)
                                            } else {
                                                it
                                            }
                                        }
                                    },
                                    modifier = Modifier.width(100.dp),
                                    label = { Text("Кол-во") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF333333),
                                        unfocusedContainerColor = Color(0xFF333333),
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.White,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Кнопка "Готово", зафиксированная внизу над навигационным баром
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()-30.dp, top = 0.dp) // Учитываем нижний бар
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                    enabled = !isSaving
                ) {
                    coroutineScope.launch {
                        try {
                            isSaving = true
                            // Имитация асинхронной операции
                            delay(500)
                            onProductsSelected(orderProducts)
                        } finally {
                            isSaving = false
                        }
                    }
                },
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
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
                    ).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp),
                        color = Color.White
                    )
                }
                Text(
                    text = "Готово",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Алфавитный указатель справа
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            russianAlphabet.forEach { letter ->
                Text(
                    text = letter,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .clickable {
                            selectedLetter = letter
                            val firstIndex = groupedProducts.keys.indexOfFirst { it == letter }
                            if (firstIndex != -1) {
                                coroutineScope.launch {
                                    scrollState.animateScrollToItem(firstIndex)
                                }
                            }
                        },
                    color = if (selectedLetter == letter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Кнопка "Назад" в левом верхнем углу
        IconButton(
            onClick = { onBack() },
            modifier = Modifier
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
        }
    }
}
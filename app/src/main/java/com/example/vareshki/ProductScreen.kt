package com.example.vareshki

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onShowProfile: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit, // Новый callback для редактирования
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val products by viewModel.products.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            viewModel.fetchProducts()
        } catch (e: Exception) {
            errorMessage = "Не удалось загрузить продукты: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Список продуктов", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { onAddProduct() }) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить продукт")
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Неизвестная ошибка",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textAlign = TextAlign.Center
                )
            } else if (products.isEmpty()) {
                Text(
                    text = "Нет доступных продуктов",
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
                    items(products, key = { it.productId }) { product ->
                        ProductCard(
                            product = product,
                            onDelete = {
                                coroutineScope.launch {
                                    val success = viewModel.deleteProduct(product.productId)
                                    if (!success) {
                                        errorMessage = "Не удалось удалить продукт"
                                    }
                                }
                            },
                            onEdit = { onEditProduct(product) } // Передаем продукт для редактирования
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = { onShowProfile() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Person, contentDescription = "Редактировать профиль")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductCard(
    product: Product,
    onDelete: () -> Unit,
    onEdit: (Product) -> Unit, // Новый callback для редактирования
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .wrapContentSize(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .combinedClickable(
                        onClick = { onEdit(product) }, // Переход на экран редактирования при клике
                        onLongClick = { showMenu = true }
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Продукт: ${product.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Цена за единицу: ${product.priceOfUnit} руб.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Единица измерения: ${product.unitOfMeasurement}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            DropdownMenuItem(
                text = { Text("Удалить", color = Color.Red) },
                onClick = {
                    onDelete()
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Отмена") },
                onClick = { showMenu = false }
            )
        }
    }
}
package com.example.vareshki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    viewModel: LoginViewModel,
    product: Product,
    onBack: () -> Unit,
    onProductUpdated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var productName by remember { mutableStateOf(product.name) }
    var pricePerUnit by remember { mutableStateOf(product.priceOfUnit.toString()) }
    var unitsOfMeasurement by remember { mutableStateOf<List<UnitOfMeasurement>>(emptyList()) }
    var selectedUnit by remember { mutableStateOf<UnitOfMeasurement?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            unitsOfMeasurement = viewModel.fetchUnitsOfMeasurement()
            selectedUnit = unitsOfMeasurement.find { it.measurementName == product.unitOfMeasurement }
                ?: unitsOfMeasurement.firstOrNull()
        } catch (e: Exception) {
            errorMessage = "Не удалось загрузить единицы измерения: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(top = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Text("Редактировать продукт", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { /* Пустой */ }, enabled = false) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Transparent)
            }
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(text = errorMessage ?: "Неизвестная ошибка", color = MaterialTheme.colorScheme.error)
        } else {
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Название продукта") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = pricePerUnit,
                onValueChange = { pricePerUnit = it },
                label = { Text("Цена за единицу") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text("Единица измерения", fontSize = 16.sp)
                val unitInteractionSource = remember { MutableInteractionSource() }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = unitInteractionSource,
                            indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                            enabled = true
                        ) {
                            expanded = true
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
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = selectedUnit?.measurementName ?: "Выберите единицу измерения",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    unitsOfMeasurement.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit.measurementName) },
                            onClick = {
                                selectedUnit = unit
                                expanded = false
                            }
                        )
                    }
                }
            }

            val saveInteractionSource = remember { MutableInteractionSource() }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = saveInteractionSource,
                        indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                        enabled = !isSaving
                    ) {
                        if (productName.isBlank()) {
                            errorMessage = "Название продукта не может быть пустым"
                            return@clickable
                        }
                        if (pricePerUnit.isBlank() || pricePerUnit.toDoubleOrNull() == null) {
                            errorMessage = "Цена должна быть числом"
                            return@clickable
                        }
                        if (selectedUnit == null) {
                            errorMessage = "Выберите единицу измерения"
                            return@clickable
                        }

                        coroutineScope.launch {
                            try {
                                isSaving = true
                                val success = viewModel.updateProduct(
                                    productId = product.productId,
                                    name = productName,
                                    priceOfUnit = pricePerUnit.toDouble(),
                                    unitOfMeasurementId = selectedUnit!!.measurementId
                                )
                                if (success) {
                                    viewModel.fetchProducts()
                                    onProductUpdated()
                                } else {
                                    errorMessage = "Не удалось обновить продукт"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Не удалось обновить продукт: ${e.message}"
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
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp),
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    Text(
                        text = "Сохранить",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
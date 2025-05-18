package com.example.vareshki

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class UnitOfMeasurement(val measurementId: Int, val measurementName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onProductAdded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var productName by remember { mutableStateOf("") }
    var pricePerUnit by remember { mutableStateOf("") }
    var unitsOfMeasurement by remember { mutableStateOf<List<UnitOfMeasurement>>(emptyList()) }
    var selectedUnit by remember { mutableStateOf<UnitOfMeasurement?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            unitsOfMeasurement = viewModel.fetchUnitsOfMeasurement()
            if (unitsOfMeasurement.isNotEmpty()) {
                selectedUnit = unitsOfMeasurement.first()
            }
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
            Text("Добавить продукт", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { /* Пустой */ }, enabled = false) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Transparent)
            }
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(text = errorMessage ?: "Неизвестная ошибка", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
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
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedUnit?.measurementName ?: "Выберите единицу измерения")
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

            Button(
                onClick = {
                    if (productName.isBlank()) {
                        errorMessage = "Название продукта не может быть пустым"
                        return@Button
                    }
                    if (pricePerUnit.isBlank() || pricePerUnit.toDoubleOrNull() == null) {
                        errorMessage = "Цена должна быть числом"
                        return@Button
                    }
                    if (selectedUnit == null) {
                        errorMessage = "Выберите единицу измерения"
                        return@Button
                    }

                    coroutineScope.launch {
                        try {
                            isSaving = true
                            val success = viewModel.addProduct(
                                name = productName,
                                priceOfUnit = pricePerUnit.toDouble(),
                                unitOfMeasurementId = selectedUnit!!.measurementId
                            )
                            if (success) {
                                viewModel.fetchProducts() // Обновляем список продуктов
                                onProductAdded()
                            } else {
                                errorMessage = "Не удалось добавить продукт"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Не удалось добавить продукт: ${e.message}"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
                Text("Сохранить")
            }
        }
    }
}
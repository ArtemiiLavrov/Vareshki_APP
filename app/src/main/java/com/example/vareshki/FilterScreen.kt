package com.example.vareshki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    viewModel: LoginViewModel,
    initialFilters: FilterParams,
    onApplyFilters: (FilterParams) -> Unit,
    onBack: () -> Unit
) {
    var canteens by remember { mutableStateOf<List<Canteen>>(emptyList()) }
    var selectedSenderCanteen by remember { mutableStateOf<Canteen?>(initialFilters.senderCanteen) }
    var selectedReceiverCanteen by remember { mutableStateOf<Canteen?>(initialFilters.receiverCanteen) }
    var senderExpanded by remember { mutableStateOf(false) }
    var receiverExpanded by remember { mutableStateOf(false) }
    var statuses by remember { mutableStateOf<List<OrderStatus>>(emptyList()) }
    var selectedStatus by remember { mutableStateOf<OrderStatus?>(initialFilters.status) }
    var statusExpanded by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<String?>(initialFilters.startDate) }
    var endDate by remember { mutableStateOf<String?>(initialFilters.endDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Загрузка данных
    LaunchedEffect(Unit) {
        try {
            println("Loading canteens and statuses in FilterScreen")
            canteens = viewModel.fetchCanteens()
            statuses = viewModel.fetchOrderStatuses()
            println("Canteens loaded: $canteens")
            println("Statuses loaded: $statuses")
        } catch (e: Exception) {
            errorMessage = "Не удалось загрузить данные: ${e.message}"
            println("Error in FilterScreen: ${e.message}")
        }
    }

    // DatePicker для начальной даты
    if (showStartDatePicker) {
        println("Showing DatePickerDialog for start date, showStartDatePicker: $showStartDatePicker")
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = {
                println("Start DatePickerDialog dismissed")
                showStartDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            startDate = LocalDate.ofEpochDay(millis / (1000 * 60 * 60 * 24)).format(formatter)
                            println("Selected start date: $startDate")
                        } ?: run {
                            errorMessage = "Пожалуйста, выберите начальную дату"
                            println("Error: Start date not selected")
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    println("Start DatePickerDialog cancelled")
                    showStartDatePicker = false
                }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.padding(16.dp),
                title = { Text("Выберите начальную дату") }
            )
        }
    }

    // DatePicker для конечной даты
    if (showEndDatePicker) {
        println("Showing DatePickerDialog for end date, showEndDatePicker: $showEndDatePicker")
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = {
                println("End DatePickerDialog dismissed")
                showEndDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            endDate = LocalDate.ofEpochDay(millis / (1000 * 60 * 60 * 24)).format(formatter)
                            println("Selected end date: $endDate")
                        } ?: run {
                            errorMessage = "Пожалуйста, выберите конечную дату"
                            println("Error: End date not selected")
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    println("End DatePickerDialog cancelled")
                    showEndDatePicker = false
                }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.padding(16.dp),
                title = { Text("Выберите конечную дату") }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                Text("Фильтры", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                val backInteractionSource = remember { MutableInteractionSource() }
                Card(
                    modifier = Modifier
                        .clickable(
                            interactionSource = backInteractionSource,
                            indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        ) {
                            println("Back button clicked")
                            onBack()
                        },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(min = 100.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ),
                                    radius = 100f
                                )
                            ).padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Назад",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Фильтр по столовой-отправителю
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Отправитель:",
                    modifier = Modifier.weight(1f)
                )
                ExposedDropdownMenuBox(
                    expanded = senderExpanded,
                    onExpandedChange = { senderExpanded = !senderExpanded },
                    modifier = Modifier
                        .weight(2f)
                        .padding(start = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedSenderCanteen?.address ?: "Все",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .height(56.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = senderExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = senderExpanded,
                        onDismissRequest = { senderExpanded = false },
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Все",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                selectedSenderCanteen = null
                                senderExpanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                        canteens.forEach { canteen ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = canteen.address,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedSenderCanteen = canteen
                                    senderExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Фильтр по столовой-получателю
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Получатель:", modifier = Modifier.weight(1f))
                ExposedDropdownMenuBox(
                    expanded = receiverExpanded,
                    onExpandedChange = { receiverExpanded = !receiverExpanded },
                    modifier = Modifier
                        .weight(2f)
                        .padding(start = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedReceiverCanteen?.address ?: "Все",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .height(56.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = receiverExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = receiverExpanded,
                        onDismissRequest = { receiverExpanded = false },
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        DropdownMenuItem(
                            text = { Text("Все", fontSize = 16.sp) },
                            onClick = {
                                selectedReceiverCanteen = null
                                receiverExpanded = false
                            }
                        )
                        canteens.forEach { canteen ->
                            DropdownMenuItem(
                                text = { Text(canteen.address, fontSize = 16.sp) },
                                onClick = {
                                    selectedReceiverCanteen = canteen
                                    receiverExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Фильтр по периоду времени (два поля: Начало и Конец)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Период:", modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable {
                            println("Start date field clicked, showStartDatePicker: $showStartDatePicker")
                            showStartDatePicker = true
                            println("showStartDatePicker set to: $showStartDatePicker")
                        }
                ) {
                    OutlinedTextField(
                        value = startDate?.let { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) } ?: "Начало",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Выбрать начальную дату"
                            )
                        },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledBorderColor = MaterialTheme.colorScheme.onSurface,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable {
                            println("End date field clicked, showEndDatePicker: $showEndDatePicker")
                            showEndDatePicker = true
                            println("showEndDatePicker set to: $showEndDatePicker")
                        }
                ) {
                    OutlinedTextField(
                        value = endDate?.let { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) } ?: "Конец",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Выбрать конечную дату"
                            )
                        },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledBorderColor = MaterialTheme.colorScheme.onSurface,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Кнопки "Применить" и "Сбросить"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Кнопка "Применить"
                val applyInteractionSource = remember { MutableInteractionSource() }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = applyInteractionSource,
                            indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        ) {
                            // Валидация дат
                            if (startDate != null && endDate != null) {
                                try {
                                    val start = LocalDate.parse(startDate)
                                    val end = LocalDate.parse(endDate)
                                    if (end.isBefore(start)) {
                                        errorMessage = "Конечная дата не может быть раньше начальной"
                                        println("Validation error: End date ($endDate) is before start date ($startDate)")
                                        return@clickable
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Неверный формат даты: ${e.message}"
                                    println("Date parsing error: ${e.message}")
                                    return@clickable
                                }
                            } else if (startDate != null && endDate == null || startDate == null && endDate != null) {
                                errorMessage = "Пожалуйста, выберите обе даты или оставьте поля пустыми"
                                println("Validation error: One date is selected, but the other is not")
                                return@clickable
                            }

                            val newFilters = FilterParams(
                                senderCanteen = selectedSenderCanteen,
                                receiverCanteen = selectedReceiverCanteen,
                                startDate = startDate,
                                endDate = endDate,
                                status = selectedStatus
                            )
                            println("Applying filters: $newFilters")
                            println("Filter details - senderCanteenId: ${newFilters.senderCanteen?.canteenId}, receiverCanteenId: ${newFilters.receiverCanteen?.canteenId}, startDate: ${newFilters.startDate}, endDate: ${newFilters.endDate}, statusId: ${newFilters.status?.statusId}")
                            onApplyFilters(newFilters)
                        },
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
                                    radius = 130f
                                )
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Применить",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                //Kнопка "Сбросить"
                val resetInteractionSource = remember { MutableInteractionSource() }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                        interactionSource = resetInteractionSource,
                indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                ) {
                selectedSenderCanteen = null
                selectedReceiverCanteen = null
                startDate = null
                endDate = null
                selectedStatus = null
                errorMessage = null
                val resetFilters = FilterParams()
                println("Resetting filters: $resetFilters")
                onApplyFilters(resetFilters)
            },
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
                radius = 130f
                )
                )
                .padding(8.dp),
                contentAlignment = Alignment.Center
                ) {
                Text(
                    text = "Сбросить",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
                )
            }
            }
            }
        }
    }
}
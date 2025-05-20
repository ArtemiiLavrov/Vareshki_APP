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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceFilterDialog(
    startDate: String?,
    endDate: String?,
    onApply: (String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var localStartDate by remember { mutableStateOf(startDate) }
    var localEndDate by remember { mutableStateOf(endDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фильтры", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = localStartDate?.let { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) } ?: "Начало",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartDatePicker = true },
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "Выбрать начальную дату")
                        },
                        enabled = false,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = localEndDate?.let { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) } ?: "Конец",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndDatePicker = true },
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "Выбрать конечную дату")
                        },
                        enabled = false,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    )
                }
                // Кнопки "Применить" и "Сбросить" в одну строку
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            localStartDate = null
                            localEndDate = null
                            errorMessage = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(43.dp)
                    ) {
                        Text("Сбросить")
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(43.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // Валидация дат
                                if (localStartDate != null && localEndDate != null) {
                                    try {
                                        val start = LocalDate.parse(localStartDate)
                                        val end = LocalDate.parse(localEndDate)
                                        if (end.isBefore(start)) {
                                            errorMessage = "Конечная дата не может быть раньше начальной"
                                            return@clickable
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Неверный формат даты: ${e.message}"
                                        return@clickable
                                    }
                                }
                                errorMessage = null
                                onApply(localStartDate, localEndDate)
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
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Применить",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}, // убираем стандартную confirmButton
        dismissButton = {}
    )

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localStartDate?.let {
                try {
                    LocalDate.parse(it).toEpochDay() * 24 * 60 * 60 * 1000
                } catch (e: Exception) {
                    null
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        localStartDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000)).toString()
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localEndDate?.let {
                try {
                    LocalDate.parse(it).toEpochDay() * 24 * 60 * 60 * 1000
                } catch (e: Exception) {
                    null
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        localEndDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000)).toString()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
} 
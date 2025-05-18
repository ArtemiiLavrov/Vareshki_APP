package com.example.vareshki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEmployeeScreen(
    viewModel: LoginViewModel,
    employee: Employee,
    onBack: () -> Unit,
    onEmployeeUpdated: () -> Unit,
    onShowProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(employee.name) }
    var surname by remember { mutableStateOf(employee.surname) }
    var patronymic by remember { mutableStateOf(employee.patronymic) }
    var phoneNumber by remember { mutableStateOf(employee.phoneNumber) }
    var role by remember { mutableStateOf(employee.role) }
    var selectedCanteenId by remember { mutableStateOf(employee.canteenId) }
    var expanded by remember { mutableStateOf(false) }
    val canteens by viewModel.canteens.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            viewModel.fetchCanteens()
        } catch (e: Exception) {
            errorMessage = "Не удалось загрузить столовые: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 48.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Редактировать работника", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = surname,
                            onValueChange = { surname = it },
                            label = { Text("Фамилия") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Имя") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = patronymic,
                            onValueChange = { patronymic = it },
                            label = { Text("Отчество") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Номер телефона") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = canteens.find { it.canteenId == selectedCanteenId }?.address ?: "Выберите столовую",
                                onValueChange = {},
                                label = { Text("Столовая") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = true },
                                enabled = false
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Без столовой") },
                                    onClick = {
                                        selectedCanteenId = null
                                        expanded = false
                                    }
                                )
                                canteens.forEach { canteen ->
                                    DropdownMenuItem(
                                        text = { Text(canteen.address) },
                                        onClick = {
                                            selectedCanteenId = canteen.canteenId
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Роль:", modifier = Modifier.align(Alignment.CenterVertically))
                            RadioButton(
                                selected = role == 1,
                                onClick = { role = 1 },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Пользователь")
                            RadioButton(
                                selected = role == 2,
                                onClick = { role = 2 },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Администратор")
                        }
                        val saveInteractionSource = remember { MutableInteractionSource() }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = saveInteractionSource,
                                    indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                                    enabled = !isSaving && name.isNotEmpty() && surname.isNotEmpty() && phoneNumber.isNotEmpty()
                                ) {
                                    coroutineScope.launch {
                                        try {
                                            isSaving = true
                                            val updatedEmployee = Employee(
                                                employeeId = employee.employeeId,
                                                name = name,
                                                surname = surname,
                                                patronymic = patronymic,
                                                phoneNumber = phoneNumber,
                                                role = role,
                                                canteenId = selectedCanteenId
                                            )
                                            val success = viewModel.updateEmployee(updatedEmployee)
                                            if (success) {
                                                onEmployeeUpdated()
                                            } else {
                                                errorMessage = "Не удалось обновить сотрудника"
                                            }
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
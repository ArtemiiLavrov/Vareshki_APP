package com.example.vareshki

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var employee by remember { mutableStateOf<Employee?>(null) }
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var patronymic by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var saveSuccess by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Загружаем данные профиля
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            employee = viewModel.fetchEmployeeProfile()
            employee?.let {
                name = it.name
                surname = it.surname
                patronymic = it.patronymic
                phoneNumber = it.phoneNumber
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактировать профиль") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Button(
                    onClick = {
                        if (name.isNotBlank() && surname.isNotBlank() && phoneNumber.isNotBlank()) {
                            coroutineScope.launch {
                                isSaving = true
                                employee?.let { emp ->
                                    val updatedEmployee = Employee(
                                        employeeId = emp.employeeId,
                                        name = name,
                                        surname = surname,
                                        patronymic = patronymic,
                                        phoneNumber = phoneNumber,
                                        role = emp.role // Передаём role из текущего employee
                                    )
                                    if (viewModel.updateEmployeeProfile(updatedEmployee)) {
                                        saveSuccess = true
                                        employee = updatedEmployee // Обновляем локальный employee
                                    }
                                }
                                isSaving = false
                            }
                        } else {
                            saveSuccess = false
                            // Можно добавить сообщение об ошибке
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving && employee != null
                ) {
                    Text("Сохранить")
                }

                if (saveSuccess) {
                    Text(
                        "Профиль успешно обновлён",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.logout()
                            onLogout()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Выйти из аккаунта")
                }
            }
        }
    }
}
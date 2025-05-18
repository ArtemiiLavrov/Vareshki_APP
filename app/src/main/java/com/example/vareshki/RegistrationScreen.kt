package com.example.vareshki

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: LoginViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val composeView = LocalView.current

    // Состояния для полей ввода
    var surname by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var patronymic by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Состояния для отображения пароля
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Заголовок
            Text(
                text = "Регистрация",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Поле для фамилии
            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it },
                label = { Text("Фамилия") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Поле для имени
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Поле для отчества
            OutlinedTextField(
                value = patronymic,
                onValueChange = { patronymic = it },
                label = { Text("Отчество") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Поле для номера телефона
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Номер телефона") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Поле для пароля
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Поле для подтверждения пароля
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Подтвердите пароль") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        coroutineScope.launch {
                            handleRegistration(
                                surname, name, patronymic, phoneNumber, password, confirmPassword,
                                context, viewModel, navController, focusManager
                            )
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Скрыть пароль" else "Показать пароль"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Кнопка регистрации
            Button(
                onClick = {
                    coroutineScope.launch {
                        handleRegistration(
                            surname, name, patronymic, phoneNumber, password, confirmPassword,
                            context, viewModel, navController, focusManager
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                enabled = surname.isNotEmpty() && name.isNotEmpty() && phoneNumber.isNotEmpty() &&
                        password.isNotEmpty() && confirmPassword.isNotEmpty()
            ) {
                Text("Зарегистрироваться")
            }
        }
    }

    // Автофокус на поле фамилии при открытии экрана
    LaunchedEffect(Unit) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(composeView, InputMethodManager.SHOW_IMPLICIT)
    }
}

// Вспомогательная функция для обработки регистрации
private suspend fun handleRegistration(
    surname: String,
    name: String,
    patronymic: String,
    phoneNumber: String,
    password: String,
    confirmPassword: String,
    context: Context,
    viewModel: LoginViewModel,
    navController: NavController,
    focusManager: FocusManager
) {
    // Сброс фокуса
    focusManager.clearFocus()

    // Валидация входных данных
    // Проверка на пустые поля
    if (surname.isBlank() || name.isBlank() || phoneNumber.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
        Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
        return
    }

    // Проверка совпадения паролей
    if (password != confirmPassword) {
        Toast.makeText(context, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
        return
    }

    // Проверка формата номера телефона (пример: 10-11 цифр)
    val phoneRegex = Regex("^\\+?[1-9]\\d{10}$")
    if (!phoneNumber.matches(phoneRegex)) {
        Toast.makeText(context, "Некорректный номер телефона", Toast.LENGTH_SHORT).show()
        return
    }

    // Проверка сложности пароля (минимум 8 символов, буквы и цифры)
    if (password.length < 8 || !password.any { it.isDigit() } || !password.any { it.isLetter() }) {
        Toast.makeText(context, "Пароль должен быть от 8 символов и содержать буквы и цифры", Toast.LENGTH_SHORT).show()
        return
    }

    // Проверка допустимых символов в surname, name, patronymic
    val nameRegex = Regex("^[А-Яа-яA-Za-z\\s-]+$")
    if (!surname.matches(nameRegex) || !name.matches(nameRegex) || (!patronymic.isBlank() && !patronymic.matches(nameRegex))) {
        Toast.makeText(context, "ФИО должно содержать только буквы", Toast.LENGTH_SHORT).show()
        return
    }

    // Вызов метода регистрации
    val result = viewModel.registerUser(
        surname = surname,
        name = name,
        patronymic = patronymic,
        phoneNumber = phoneNumber,
        password = password
    )

    // Обработка результата
    if (result) {
        Toast.makeText(context, "Регистрация успешна", Toast.LENGTH_SHORT).show()
        navController.navigate("loginScreen") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    } else {
        Toast.makeText(context, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
    }
}
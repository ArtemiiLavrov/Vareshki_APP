package com.example.vareshki

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isLoggedIn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    println("Экран входа - isLoggedIn: $isLoggedIn") // Лог для отладки

    if (isLoggedIn) {
        MainScreen(viewModel = viewModel, navController = navController)
    } else {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Надпись "Вход"
                    Text(
                        text = "Вход",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(8.dp)
                            .onGloballyPositioned {
                                println("Текст 'Вход' отображен на позиции: ${it.positionInRoot()}")
                            }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val focusManager = LocalFocusManager.current
                    val phoneFocusRequester = remember { FocusRequester() }
                    val composeView = LocalView.current
                    var isPhoneFieldFocused by remember { mutableStateOf(false) }

                    // Поле для номера телефона
                    OutlinedTextField(
                        value = uiState.phoneNumber,
                        onValueChange = { viewModel.updatePhoneNumber(it) },
                        label = { Text("Номер телефона") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(phoneFocusRequester)
                            .onFocusChanged { focusState ->
                                isPhoneFieldFocused = focusState.isFocused
                                println("Состояние фокуса поля телефона: $focusState")
                                if (focusState.isFocused) {
                                    println("Поле телефона в фокусе, показ клавиатуры")
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showSoftInput(composeView, InputMethodManager.SHOW_IMPLICIT)
                                }
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                println("Действие 'Далее' для поля телефона")
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val passwordFocusRequester = remember { FocusRequester() }
                    // Поле для пароля
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("Пароль") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester)
                            .onFocusChanged { focusState ->
                                println("Состояние фокуса поля пароля: $focusState")
                                if (focusState.isFocused) {
                                    println("Поле пароля в фокусе, показ клавиатуры")
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showSoftInput(composeView, InputMethodManager.SHOW_IMPLICIT)
                                }
                            },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                println("Действие 'Готово' для поля пароля")
                                coroutineScope.launch {
                                    if (viewModel.validateCredentials()) {
                                        Toast.makeText(context, "Вход выполнен", Toast.LENGTH_SHORT).show()
                                        isLoggedIn = true
                                    } else {
                                        Toast.makeText(context, "Неверные данные", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                focusManager.clearFocus()
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Кнопка входа
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (viewModel.validateCredentials()) {
                                    Toast.makeText(context, "Вход выполнен", Toast.LENGTH_SHORT).show()
                                    isLoggedIn = true
                                } else {
                                    Toast.makeText(context, "Неверные данные", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding(),
                        enabled = uiState.phoneNumber.isNotEmpty() && uiState.password.isNotEmpty()
                    ) {
                        Text("Войти")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Надпись для перехода на регистрацию
                    Text(
                        text = "Нет аккаунта? Зарегистрироваться",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                println("Переход на экран регистрации")
                                navController.navigate("registrationScreen")
                            }
                            .padding(8.dp)
                    )

                    // Автофокус на поле телефона
                    LaunchedEffect(Unit) {
                        delay(200)
                        phoneFocusRequester.requestFocus()
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(composeView, InputMethodManager.SHOW_IMPLICIT)
                        println("InputMethodManager showSoftInput вызван с SHOW_IMPLICIT")
                    }
                }
            }
        }
    }
}
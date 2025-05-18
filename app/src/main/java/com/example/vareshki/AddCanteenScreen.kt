package com.example.vareshki

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCanteenScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onCanteenAdded: () -> Unit
) {
    var address by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить столовую") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Адрес") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val success = viewModel.addCanteen(address)
                        if (success) {
                            onCanteenAdded()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = address.isNotEmpty()
            ) {
                Text("Добавить")
            }
        }
    }
}
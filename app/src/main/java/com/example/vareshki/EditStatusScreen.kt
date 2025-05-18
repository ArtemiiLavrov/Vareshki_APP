package com.example.vareshki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun EditStatusScreen(
    viewModel: LoginViewModel,
    status: OrderStatus,
    onBack: () -> Unit,
    onStatusUpdated: () -> Unit
) {
    var statusName by remember { mutableStateOf(status.statusName) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактировать статус") },
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
                value = statusName,
                onValueChange = { statusName = it },
                label = { Text("Название статуса") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            val saveInteractionSource = remember { MutableInteractionSource() }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = saveInteractionSource,
                        indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                        enabled = !isSaving && statusName.isNotEmpty()
                    ) {
                        coroutineScope.launch {
                            try {
                                isSaving = true
                                val success = viewModel.updateStatus(status.statusId, statusName)
                                if (success) {
                                    onStatusUpdated()
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
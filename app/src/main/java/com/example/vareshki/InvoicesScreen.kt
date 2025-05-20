package com.example.vareshki

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.minio.GetObjectArgs
import io.minio.ListObjectsArgs
import io.minio.messages.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import android.widget.Toast

suspend fun downloadInvoiceFile(objectName: String, context: Context): File {
    return withContext(Dispatchers.IO) {
        val minioClient = S3ClientProvider.minioClient
        val inputStream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket("invoices")
                .`object`(objectName)
                .build()
        )
        
        // Создаем директорию для файлов, если её нет
        val downloadDir = File(context.getExternalFilesDir(null), "invoices")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        
        // Сохраняем файл
        val file = File(downloadDir, objectName)
        FileOutputStream(file).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        file
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    onBack: () -> Unit,
    onShowProfile: () -> Unit
) {
    var invoices by remember { mutableStateOf<List<Item>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val result = S3ClientProvider.minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket("invoices")
                    .prefix("invoice_")
                    .build()
            )
            invoices = result.map { it.get() }.toList()
        } catch (e: Exception) {
            errorMessage = "Ошибка при загрузке накладных: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Накладные") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onShowProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Профиль")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                invoices.isEmpty() -> {
                    Text(
                        text = "Накладные не найдены",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(invoices) { invoice ->
                            InvoiceItem(
                                invoice = invoice,
                                onDownload = { objectName ->
                                    coroutineScope.launch {
                                        try {
                                            val file = downloadInvoiceFile(objectName, context)
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                file
                                            )
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/pdf")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Ошибка при скачивании: ${e.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceItem(
    invoice: Item,
    onDownload: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            onDownload(invoice.objectName())
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = invoice.objectName().removePrefix("invoice_").removeSuffix(".pdf"),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Размер: ${invoice.size() / 1024} KB",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Дата: ${invoice.lastModified()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = {
                onDownload(invoice.objectName())
            }) {
                Icon(Icons.Default.Download, contentDescription = "Скачать")
            }
        }
    }
} 
package com.example.vareshki

import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context

class InvoiceStorageManager(private val context: Context) {
    companion object {
        private const val BUCKET_NAME = "invoices"
        private const val INVOICE_PREFIX = "invoice_"
    }

    suspend fun saveInvoice(orderId: String, invoiceContent: String) = withContext(Dispatchers.IO) {
        try {
            // Формируем имя файла
            val fileName = "${INVOICE_PREFIX}${orderId}.pdf"
            
            // Конвертируем XML в PDF
            val pdfConverter = InvoicePdfConverter()
            val pdfContent = pdfConverter.convertXmlToPdf(invoiceContent, context)
            
            // Проверяем существование файла
            val fileExists = try {
                S3ClientProvider.minioClient.statObject(
                    StatObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .`object`(fileName)
                        .build()
                )
                true
            } catch (e: Exception) {
                false
            }

            // Если файл существует - удаляем его и ждем завершения операции
            if (fileExists) {
                withContext(Dispatchers.IO) {
                    try {
                        S3ClientProvider.minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                .bucket(BUCKET_NAME)
                                .`object`(fileName)
                                .build()
                        )
                        // Добавляем небольшую задержку после удаления
                        kotlinx.coroutines.delay(100)
                    } catch (e: Exception) {
                        throw Exception("Ошибка при удалении существующей накладной: ${e.message}")
                    }
                }
            }

            // Сохраняем новый файл
            ByteArrayInputStream(pdfContent).use { inputStream ->
                try {
                    S3ClientProvider.minioClient.putObject(
                        PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .`object`(fileName)
                            .stream(inputStream, pdfContent.size.toLong(), -1)
                            .contentType("application/pdf")
                            .build()
                    )
                } catch (e: Exception) {
                    throw Exception("Ошибка при сохранении новой накладной: ${e.message}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Ошибка при работе с накладной: ${e.message}")
        }
    }
} 
package com.example.vareshki

// Шаблон строки товара в накладной
data class InvoiceItem(
    val name: String = "",
    val estimatedQuantity: Int = 0,
    val realQuantity: Int = 0,
    val price: Double = 0.0,
    val total: Double = 0.0
)

// Шаблон накладной
data class InvoiceTemplate(
    val number: String = "",
    val date: String = "",
    val recipient: String = "",
    val sender: String = "",
    val items: List<InvoiceItem> = emptyList()
) 
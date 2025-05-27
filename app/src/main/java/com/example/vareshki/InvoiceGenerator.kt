package com.example.vareshki

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.io.StringWriter

class InvoiceGenerator(private val viewModel: LoginViewModel) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }

    suspend fun generateInvoiceXml(order: Order, orderDetails: OrderDetails): String {
        val docFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docFactory.newDocumentBuilder()
        val doc = docBuilder.newDocument()

        val invoiceElement = doc.createElement("invoice")
        doc.appendChild(invoiceElement)

        val headerElement = doc.createElement("header")
        invoiceElement.appendChild(headerElement)

        addElement(doc, headerElement, "number", order.orderId.toString())

        val currentDate = LocalDate.now()
        val day = currentDate.dayOfMonth.toString().padStart(2, '0')
        val month = currentDate.monthValue.toString().padStart(2, '0')
        val year = currentDate.year.toString().substring(2)
        addElement(doc, headerElement, "date", "\"$day\" $month 20$year г.")

        addElement(doc, headerElement, "recipient", order.receiverCanteen.address)
        addElement(doc, headerElement, "sender", order.senderCanteen.address)

        val tableElement = doc.createElement("table")
        invoiceElement.appendChild(tableElement)

        val columnsElement = doc.createElement("columns")
        tableElement.appendChild(columnsElement)

        val columnHeaders = listOf(
            "№ п/п",
            "Наименование",
            "Количество ожидаемое",
            "Количество отправленное",
            "Единица измерения",
            "Цена",
            "Сумма",
            "Статус"
        )
        columnHeaders.forEach { header ->
            addElement(doc, columnsElement, "column", header, mapOf("width" to when (header) {
                "Наименование" -> "100px"
                else -> "50px"
            }))
        }

        val rowsElement = doc.createElement("rows")
        tableElement.appendChild(rowsElement)

        if (orderDetails.products.isEmpty()) {
            val emptyRow = doc.createElement("row")
            rowsElement.appendChild(emptyRow)
            addElement(doc, emptyRow, "message", "Нет продуктов")
        } else {
            orderDetails.products.forEachIndexed { index, orderProduct ->
                val rowElement = doc.createElement("row")
                rowsElement.appendChild(rowElement)

                val measurementName = viewModel.getMeasurementName(orderProduct.product.unitOfMeasurement.toInt())
                val actualQuantity = viewModel.getActualQuantity(order.orderId, orderProduct.product.productId) ?: 0.0
                val isAccepted = viewModel.getIsAccepted(order.orderId, orderProduct.product.productId)
                val status = if (isAccepted) "Принято" else "Не принято"
                val productName = orderProduct.product.name

                addElement(doc, rowElement, "number", (index + 1).toString())
                addElement(doc, rowElement, "name", productName)
                addElement(doc, rowElement, "expectedQuantity", String.format("%.2f", orderProduct.quantity))
                addElement(doc, rowElement, "actualQuantity", String.format("%.2f", actualQuantity))
                addElement(doc, rowElement, "unitOfMeasurement", measurementName) // Ограничиваем до 6 символов
                addElement(doc, rowElement, "price", String.format("%.2f", orderProduct.product.priceOfUnit))
                addElement(doc, rowElement, "total", String.format("%.2f", orderProduct.product.priceOfUnit * actualQuantity))
                addElement(doc, rowElement, "status", status)
            }
        }

        val signaturesElement = doc.createElement("signatures")
        invoiceElement.appendChild(signaturesElement)
        addElement(doc, signaturesElement, "delivered_by", "Сдал: ___________")
        addElement(doc, signaturesElement, "received_by", "Принял: ___________")

        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        val source = DOMSource(doc)
        val stringWriter = StringWriter()
        val result = StreamResult(stringWriter)
        transformer.transform(source, result)

        return stringWriter.toString()
    }

    private fun addElement(doc: Document, parent: Element, name: String, value: String, attributes: Map<String, String> = emptyMap()): Element {
        val element = doc.createElement(name)
        element.appendChild(doc.createTextNode(value))
        attributes.forEach { (key, attrValue) -> element.setAttribute(key, attrValue) }
        parent.appendChild(element)
        return element
    }
}
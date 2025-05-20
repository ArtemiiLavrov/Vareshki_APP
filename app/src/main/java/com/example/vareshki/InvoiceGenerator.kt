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
import kotlinx.coroutines.runBlocking

class InvoiceGenerator(private val viewModel: LoginViewModel) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }

    fun generateInvoiceXml(order: Order, orderDetails: OrderDetails): String {
        val docFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docFactory.newDocumentBuilder()
        val doc = docBuilder.newDocument()

        val invoiceElement = doc.createElement("invoice")
        doc.appendChild(invoiceElement)

        val headerElement = doc.createElement("header")
        invoiceElement.appendChild(headerElement)

        addElement(doc, headerElement, "number", order.orderId.toString())

        // Date
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
        
        val columnHeaders = listOf("№ п/п", "Наименование", "Количество", "Цена, руб.", "Сумма, руб.")
        columnHeaders.forEach { header ->
            addElement(doc, columnsElement, "column", header)
        }

        val rowsElement = doc.createElement("rows")
        tableElement.appendChild(rowsElement)

        orderDetails.products.forEachIndexed { index, orderProduct ->
            val rowElement = doc.createElement("row")
            rowsElement.appendChild(rowElement)

            val measurementName = runBlocking {
                viewModel.getMeasurementName(orderProduct.product.unitOfMeasurement.toInt())
            }

            addElement(doc, rowElement, "number", (index + 1).toString())
            addElement(doc, rowElement, "name", "${orderProduct.product.name}, ${measurementName}")
            addElement(doc, rowElement, "quantity", orderProduct.quantity.toString())
            addElement(doc, rowElement, "price", String.format("%.2f", orderProduct.product.priceOfUnit))
            addElement(doc, rowElement, "total", String.format("%.2f", orderProduct.product.priceOfUnit * orderProduct.quantity))
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

    private fun addElement(doc: Document, parent: Element, name: String, value: String) {
        val element = doc.createElement(name)
        element.appendChild(doc.createTextNode(value))
        parent.appendChild(element)
    }
} 
package com.example.vareshki

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFont
import android.content.Context
import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy

class InvoicePdfConverter {
    private lateinit var regularFont: PdfFont
    private lateinit var boldFont: PdfFont

    private fun initializeFonts(context: Context) {
        try {
            // Копируем файлы шрифтов из assets во временную директорию
            val tempDir = context.cacheDir
            
            // Копируем обычный шрифт
            val regularFontFile = File(tempDir, "arial.ttf")
            copyAssetToFile(context.assets, "fonts/arial.ttf", regularFontFile)
            
            // Копируем жирный шрифт
            val boldFontFile = File(tempDir, "arialbd.ttf")
            copyAssetToFile(context.assets, "fonts/arialbd.ttf", boldFontFile)

            // Создаем шрифты с явным указанием стратегии встраивания
            regularFont = PdfFontFactory.createFont(
                regularFontFile.absolutePath,
                PdfEncodings.IDENTITY_H,
                EmbeddingStrategy.FORCE_EMBEDDED
            )
            boldFont = PdfFontFactory.createFont(
                boldFontFile.absolutePath,
                PdfEncodings.IDENTITY_H,
                EmbeddingStrategy.FORCE_EMBEDDED
            )
        } catch (e: Exception) {
            throw Exception("Ошибка при инициализации шрифтов: ${e.message}")
        }
    }

    private fun copyAssetToFile(assets: AssetManager, assetPath: String, destFile: File) {
        assets.open(assetPath).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun convertXmlToPdf(xmlContent: String, context: Context): ByteArray {
        // Инициализируем шрифты
        initializeFonts(context)

        val docFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docFactory.newDocumentBuilder()
        val xmlDoc = docBuilder.parse(xmlContent.byteInputStream())
        
        val outputStream = ByteArrayOutputStream()
        val pdfWriter = PdfWriter(outputStream)
        val pdfDoc = PdfDocument(pdfWriter)
        val document = Document(pdfDoc)

        // Получаем данные из XML
        val header = xmlDoc.getElementsByTagName("header").item(0)
        val number = getElementText(header, "number")
        val date = getElementText(header, "date")
        val recipient = getElementText(header, "recipient")
        val sender = getElementText(header, "sender")

        // Создаем заголовок накладной
        val title = Paragraph()
            .add(Paragraph("НАКЛАДНАЯ №$number").setFont(boldFont).setFontSize(14f))
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
        document.add(title)

        // Добавляем информацию о получателе и отправителе
        document.add(Paragraph("Кому: $recipient").setFont(regularFont).setFontSize(12f).setMarginBottom(5f))
        document.add(Paragraph("От: $sender").setFont(regularFont).setFontSize(12f).setMarginBottom(5f))
        document.add(Paragraph(date).setFont(regularFont).setFontSize(12f)
            .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(20f))

        // Создаем таблицу
        val table = Table(UnitValue.createPercentArray(floatArrayOf(8f, 44f, 16f, 16f, 16f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)

        // Добавляем заголовки таблицы
        val columns = xmlDoc.getElementsByTagName("column")
        for (i in 0 until columns.length) {
            table.addHeaderCell(
                Cell()
                    .setFont(boldFont)
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPadding(5f)
                    .setBorder(SolidBorder(ColorConstants.BLACK, 1f))
                    .add(Paragraph(columns.item(i).textContent))
            )
        }

        // Добавляем строки с данными
        val rows = xmlDoc.getElementsByTagName("row")
        for (i in 0 until rows.length) {
            val row = rows.item(i)
            
            // Номер
            addTableCell(table, getElementText(row, "number"), TextAlignment.CENTER)
            
            // Наименование
            addTableCell(table, getElementText(row, "name"), TextAlignment.LEFT)
            
            // Количество
            addTableCell(table, getElementText(row, "quantity"), TextAlignment.CENTER)
            
            // Цена
            addTableCell(table, getElementText(row, "price"), TextAlignment.RIGHT)
            
            // Сумма
            addTableCell(table, getElementText(row, "total"), TextAlignment.RIGHT)
        }
        document.add(table)

        // Добавляем подписи
        val signatures = xmlDoc.getElementsByTagName("signatures").item(0)
        val signatureTable = Table(UnitValue.createPercentArray(2)).useAllAvailableWidth()
        
        signatureTable.addCell(
            Cell()
                .setBorder(SolidBorder.NO_BORDER)
                .add(Paragraph(getElementText(signatures, "delivered_by"))
                    .setFont(regularFont)
                    .setFontSize(12f))
        )
        
        signatureTable.addCell(
            Cell()
                .setBorder(SolidBorder.NO_BORDER)
                .add(Paragraph(getElementText(signatures, "received_by"))
                    .setFont(regularFont)
                    .setFontSize(12f))
        )
        
        document.add(signatureTable)
        document.close()
        
        return outputStream.toByteArray()
    }

    private fun getElementText(parent: org.w3c.dom.Node, tagName: String): String {
        val elements = parent.childNodes
        for (i in 0 until elements.length) {
            val node = elements.item(i)
            if (node.nodeName == tagName) {
                return node.textContent
            }
        }
        return ""
    }

    private fun addTableCell(table: Table, text: String, alignment: TextAlignment) {
        table.addCell(
            Cell()
                .setFont(regularFont)
                .setFontSize(10f)
                .setTextAlignment(alignment)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(5f)
                .setBorder(SolidBorder(ColorConstants.BLACK, 1f))
                .add(Paragraph(text))
        )
    }
} 
package com.example.savvy.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.savvy.R
import com.example.savvy.data.Transaction
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun exportTransactionsToPdf(
        context: Context,
        transactions: List<Transaction>,
        fileName: String,
        reportTitle: String,
        logoBitmap: Bitmap? = null
    ): Uri? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }

        var y = 40f

        // Draw logo on the right
        logoBitmap?.let { bitmap ->
            val logoWidth = 80
            val logoHeight = (bitmap.height.toFloat() / bitmap.width.toFloat() * logoWidth).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, logoWidth, logoHeight, true)
            canvas.drawBitmap(scaledBitmap, 495f - logoWidth, y - (scaledBitmap.height / 2), null) // Right top position
            y += scaledBitmap.height / 2
        }

        // Draw title and date
        canvas.drawText(reportTitle, 30f, y, titlePaint)
        y += 25f
        canvas.drawText("Dicetak pada: ${SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("id", "ID")).format(Date())}", 30f, y, paint)
        y += 30f

        // Draw headers
        canvas.drawText("Tanggal", 30f, y, paint)
        canvas.drawText("Kategori", 130f, y, paint)
        canvas.drawText("Tipe", 230f, y, paint)
        canvas.drawText("Jumlah", 330f, y, paint)
        canvas.drawText("Catatan", 430f, y, paint)
        y += 15f
        canvas.drawLine(20f, y, pageInfo.pageWidth - 20f, y, paint)
        y += 15f

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }

        transactions.forEach { transaction ->
            if (y > 800f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
                canvas.drawText("Tanggal", 30f, y, paint)
                canvas.drawText("Kategori", 130f, y, paint)
                canvas.drawText("Tipe", 230f, y, paint)
                canvas.drawText("Jumlah", 330f, y, paint)
                canvas.drawText("Catatan", 430f, y, paint)
                y += 15f
                canvas.drawLine(20f, y, pageInfo.pageWidth - 20f, y, paint)
                y += 15f
            }

            canvas.drawText(SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID")).format(transaction.date ?: Date()), 30f, y, paint)
            canvas.drawText(transaction.category, 130f, y, paint)
            canvas.drawText(transaction.type, 230f, y, paint)
            canvas.drawText(currencyFormat.format(transaction.amount), 330f, y, paint)
            canvas.drawText(transaction.note, 430f, y, paint)
            y += 20f
        }

        document.finishPage(page)

        val documentsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(documentsFolder, "$fileName.pdf")

        try {
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            document.close()
            Toast.makeText(context, "PDF berhasil disimpan di ${file.absolutePath}", Toast.LENGTH_LONG).show()
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: IOException) {
            Log.e("ExportUtils", "Error writing PDF: ${e.message}", e)
            Toast.makeText(context, "Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<Transaction>,
        fileName: String,
        categoryName: String? = null
    ): Uri? {
        val documentsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(documentsFolder, "$fileName.csv")

        try {
            FileWriter(file).use { writer ->
                val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("id", "ID"))
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                    maximumFractionDigits = 0
                }

                writer.append("Tanggal,Kategori,Tipe,Jumlah,Catatan\n")

                transactions.forEach { transaction ->
                    val dateStr = dateFormat.format(transaction.date ?: Date())
                    val amountStr = currencyFormat.format(transaction.amount).replace(",", "")
                    val noteCsv = transaction.note.replace(",", ";").replace("\n", " ").trim()

                    writer.append("$dateStr,")
                    writer.append("${transaction.category},")
                    writer.append("${transaction.type},")
                    writer.append("$amountStr,")
                    writer.append("\"$noteCsv\"\n")
                }
            }
            Toast.makeText(context, "CSV berhasil disimpan di ${file.absolutePath}", Toast.LENGTH_LONG).show()
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: IOException) {
            Log.e("ExportUtils", "Error writing CSV: ${e.message}", e)
            Toast.makeText(context, "Gagal menyimpan CSV: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    fun openFile(context: Context, uri: Uri, mimeType: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi untuk membuka file ini.", Toast.LENGTH_SHORT).show()
            Log.e("ExportUtils", "Error opening file: ${e.message}", e)
        }
    }
}
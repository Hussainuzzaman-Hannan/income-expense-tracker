package com.example.incomeexpensetracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for generating a PDF document from a list of financial transactions.
 */
object PdfGenerator {

    /**
     * Generates a PDF document containing transaction details and writes it to the specified URI.
     *
     * @param context The application context, used for accessing string resources.
     * @param uri The URI where the PDF document will be written.
     * @param transactions The list of [Transaction] objects to be included in the PDF.
     * @throws Exception if an error occurs during PDF generation or writing.
     */
    fun generatePdf(context: Context, uri: Uri, transactions: List<Transaction>) {
        val document = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size (approx)
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas // Changed to 'var' so it can be reassigned
        val paint = Paint()

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("bn", "BD"))
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val currentDateTime = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US).format(Date())

        var x = 40f
        var y = 40f
        val lineSpacing = 25f
        val tableRowSpacing = 20f
        val columnWidthId = 60f
        val columnWidthDescription = 180f
        val columnWidthAmount = 80f
        val columnWidthType = 60f
        val columnWidthCategory = 100f
        val columnWidthDate = 80f

        // Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 24f
        paint.color = Color.BLACK
        canvas.drawText(context.getString(R.string.app_name) + " - Transaction Report", x, y, paint)
        y += lineSpacing

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated On: $currentDateTime", x, y, paint)
        y += lineSpacing * 1.5f // Increased spacing after generated date

        // Calculate Summary Data
        var totalIncome = 0.0
        var totalExpenses = 0.0
        transactions.forEach { transaction ->
            if (transaction.type == TransactionType.INCOME) {
                totalIncome += transaction.amount
            } else {
                totalExpenses += transaction.amount
            }
        }
        val balance = totalIncome - totalExpenses

        // Draw Summary Section
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText(context.getString(R.string.summary_title), x, y, paint)
        y += lineSpacing

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        canvas.drawText(context.getString(R.string.total_income_label) + " " + currencyFormatter.format(totalIncome), x, y, paint)
        y += lineSpacing
        canvas.drawText(context.getString(R.string.total_expenses_label) + " " + currencyFormatter.format(totalExpenses), x, y, paint)
        y += lineSpacing

        // Balance with color coding
        paint.color = when {
            balance > 0 -> Color.parseColor("#4CAF50") // Green for positive
            balance < 0 -> Color.parseColor("#F44336") // Red for negative
            else -> Color.parseColor("#2196F3") // Blue for neutral/zero
        }
        canvas.drawText(context.getString(R.string.balance_label) + " " + currencyFormatter.format(balance), x, y, paint)
        paint.color = Color.BLACK // Reset color
        y += lineSpacing * 2 // Increased spacing before transaction table

        // Table Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        canvas.drawText("ID", x, y, paint)
        canvas.drawText("Description", x + columnWidthId, y, paint)
        canvas.drawText("Amount", x + columnWidthId + columnWidthDescription, y, paint)
        canvas.drawText(context.getString(R.string.transaction_type_label), x + columnWidthId + columnWidthDescription + columnWidthAmount, y, paint)
        canvas.drawText(context.getString(R.string.transaction_category_label), x + columnWidthId + columnWidthDescription + columnWidthAmount + columnWidthType, y, paint)
        canvas.drawText(context.getString(R.string.date_label), x + columnWidthId + columnWidthDescription + columnWidthAmount + columnWidthType + columnWidthCategory, y, paint)
        y += lineSpacing

        // Draw a line under the header
        canvas.drawLine(x, y - 5, pageInfo.pageWidth - x, y - 5, paint)
        y += 5 // Small extra spacing after line

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f // Smaller font size for table content

        // Table Content
        transactions.forEach { transaction ->
            if (y > pageInfo.pageHeight - 80) { // Check if new page is needed, adjusted threshold
                document.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create() // Reassign pageInfo for new page
                page = document.startPage(pageInfo) // Reassign page for new page
                canvas = page.canvas // Crucial: Reassign canvas to the new page's canvas
                canvas.drawColor(Color.WHITE) // Clear the new canvas's background

                x = 40f // Reset X for new page
                y = 40f // Reset Y for new page

                // Redraw header on new page
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 24f
                paint.color = Color.BLACK
                canvas.drawText(context.getString(R.string.app_name) + " - Transaction Report (Cont.)", x, y, paint)
                y += lineSpacing

                paint.textSize = 12f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Generated On: $currentDateTime", x, y, paint)
                y += lineSpacing * 2

                // Table Header on new page
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                canvas.drawText("ID", x, y, paint)
                canvas.drawText("Description", x + columnWidthId, y, paint)
                canvas.drawText("Amount", x + columnWidthId + columnWidthDescription, y, paint)
                canvas.drawText(context.getString(R.string.transaction_type_label), x + columnWidthId + columnWidthDescription + columnWidthAmount, y, paint)
                canvas.drawText(context.getString(R.string.transaction_category_label), x + columnWidthId + columnWidthDescription + columnWidthAmount + columnWidthType, y, paint)
                canvas.drawText(context.getString(R.string.date_label), x + columnWidthId + columnWidthDescription + columnWidthAmount + columnWidthType + columnWidthCategory, y, paint)
                y += lineSpacing

                canvas.drawLine(x, y - 5, pageInfo.pageWidth - x, y - 5, paint)
                y += 5
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 9f
            }

            // Draw transaction details
            paint.color = Color.BLACK // Set color to black for text by default
            canvas.drawText(transaction.id.substring(0, 4), x, y, paint) // Shorten ID for brevity
            canvas.drawText(truncateText(transaction.description, paint, columnWidthDescription), x + columnWidthId, y, paint)
            // Color amount based on type
            paint.color = if (transaction.type == TransactionType.INCOME) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
            canvas.drawText(currencyFormatter.format(transaction.amount), x + columnWidthId + columnWidthDescription, y, paint)
            paint.color = Color.BLACK // Reset color to black for other text
            canvas.drawText(transaction.type.name, x + columnWidthId + columnWidthDescription + columnWidthAmount, y, paint)
            canvas.drawText(truncateText(transaction.category, paint, columnWidthCategory), x + columnWidthId + columnWidthDescription + columnWidthAmount + columnWidthType, y, paint)
            canvas.drawText(transaction.date, x + columnWidthId + columnWidthDescription + columnWidthAmount + columnWidthType + columnWidthCategory, y, paint)
            y += tableRowSpacing
        }

        document.finishPage(page)

        var outputStream: OutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(uri)
            document.writeTo(outputStream)
            Log.d("PdfGenerator", "PDF generated and written to URI: $uri")
        } catch (e: Exception) {
            Log.e("PdfGenerator", "Error writing PDF: ${e.message}")
            throw e // Re-throw the exception to be handled by the caller
        } finally {
            outputStream?.close()
            document.close()
            Log.d("PdfGenerator", "PDF document closed.")
        }
    }

    /**
     * Truncates text to fit within a specified width, adding "..." if truncated.
     */
    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        return if (paint.measureText(text) > maxWidth) {
            var truncatedText = text
            while (paint.measureText(truncatedText + "...") > maxWidth && truncatedText.length > 1) {
                truncatedText = truncatedText.substring(0, truncatedText.length - 1)
            }
            "$truncatedText..."
        } else {
            text
        }
    }
}

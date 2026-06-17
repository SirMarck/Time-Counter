package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.Client
import com.example.data.Session
import java.io.File
import java.io.FileOutputStream

object ExportUtils {
    private fun drawReportContent(
        context: Context,
        canvas: Canvas,
        width: Float,
        client: Client,
        sessions: List<Session>,
        monthName: String,
        paint: Paint
    ) {
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width, canvas.height.toFloat(), paint)

        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.LEFT

        var y = 60f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f
        canvas.drawText("Relatório Comercial", 50f, y, paint)

        y += 35f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 15f
        paint.color = Color.GRAY
        val reportDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText("Gerado em: $reportDate", 50f, y, paint)

        y += 50f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 26f // Enfatizado - Nome do cliente maior
        canvas.drawText("Cliente: ${client.name}", 50f, y, paint)

        y += 35f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 18f
        canvas.drawText("Mês de Referência: $monthName", 50f, y, paint)
        y += 30f

        var totalDuration = 0L
        sessions.forEach { totalDuration += maxOf(0L, (it.endTime!! - it.startTime) - it.pausedDuration) }
        val totalHours = totalDuration.toDouble() / (1000 * 60 * 60)
        val totalValue = totalHours * client.hourlyRate

        canvas.drawText("Total de Horas: ${FormatUtils.formatDuration(totalDuration)}", 50f, y, paint)
        y += 30f
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Valor a Cobrar: ${FormatUtils.formatCurrency(totalValue)}", 50f, y, paint)
        
        y += 60f
        paint.textSize = 18f
        canvas.drawText("Histórico de Serviços:", 50f, y, paint)
        y += 10f
        
        paint.strokeWidth = 1f
        canvas.drawLine(50f, y, width - 50f, y, paint)
        y += 30f

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 16f
        
        for (session in sessions.sortedBy { it.startTime }) {
            val duration = maxOf(0L, (session.endTime!! - session.startTime) - session.pausedDuration)
            val valItem = (duration.toDouble() / (1000 * 60 * 60)) * client.hourlyRate
            val dateStr = FormatUtils.formatDate(session.startTime)
            val desc = if (session.description.length > 45) session.description.take(42) + "..." else session.description
            
            canvas.drawText("$dateStr — $desc", 50f, y, paint)
            y += 25f
            
            paint.color = Color.DKGRAY
            canvas.drawText("Duração: ${FormatUtils.formatDuration(duration)} | Subtotal: ${FormatUtils.formatCurrency(valItem)}", 70f, y, paint)
            y += 35f
            paint.color = Color.BLACK
        }
        
        y += 40f
        paint.textSize = 14f
        paint.color = Color.DKGRAY
        paint.textAlign = Paint.Align.CENTER
        
        val sharedPrefs = context.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE)
        val compName = sharedPrefs.getString("company_name", "") ?: ""
        val compCnpj = sharedPrefs.getString("company_cnpj", "") ?: ""
        
        if (compName.isNotEmpty()) {
            val footerText = if (compCnpj.isNotEmpty()) "$compName — CNPJ: $compCnpj" else compName
            canvas.drawText(footerText, width / 2, y, paint)
            y += 20f
        }
        
        paint.color = Color.GRAY
        canvas.drawText("Gerado por TempoTrack", width / 2, y, paint)
    }

    fun generatePdf(context: Context, client: Client, sessions: List<Session>, monthName: String): File? {
        try {
            val document = PdfDocument()
            val width = 595
            val height = maxOf(842, 350 + sessions.size * 60 + 100)
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = document.startPage(pageInfo)
            
            val paint = Paint()
            drawReportContent(context, page.canvas, width.toFloat(), client, sessions, monthName, paint)
            
            document.finishPage(page)
            
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportsDir, "Relatorio_${client.name.replace(" ", "_")}_$monthName.pdf")
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun generateImage(context: Context, client: Client, sessions: List<Session>, monthName: String): File? {
        try {
            val width = 600
            val height = maxOf(800, 350 + sessions.size * 60 + 100)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            
            drawReportContent(context, canvas, width.toFloat(), client, sessions, monthName, paint)
            
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportsDir, "Relatorio_${client.name.replace(" ", "_")}_$monthName.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório"))
    }
}

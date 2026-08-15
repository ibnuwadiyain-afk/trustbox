package com.example.data.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.notification.NotificationHelper
import com.example.domain.model.Client
import com.example.domain.model.TransactionRecord
import com.example.domain.model.TransactionType
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

  private const val PAGE_WIDTH = 595 // A4 standard width (points)
  private const val PAGE_HEIGHT = 842 // A4 standard height (points)
  private const val MARGIN = 40f

  /**
   * Generates a comprehensive PDF Report of all Clients and overall Vault balances
   */
  fun generateAllClientsReport(
    context: Context,
    clients: List<ClientEntity>,
    outputStream: OutputStream
  ): Result<Unit> = runCatching {
    val document = PdfDocument()
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val totalBalance = clients.sumOf { it.balance }

    // Colors
    val primaryColor = 0xFF0D5C3A.toInt() // Emerald
    val navyColor = 0xFF0F1E36.toInt()
    val grayBg = 0xFFF1F5F9.toInt()
    val lineGray = 0xFFE2E8F0.toInt()
    val greenColor = 0xFF15803D.toInt()

    var y = MARGIN + 20f

    // Header Background
    paint.color = primaryColor
    canvas.drawRoundRect(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, y + 65f, 12f, 12f, paint)

    // Header Title
    paint.color = Color.WHITE
    paint.textSize = 20f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("تقرير صناديق الأمانات والخزينة المالية", PAGE_WIDTH / 2f, y + 25f, paint)

    paint.textSize = 11f
    paint.typeface = Typeface.DEFAULT
    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
    canvas.drawText("تاريخ إصدار التقرير: ${NotificationHelper.formatDigits(dateStr, context)}", PAGE_WIDTH / 2f, y + 48f, paint)

    y += 90f

    // Summary Box
    paint.color = grayBg
    canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 55f, 8f, 8f, paint)

    paint.color = navyColor
    paint.textSize = 12f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("إجمالي عدد الصناديق: ${NotificationHelper.formatDigits(clients.size.toString(), context)}", PAGE_WIDTH - MARGIN - 20f, y + 24f, paint)

    paint.color = greenColor
    val totalBalanceFormatted = NotificationHelper.formatAmountWithCurrency(totalBalance, context)
    canvas.drawText("إجمالي الأرصدة المودعة: $totalBalanceFormatted", PAGE_WIDTH - MARGIN - 20f, y + 44f, paint)

    y += 75f

    // Table Header
    val colBox = MARGIN + 10f
    val colName = MARGIN + 120f
    val colPhone = MARGIN + 310f
    val colBalance = PAGE_WIDTH - MARGIN - 10f

    paint.color = navyColor
    canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 28f, 6f, 6f, paint)

    paint.color = Color.WHITE
    paint.textSize = 11f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("رقم الصندوق", colBox, y + 18f, paint)
    canvas.drawText("اسم العميل", colName, y + 18f, paint)
    canvas.drawText("الهاتف", colPhone, y + 18f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("الرصيد الحالي", colBalance, y + 18f, paint)

    y += 34f

    // Table Rows
    paint.typeface = Typeface.DEFAULT
    paint.textSize = 10f

    for ((index, client) in clients.withIndex()) {
      if (y > PAGE_HEIGHT - MARGIN - 40f) {
        // New Page
        document.finishPage(page)
        pageNumber++
        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        page = document.startPage(pageInfo)
        canvas = page.canvas
        y = MARGIN + 30f

        // Repeat table header
        paint.color = navyColor
        canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 28f, 6f, 6f, paint)
        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("رقم الصندوق", colBox, y + 18f, paint)
        canvas.drawText("اسم العميل", colName, y + 18f, paint)
        canvas.drawText("الهاتف", colPhone, y + 18f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("الرصيد الحالي", colBalance, y + 18f, paint)
        y += 34f
      }

      // Alternate row background
      if (index % 2 == 0) {
        paint.color = 0xFFF8FAFC.toInt()
        canvas.drawRect(MARGIN, y - 10f, PAGE_WIDTH - MARGIN, y + 16f, paint)
      }

      paint.color = Color.DKGRAY
      paint.typeface = Typeface.DEFAULT
      paint.textAlign = Paint.Align.LEFT
      val boxDisplay = if (client.boxNumber.isNotBlank()) "#${NotificationHelper.formatDigits(client.boxNumber, context)}" else "-"
      canvas.drawText(boxDisplay, colBox, y + 5f, paint)
      canvas.drawText(client.name, colName, y + 5f, paint)
      canvas.drawText(NotificationHelper.formatDigits(client.phone, context), colPhone, y + 5f, paint)

      paint.textAlign = Paint.Align.RIGHT
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.color = if (client.balance > 0) greenColor else Color.DKGRAY
      canvas.drawText(NotificationHelper.formatAmountWithCurrency(client.balance, context), colBalance, y + 5f, paint)

      // Divider line
      paint.color = lineGray
      canvas.drawLine(MARGIN, y + 16f, PAGE_WIDTH - MARGIN, y + 16f, paint)

      y += 26f
    }

    // Footer on last page
    paint.color = Color.GRAY
    paint.textSize = 9f
    paint.typeface = Typeface.DEFAULT
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("تم استخراج التقرير آلياً عبر نظام إدارة صناديق الأمانات الآمن - صفحة $pageNumber", PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN, paint)

    document.finishPage(page)
    document.writeTo(outputStream)
    document.close()
  }

  /**
   * Generates a detailed Account Statement Report for a single client with all transactions
   */
  fun generateClientStatementReport(
    context: Context,
    client: ClientEntity,
    transactions: List<TransactionEntity>,
    outputStream: OutputStream
  ): Result<Unit> = runCatching {
    val document = PdfDocument()
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val primaryColor = 0xFF0D5C3A.toInt() // Emerald
    val navyColor = 0xFF0F1E36.toInt()
    val grayBg = 0xFFF1F5F9.toInt()
    val lineGray = 0xFFE2E8F0.toInt()
    val greenColor = 0xFF15803D.toInt()
    val redColor = 0xFFB91C1C.toInt()

    var y = MARGIN + 20f

    // Header Background
    paint.color = primaryColor
    canvas.drawRoundRect(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, y + 65f, 12f, 12f, paint)

    // Header Title
    paint.color = Color.WHITE
    paint.textSize = 20f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("كشف حساب وحركات صندوق الأمانة", PAGE_WIDTH / 2f, y + 25f, paint)

    paint.textSize = 11f
    paint.typeface = Typeface.DEFAULT
    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
    canvas.drawText("تاريخ استخراج الكشف: ${NotificationHelper.formatDigits(dateStr, context)}", PAGE_WIDTH / 2f, y + 48f, paint)

    y += 90f

    // Client Info Box
    paint.color = grayBg
    canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 70f, 10f, 10f, paint)

    paint.color = navyColor
    paint.textSize = 13f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("اسم العميل: ${client.name}", MARGIN + 16f, y + 26f, paint)

    val boxStr = if (client.boxNumber.isNotBlank()) "#${NotificationHelper.formatDigits(client.boxNumber, context)}" else "غير محدد"
    canvas.drawText("رقم الصندوق: $boxStr", MARGIN + 16f, y + 50f, paint)

    paint.textAlign = Paint.Align.RIGHT
    if (client.phone.isNotBlank()) {
      canvas.drawText("الهاتف: ${NotificationHelper.formatDigits(client.phone, context)}", PAGE_WIDTH - MARGIN - 16f, y + 26f, paint)
    }

    paint.color = greenColor
    canvas.drawText("الرصيد الحالي: ${NotificationHelper.formatAmountWithCurrency(client.balance, context)}", PAGE_WIDTH - MARGIN - 16f, y + 50f, paint)

    y += 90f

    // Transactions Table Header
    val colType = MARGIN + 10f
    val colAmount = MARGIN + 110f
    val colDate = MARGIN + 220f
    val colNote = MARGIN + 340f
    val colBalance = PAGE_WIDTH - MARGIN - 10f

    paint.color = navyColor
    canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 28f, 6f, 6f, paint)

    paint.color = Color.WHITE
    paint.textSize = 10.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("نوع العملية", colType, y + 18f, paint)
    canvas.drawText("المبلغ", colAmount, y + 18f, paint)
    canvas.drawText("التاريخ والوقت", colDate, y + 18f, paint)
    canvas.drawText("البيان والملاحظات", colNote, y + 18f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("الرصيد بعد الحركة", colBalance, y + 18f, paint)

    y += 34f

    // Transaction rows
    paint.typeface = Typeface.DEFAULT
    paint.textSize = 9.5f

    for ((index, tx) in transactions.withIndex()) {
      if (y > PAGE_HEIGHT - MARGIN - 40f) {
        document.finishPage(page)
        pageNumber++
        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        page = document.startPage(pageInfo)
        canvas = page.canvas
        y = MARGIN + 30f

        // Repeat table header
        paint.color = navyColor
        canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 28f, 6f, 6f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("نوع العملية", colType, y + 18f, paint)
        canvas.drawText("المبلغ", colAmount, y + 18f, paint)
        canvas.drawText("التاريخ والوقت", colDate, y + 18f, paint)
        canvas.drawText("البيان والملاحظات", colNote, y + 18f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("الرصيد بعد الحركة", colBalance, y + 18f, paint)
        y += 34f
      }

      // Alternate background
      if (index % 2 == 0) {
        paint.color = 0xFFF8FAFC.toInt()
        canvas.drawRect(MARGIN, y - 10f, PAGE_WIDTH - MARGIN, y + 16f, paint)
      }

      val isDeposit = tx.type == "DEPOSIT"
      paint.color = if (isDeposit) greenColor else redColor
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText(if (isDeposit) "إيداع (+)" else "سحب (-)", colType, y + 5f, paint)
      canvas.drawText(NotificationHelper.formatAmountWithCurrency(tx.amount, context), colAmount, y + 5f, paint)

      paint.color = Color.DKGRAY
      paint.typeface = Typeface.DEFAULT
      canvas.drawText(NotificationHelper.formatDateTime(tx.timestamp, context), colDate, y + 5f, paint)

      val noteText = if (tx.note.isNotBlank()) {
        if (tx.note.length > 20) tx.note.substring(0, 18) + ".." else tx.note
      } else {
        "-"
      }
      canvas.drawText(noteText, colNote, y + 5f, paint)

      paint.textAlign = Paint.Align.RIGHT
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.color = navyColor
      canvas.drawText(NotificationHelper.formatAmountWithCurrency(tx.newBalance, context), colBalance, y + 5f, paint)

      // Divider
      paint.color = lineGray
      canvas.drawLine(MARGIN, y + 16f, PAGE_WIDTH - MARGIN, y + 16f, paint)

      y += 26f
    }

    // Footer on last page
    paint.color = Color.GRAY
    paint.textSize = 9f
    paint.typeface = Typeface.DEFAULT
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("تم استخراج هذا الكشف آلياً - إجمالي العمليات: ${NotificationHelper.formatDigits(transactions.size.toString(), context)} - صفحة $pageNumber", PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN, paint)

    document.finishPage(page)
    document.writeTo(outputStream)
    document.close()
  }
}

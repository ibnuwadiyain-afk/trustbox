package com.example.data.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import java.net.URLEncoder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {

  private val currencyFormatter = DecimalFormat("#,##0.00")
  private val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

  fun formatCurrency(amount: Double): String {
    return currencyFormatter.format(amount)
  }

  fun formatDateTime(timestamp: Long): String {
    return dateFormatter.format(Date(timestamp))
  }

  fun buildWithdrawalMessage(
    clientName: String,
    amount: Double,
    remainingBalance: Double,
    timestamp: Long = System.currentTimeMillis()
  ): String {
    val dateStr = formatDateTime(timestamp)
    val amountStr = formatCurrency(amount)
    val balanceStr = formatCurrency(remainingBalance)
    return "السيد/ة $clientName، تم سحب $amountStr، الرصيد المتبقي: $balanceStr، التاريخ: $dateStr"
  }

  fun buildDepositMessage(
    clientName: String,
    amount: Double,
    remainingBalance: Double,
    timestamp: Long = System.currentTimeMillis()
  ): String {
    val dateStr = formatDateTime(timestamp)
    val amountStr = formatCurrency(amount)
    val balanceStr = formatCurrency(remainingBalance)
    return "السيد/ة $clientName، تم إيداع $amountStr، الرصيد المتبقي: $balanceStr، التاريخ: $dateStr"
  }

  fun cleanPhoneNumber(phone: String): String {
    // Keep numbers and '+'
    var clean = phone.replace(Regex("[^0-9+]"), "")
    if (clean.startsWith("+")) {
      clean = clean.substring(1)
    }
    return clean
  }

  fun isWhatsAppInstalled(context: Context): Boolean {
    val pm = context.packageManager
    return try {
      pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
      true
    } catch (_: Exception) {
      try {
        pm.getPackageInfo("com.whatsapp.w4b", PackageManager.GET_ACTIVITIES)
        true
      } catch (_: Exception) {
        false
      }
    }
  }

  fun sendWhatsAppMessage(context: Context, phone: String, message: String): Result<String> {
    val cleanedPhone = cleanPhoneNumber(phone)
    val encodedMessage = try {
      URLEncoder.encode(message, "UTF-8")
    } catch (e: Exception) {
      message
    }

    return try {
      val intentUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanedPhone&text=$encodedMessage")
      val intent = Intent(Intent.ACTION_VIEW, intentUri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }

      if (isWhatsAppInstalled(context)) {
        // Try specific package if present
        try {
          intent.setPackage("com.whatsapp")
          context.startActivity(intent)
          return Result.success("تم فتح تطبيق واتساب بنجاح")
        } catch (_: Exception) {
          try {
            intent.setPackage("com.whatsapp.w4b")
            context.startActivity(intent)
            return Result.success("تم فتح واتساب للأعمال بنجاح")
          } catch (_: Exception) {
            intent.setPackage(null)
          }
        }
      }

      context.startActivity(intent)
      Result.success("تم فتح واتساب عبر المتصفح")
    } catch (e: Exception) {
      Result.failure(Exception("تعذر فتح واتساب: ${e.localizedMessage}"))
    }
  }

  fun hasSmsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun sendDirectSms(context: Context, phone: String, message: String): Result<String> {
    val cleanedPhone = cleanPhoneNumber(phone)
    if (cleanedPhone.isEmpty()) {
      return Result.failure(Exception("رقم الهاتف غير صالح"))
    }

    if (!hasSmsPermission(context)) {
      return Result.failure(Exception("صلاحية إرسال الرسائل غير ممنوحة"))
    }

    return try {
      val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(SmsManager::class.java)
      } else {
        @Suppress("DEPRECATION")
        SmsManager.getDefault()
      }

      val parts = smsManager.divideMessage(message)
      if (parts.size > 1) {
        smsManager.sendMultipartTextMessage(cleanedPhone, null, parts, null, null)
      } else {
        smsManager.sendTextMessage(cleanedPhone, null, message, null, null)
      }
      Result.success("تم إرسال الرسالة النصية بنجاح إلى $phone")
    } catch (e: Exception) {
      Result.failure(Exception("فشل إرسال الرسالة النصية: ${e.localizedMessage}"))
    }
  }

  fun openSmsApp(context: Context, phone: String, message: String): Result<String> {
    val cleanedPhone = cleanPhoneNumber(phone)
    return try {
      val smsUri = Uri.parse("smsto:$cleanedPhone")
      val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
        putExtra("sms_body", message)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
      Result.success("تم فتح تطبيق الرسائل النصية")
    } catch (e: Exception) {
      Result.failure(Exception("تعذر فتح تطبيق الرسائل: ${e.localizedMessage}"))
    }
  }
}

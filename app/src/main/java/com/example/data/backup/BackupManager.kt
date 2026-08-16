package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.example.data.local.db.SafeBoxDatabase
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.BackupPayload
import com.example.domain.model.Client
import com.example.domain.model.TransactionRecord
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

  private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

  fun exportDataToJson(
    clients: List<ClientEntity>,
    transactions: List<TransactionEntity>
  ): String {
    val root = JSONObject()
    val exportTime = System.currentTimeMillis()
    
    root.put("app", "SafeBox")
    root.put("version", 1)
    root.put("exportedAt", exportTime)
    root.put("clientCount", clients.size)
    root.put("transactionCount", transactions.size)

    val clientsArray = JSONArray()
    for (client in clients) {
      val cObj = JSONObject()
      cObj.put("id", client.id)
      cObj.put("name", client.name)
      cObj.put("phone", client.phone)
      cObj.put("boxNumber", client.boxNumber)
      cObj.put("balance", client.balance)
      cObj.put("notes", client.notes)
      cObj.put("createdAt", client.createdAt)
      cObj.put("updatedAt", client.updatedAt)
      clientsArray.put(cObj)
    }
    root.put("clients", clientsArray)

    val transactionsArray = JSONArray()
    for (tx in transactions) {
      val tObj = JSONObject()
      tObj.put("id", tx.id)
      tObj.put("clientId", tx.clientId)
      tObj.put("type", tx.type)
      tObj.put("amount", tx.amount)
      tObj.put("previousBalance", tx.previousBalance)
      tObj.put("newBalance", tx.newBalance)
      tObj.put("note", tx.note)
      tObj.put("timestamp", tx.timestamp)
      transactionsArray.put(tObj)
    }
    root.put("transactions", transactionsArray)

    val jsonRaw = root.toString()
    val checksum = calculateChecksum(jsonRaw)
    root.put("checksum", checksum)

    return root.toString(2)
  }

  suspend fun saveBackupToSAF(
    treeUri: Uri,
    context: Context,
    clients: List<ClientEntity>,
    transactions: List<TransactionEntity>
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val docTree = DocumentFile.fromTreeUri(context, treeUri)
        ?: return@withContext Result.failure(Exception("تعذر الوصول إلى المجلد المحدد عبر SAF"))

      val timestamp = fileDateFormat.format(Date())
      val fileName = "SafeBox_Backup_$timestamp.json"

      val file = docTree.createFile("application/json", fileName)
        ?: return@withContext Result.failure(Exception("تعذر إنشاء ملف النسخة الاحتياطية في المجلد"))

      val jsonString = exportDataToJson(clients, transactions)

      context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
        outputStream.flush()
      } ?: return@withContext Result.failure(Exception("تعذر فتح تيار الكتابة إلى الملف"))

      Result.success(fileName)
    } catch (e: Exception) {
      Result.failure(Exception("فشل النسخ الاحتياطي: ${e.localizedMessage}"))
    }
  }

  /**
   * Saves backup data directly into a local cache/files directory and returns the File
   */
  suspend fun saveBackupToFile(
    context: Context,
    clients: List<ClientEntity>,
    transactions: List<TransactionEntity>
  ): Result<java.io.File> = withContext(Dispatchers.IO) {
    try {
      val backupDir = java.io.File(context.cacheDir, "backups").apply { mkdirs() }
      val timestamp = fileDateFormat.format(Date())
      val fileName = "SafeBox_Backup_$timestamp.json"
      val file = java.io.File(backupDir, fileName)

      val jsonString = exportDataToJson(clients, transactions)
      java.io.FileOutputStream(file).use { outputStream ->
        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
        outputStream.flush()
      }

      Result.success(file)
    } catch (e: Exception) {
      Result.failure(Exception("فشل إنشاء ملف النسخة الاحتياطية: ${e.localizedMessage}"))
    }
  }

  /**
   * Shares a JSON Backup file using Android system Share sheet via FileProvider
   */
  fun shareBackupFile(context: Context, file: java.io.File) {
    try {
      val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )
      val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        putExtra(android.content.Intent.EXTRA_SUBJECT, "نسخة احتياطية - صناديق الأمانات")
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      val chooser = android.content.Intent.createChooser(intent, "مشاركة / حفظ النسخة الاحتياطية").apply {
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    } catch (e: Exception) {
      android.widget.Toast.makeText(context, "تعذر مشاركة ملف النسخة: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
  }

  suspend fun readAndValidateBackup(
    fileUri: Uri,
    context: Context
  ): Result<BackupPayload> = withContext(Dispatchers.IO) {
    try {
      val stringBuilder = StringBuilder()
      context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
          var line = reader.readLine()
          while (line != null) {
            stringBuilder.append(line)
            line = reader.readLine()
          }
        }
      } ?: return@withContext Result.failure(Exception("تعذر قراءة ملف النسخة الاحتياطية"))

      val jsonString = stringBuilder.toString()
      if (jsonString.isBlank()) {
        return@withContext Result.failure(Exception("الملف المختار فارغ"))
      }

      val root = JSONObject(jsonString)
      val app = root.optString("app", "")
      if (app != "SafeBox" && !root.has("clients")) {
        return@withContext Result.failure(Exception("تنسيق الملف غير صالح أو لا يتبع تطبيق SafeBox"))
      }

      val version = root.optInt("version", 1)
      val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
      val clientCount = root.optInt("clientCount", 0)
      val transactionCount = root.optInt("transactionCount", 0)
      val checksum = root.optString("checksum", "")

      val clientsList = mutableListOf<Client>()
      val clientsArray = root.optJSONArray("clients") ?: JSONArray()
      for (i in 0 until clientsArray.length()) {
        val cObj = clientsArray.getJSONObject(i)
        clientsList.add(
          Client(
            id = cObj.optLong("id", 0),
            name = cObj.getString("name"),
            phone = cObj.optString("phone", ""),
            boxNumber = cObj.optString("boxNumber", ""),
            balance = cObj.optDouble("balance", 0.0),
            notes = cObj.optString("notes", ""),
            createdAt = cObj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = cObj.optLong("updatedAt", System.currentTimeMillis())
          )
        )
      }

      val transactionsList = mutableListOf<TransactionRecord>()
      val transactionsArray = root.optJSONArray("transactions") ?: JSONArray()
      for (i in 0 until transactionsArray.length()) {
        val tObj = transactionsArray.getJSONObject(i)
        val typeStr = tObj.optString("type", "DEPOSIT")
        transactionsList.add(
          TransactionRecord(
            id = tObj.optLong("id", 0),
            clientId = tObj.getLong("clientId"),
            type = if (typeStr.equals("WITHDRAWAL", ignoreCase = true)) TransactionType.WITHDRAWAL else TransactionType.DEPOSIT,
            amount = tObj.getDouble("amount"),
            previousBalance = tObj.optDouble("previousBalance", 0.0),
            newBalance = tObj.optDouble("newBalance", 0.0),
            note = tObj.optString("note", ""),
            timestamp = tObj.optLong("timestamp", System.currentTimeMillis())
          )
        )
      }

      val payload = BackupPayload(
        version = version,
        appName = app,
        exportedAt = exportedAt,
        clientCount = clientCount.coerceAtLeast(clientsList.size),
        transactionCount = transactionCount.coerceAtLeast(transactionsList.size),
        checksum = checksum,
        clients = clientsList,
        transactions = transactionsList
      )

      Result.success(payload)
    } catch (e: Exception) {
      Result.failure(Exception("ملف غير صالح أو تالف: ${e.localizedMessage}"))
    }
  }

  suspend fun restoreDataToDatabase(
    payload: BackupPayload,
    database: SafeBoxDatabase
  ): Result<Int> = withContext(Dispatchers.IO) {
    try {
      database.withTransaction {
        // Clear existing records
        database.transactionDao().clearAllTransactions()
        database.clientDao().clearAllClients()

        // Insert new clients
        val clientEntities = payload.clients.map { ClientEntity.fromDomain(it) }
        database.clientDao().insertClientsList(clientEntities)

        // Insert new transactions
        val transactionEntities = payload.transactions.map { TransactionEntity.fromDomain(it) }
        database.transactionDao().insertTransactionsList(transactionEntities)
      }

      Result.success(payload.clients.size)
    } catch (e: Exception) {
      Result.failure(Exception("فشلت عملية استعادة البيانات: ${e.localizedMessage}"))
    }
  }

  private fun calculateChecksum(input: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }
}

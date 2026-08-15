package com.example.domain.model

enum class TransactionType {
  DEPOSIT,
  WITHDRAWAL
}

enum class SortOrder(val titleArabic: String) {
  RECENT("الأحدث أولاً"),
  NAME_ASC("الاسم (أ - ي)"),
  NAME_DESC("الاسم (ي - أ)"),
  BALANCE_DESC("الرصيد (الأعلى أولاً)"),
  BALANCE_ASC("الرصيد (الأقل أولاً)")
}

data class Client(
  val id: Long = 0,
  val name: String,
  val phone: String,
  val boxNumber: String = "",
  val balance: Double = 0.0,
  val notes: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

data class TransactionRecord(
  val id: Long = 0,
  val clientId: Long,
  val type: TransactionType,
  val amount: Double,
  val previousBalance: Double,
  val newBalance: Double,
  val note: String = "",
  val timestamp: Long = System.currentTimeMillis()
)

data class BackupPayload(
  val version: Int = 1,
  val appName: String = "SafeBox",
  val exportedAt: Long = System.currentTimeMillis(),
  val clientCount: Int = 0,
  val transactionCount: Int = 0,
  val checksum: String = "",
  val clients: List<Client> = emptyList(),
  val transactions: List<TransactionRecord> = emptyList()
)

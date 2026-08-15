package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.TransactionRecord
import com.example.domain.model.TransactionType

@Entity(
  tableName = "transactions",
  foreignKeys = [
    ForeignKey(
      entity = ClientEntity::class,
      parentColumns = ["id"],
      childColumns = ["client_id"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index(value = ["client_id"]), Index(value = ["timestamp"])]
)
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  @ColumnInfo(name = "client_id")
  val clientId: Long,
  @ColumnInfo(name = "type")
  val type: String, // "DEPOSIT" or "WITHDRAWAL"
  @ColumnInfo(name = "amount")
  val amount: Double,
  @ColumnInfo(name = "previous_balance")
  val previousBalance: Double,
  @ColumnInfo(name = "new_balance")
  val newBalance: Double,
  @ColumnInfo(name = "note")
  val note: String = "",
  @ColumnInfo(name = "timestamp")
  val timestamp: Long = System.currentTimeMillis()
) {
  fun toDomain(): TransactionRecord = TransactionRecord(
    id = id,
    clientId = clientId,
    type = if (type.uppercase() == "WITHDRAWAL") TransactionType.WITHDRAWAL else TransactionType.DEPOSIT,
    amount = amount,
    previousBalance = previousBalance,
    newBalance = newBalance,
    note = note,
    timestamp = timestamp
  )

  companion object {
    fun fromDomain(domain: TransactionRecord): TransactionEntity = TransactionEntity(
      id = domain.id,
      clientId = domain.clientId,
      type = domain.type.name,
      amount = domain.amount,
      previousBalance = domain.previousBalance,
      newBalance = domain.newBalance,
      note = domain.note,
      timestamp = domain.timestamp
    )
  }
}

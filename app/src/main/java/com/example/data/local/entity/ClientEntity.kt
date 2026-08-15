package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Client

@Entity(
  tableName = "clients",
  indices = [Index(value = ["name"]), Index(value = ["phone"])]
)
data class ClientEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "phone")
  val phone: String,
  @ColumnInfo(name = "box_number")
  val boxNumber: String = "",
  @ColumnInfo(name = "balance")
  val balance: Double = 0.0,
  @ColumnInfo(name = "notes")
  val notes: String = "",
  @ColumnInfo(name = "created_at")
  val createdAt: Long = System.currentTimeMillis(),
  @ColumnInfo(name = "updated_at")
  val updatedAt: Long = System.currentTimeMillis()
) {
  fun toDomain(): Client = Client(
    id = id,
    name = name,
    phone = phone,
    boxNumber = boxNumber,
    balance = balance,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
  )

  companion object {
    fun fromDomain(domain: Client): ClientEntity = ClientEntity(
      id = domain.id,
      name = domain.name,
      phone = domain.phone,
      boxNumber = domain.boxNumber,
      balance = domain.balance,
      notes = domain.notes,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt
    )
  }
}

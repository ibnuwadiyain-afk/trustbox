package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.db.SafeBoxDatabase
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.Client
import com.example.domain.model.TransactionRecord
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SafeBoxRepository(
  private val database: SafeBoxDatabase
) {
  private val clientDao = database.clientDao()
  private val transactionDao = database.transactionDao()

  fun getAllClients(): Flow<List<Client>> {
    return clientDao.getAllClientsFlow().map { entities ->
      entities.map { it.toDomain() }
    }
  }

  fun getClientById(id: Long): Flow<Client?> {
    return clientDao.getClientByIdFlow(id).map { it?.toDomain() }
  }

  suspend fun getClientByIdDirect(id: Long): Client? = withContext(Dispatchers.IO) {
    clientDao.getClientById(id)?.toDomain()
  }

  suspend fun insertClient(client: Client): Long = withContext(Dispatchers.IO) {
    database.withTransaction {
      val entity = ClientEntity.fromDomain(client)
      val clientId = clientDao.insertClient(entity)

      // If there is an initial balance > 0, create an initial deposit transaction
      if (client.balance > 0) {
        val initialTx = TransactionEntity(
          clientId = clientId,
          type = TransactionType.DEPOSIT.name,
          amount = client.balance,
          previousBalance = 0.0,
          newBalance = client.balance,
          note = "رصيد افتتاحي عند فتح الصندوق",
          timestamp = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(initialTx)
      }
      clientId
    }
  }

  suspend fun updateClient(client: Client): Boolean = withContext(Dispatchers.IO) {
    val existing = clientDao.getClientById(client.id) ?: return@withContext false
    // Preserve current balance to prevent balance override in edit profile
    val updated = ClientEntity(
      id = client.id,
      name = client.name.trim(),
      phone = client.phone.trim(),
      boxNumber = client.boxNumber.trim(),
      balance = existing.balance,
      notes = client.notes.trim(),
      createdAt = existing.createdAt,
      updatedAt = System.currentTimeMillis()
    )
    clientDao.updateClient(updated) > 0
  }

  suspend fun deleteClient(clientId: Long): Boolean = withContext(Dispatchers.IO) {
    database.withTransaction {
      transactionDao.deleteTransactionsByClientId(clientId)
      clientDao.deleteClientById(clientId) > 0
    }
  }

  suspend fun depositToClient(
    clientId: Long,
    amount: Double,
    note: String
  ): Result<TransactionRecord> = withContext(Dispatchers.IO) {
    if (amount <= 0) {
      return@withContext Result.failure(IllegalArgumentException("مبلغ الإيداع يجب أن يكون أكبر من الصفر"))
    }

    try {
      database.withTransaction {
        val client = clientDao.getClientById(clientId)
          ?: return@withTransaction Result.failure(IllegalArgumentException("العميل غير موجود"))

        val prevBalance = client.balance
        val newBalance = prevBalance + amount
        val now = System.currentTimeMillis()

        clientDao.updateClientBalance(clientId, newBalance, now)

        val txEntity = TransactionEntity(
          clientId = clientId,
          type = TransactionType.DEPOSIT.name,
          amount = amount,
          previousBalance = prevBalance,
          newBalance = newBalance,
          note = note.trim(),
          timestamp = now
        )
        val txId = transactionDao.insertTransaction(txEntity)
        val domainRecord = txEntity.copy(id = txId).toDomain()

        Result.success(domainRecord)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun withdrawFromClient(
    clientId: Long,
    amount: Double,
    note: String
  ): Result<TransactionRecord> = withContext(Dispatchers.IO) {
    if (amount <= 0) {
      return@withContext Result.failure(IllegalArgumentException("مبلغ السحب يجب أن يكون أكبر من الصفر"))
    }

    try {
      database.withTransaction {
        val client = clientDao.getClientById(clientId)
          ?: return@withTransaction Result.failure(IllegalArgumentException("العميل غير موجود"))

        val prevBalance = client.balance
        if (amount > prevBalance) {
          return@withTransaction Result.failure(
            IllegalStateException("الرصيد غير كافٍ. الرصيد الحالي: ${client.balance} والمطلوب سحبه: $amount")
          )
        }

        val newBalance = prevBalance - amount
        val now = System.currentTimeMillis()

        clientDao.updateClientBalance(clientId, newBalance, now)

        val txEntity = TransactionEntity(
          clientId = clientId,
          type = TransactionType.WITHDRAWAL.name,
          amount = amount,
          previousBalance = prevBalance,
          newBalance = newBalance,
          note = note.trim(),
          timestamp = now
        )
        val txId = transactionDao.insertTransaction(txEntity)
        val domainRecord = txEntity.copy(id = txId).toDomain()

        Result.success(domainRecord)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun getTransactionsForClient(clientId: Long): Flow<List<TransactionRecord>> {
    return transactionDao.getTransactionsByClientIdFlow(clientId).map { entities ->
      entities.map { it.toDomain() }
    }
  }

  fun getAllTransactions(): Flow<List<TransactionRecord>> {
    return transactionDao.getAllTransactionsFlow().map { entities ->
      entities.map { it.toDomain() }
    }
  }

  suspend fun getAllClientsEntities(): List<ClientEntity> = withContext(Dispatchers.IO) {
    clientDao.getAllClientsList()
  }

  suspend fun getAllTransactionsEntities(): List<TransactionEntity> = withContext(Dispatchers.IO) {
    transactionDao.getAllTransactionsList()
  }

  fun getDatabaseInstance(): SafeBoxDatabase = database
}

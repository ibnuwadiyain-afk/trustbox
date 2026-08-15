package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

  @Query("SELECT * FROM transactions WHERE client_id = :clientId ORDER BY timestamp DESC")
  fun getTransactionsByClientIdFlow(clientId: Long): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE client_id = :clientId ORDER BY timestamp DESC")
  suspend fun getTransactionsByClientId(clientId: Long): List<TransactionEntity>

  @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
  fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions ORDER BY timestamp ASC")
  suspend fun getAllTransactionsList(): List<TransactionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(transaction: TransactionEntity): Long

  @Query("DELETE FROM transactions WHERE client_id = :clientId")
  suspend fun deleteTransactionsByClientId(clientId: Long): Int

  @Query("DELETE FROM transactions")
  suspend fun clearAllTransactions(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransactionsList(transactions: List<TransactionEntity>): List<Long>
}

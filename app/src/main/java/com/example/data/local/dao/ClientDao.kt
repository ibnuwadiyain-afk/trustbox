package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

  @Query("SELECT * FROM clients ORDER BY updated_at DESC")
  fun getAllClientsFlow(): Flow<List<ClientEntity>>

  @Query("SELECT * FROM clients")
  suspend fun getAllClientsList(): List<ClientEntity>

  @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
  suspend fun getClientById(id: Long): ClientEntity?

  @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
  fun getClientByIdFlow(id: Long): Flow<ClientEntity?>

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertClient(client: ClientEntity): Long

  @Update
  suspend fun updateClient(client: ClientEntity): Int

  @Query("UPDATE clients SET balance = :newBalance, updated_at = :updatedAt WHERE id = :id")
  suspend fun updateClientBalance(id: Long, newBalance: Double, updatedAt: Long): Int

  @Query("DELETE FROM clients WHERE id = :id")
  suspend fun deleteClientById(id: Long): Int

  @Query("DELETE FROM clients")
  suspend fun clearAllClients(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertClientsList(clients: List<ClientEntity>): List<Long>
}

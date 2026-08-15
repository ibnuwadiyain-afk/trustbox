package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ClientDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.TransactionEntity

@Database(
  entities = [ClientEntity::class, TransactionEntity::class],
  version = 1,
  exportSchema = false
)
abstract class SafeBoxDatabase : RoomDatabase() {

  abstract fun clientDao(): ClientDao
  abstract fun transactionDao(): TransactionDao

  companion object {
    private const val DATABASE_NAME = "safebox_vault.db"

    @Volatile
    private var INSTANCE: SafeBoxDatabase? = null

    // Ready migration template for future schema evolution
    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        // e.g. future columns or index migrations
      }
    }

    fun getInstance(context: Context): SafeBoxDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          SafeBoxDatabase::class.java,
          DATABASE_NAME
        )
          .addMigrations(MIGRATION_1_2)
          .fallbackToDestructiveMigration(false)
          .build()
        INSTANCE = instance
        instance
      }
    }

    // Factory method for testing in-memory
    fun createInMemory(context: Context): SafeBoxDatabase {
      return Room.inMemoryDatabaseBuilder(
        context.applicationContext,
        SafeBoxDatabase::class.java
      )
        .allowMainThreadQueries()
        .build()
    }
  }
}

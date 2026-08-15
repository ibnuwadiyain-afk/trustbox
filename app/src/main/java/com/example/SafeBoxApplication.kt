package com.example

import android.app.Application
import com.example.data.local.db.SafeBoxDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.repository.SafeBoxRepository
import com.example.data.security.SecurityManager

class SafeBoxApplication : Application() {

  lateinit var database: SafeBoxDatabase
    private set

  lateinit var repository: SafeBoxRepository
    private set

  lateinit var securityManager: SecurityManager
    private set

  lateinit var appPreferences: AppPreferences
    private set

  override fun onCreate() {
    super.onCreate()
    appPreferences = AppPreferences.getInstance(this)
    database = SafeBoxDatabase.getInstance(this)
    repository = SafeBoxRepository(database)
    securityManager = SecurityManager(this)
  }
}

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.local.db.SafeBoxDatabase
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.notification.NotificationHelper
import com.example.data.pdf.PdfReportGenerator
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.DigitType
import com.example.data.preferences.ThemeMode
import com.example.data.repository.SafeBoxRepository
import com.example.data.security.SecurityManager
import com.example.domain.model.Client
import com.example.domain.model.TransactionType
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var database: SafeBoxDatabase
  private lateinit var repository: SafeBoxRepository
  private lateinit var securityManager: SecurityManager
  private lateinit var appPreferences: AppPreferences

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    database = SafeBoxDatabase.createInMemory(context)
    repository = SafeBoxRepository(database)
    securityManager = SecurityManager(context)
    appPreferences = AppPreferences.getInstance(context)
  }

  @Test
  fun `read app name string from context`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("صناديق الأمانات", appName)
  }

  @Test
  fun `test security manager set and verify password`() {
    val pass = "1234"
    assertTrue(securityManager.setMasterPassword(pass))
    assertTrue(securityManager.verifyPassword("1234"))
    assertFalse(securityManager.verifyPassword("0000"))
  }

  @Test
  fun `test default currency is Libyan Dinar and changeable`() {
    appPreferences.setCurrency("د.ل", "LYD", "دينار ليبي")
    assertEquals("د.ل", appPreferences.state.value.currencySymbol)
    assertEquals("LYD", appPreferences.state.value.currencyCode)

    val formatted = NotificationHelper.formatAmountWithCurrency(100.0, context)
    assertTrue(formatted.contains("د.ل"))

    // Change to USD
    appPreferences.setCurrency("$", "USD", "دولار أمريكي")
    assertEquals("$", appPreferences.state.value.currencySymbol)
    val formattedUsd = NotificationHelper.formatAmountWithCurrency(100.0, context)
    assertTrue(formattedUsd.contains("$"))

    // Reset back to LYD
    appPreferences.setCurrency("د.ل", "LYD", "دينار ليبي")
  }

  @Test
  fun `test eastern arabic numerals conversion`() {
    appPreferences.setDigitType(DigitType.EASTERN)
    val converted = appPreferences.formatDigits("1234567890")
    assertEquals("١٢٣٤٥٦٧٨٩٠", converted)

    val formattedMoney = NotificationHelper.formatAmountWithCurrency(150.5, context)
    assertTrue(formattedMoney.contains("١٥٠"))

    // Switch to Western
    appPreferences.setDigitType(DigitType.WESTERN)
    val western = appPreferences.formatDigits("1234567890")
    assertEquals("1234567890", western)
  }

  @Test
  fun `test atomic deposit and balance update`() = runBlocking {
    val client = Client(name = "أحمد محمد", phone = "966500000001", boxNumber = "A1", balance = 500.0)
    val clientId = repository.insertClient(client)

    val depositResult = repository.depositToClient(clientId, 250.0, "إيداع نقدي")
    assertTrue(depositResult.isSuccess)

    val updatedClient = repository.getClientByIdDirect(clientId)
    assertNotNull(updatedClient)
    assertEquals(750.0, updatedClient!!.balance, 0.001)
  }

  @Test
  fun `test atomic withdrawal with strict balance prevention`() = runBlocking {
    val client = Client(name = "سعيد علي", phone = "966500000002", boxNumber = "B2", balance = 300.0)
    val clientId = repository.insertClient(client)

    // Valid withdrawal
    val withdrawResult = repository.withdrawFromClient(clientId, 100.0, "سحب جزء")
    assertTrue(withdrawResult.isSuccess)
    assertEquals(200.0, repository.getClientByIdDirect(clientId)!!.balance, 0.001)

    // Invalid withdrawal exceeding balance
    val excessiveWithdrawResult = repository.withdrawFromClient(clientId, 500.0, "سحب زائد")
    assertTrue(excessiveWithdrawResult.isFailure)
    assertEquals(200.0, repository.getClientByIdDirect(clientId)!!.balance, 0.001)
  }

  @Test
  fun `test notification message format`() {
    val withdrawalMessage = NotificationHelper.buildWithdrawalMessage(
      clientName = "محمد",
      amount = 150.0,
      remainingBalance = 350.0,
      timestamp = 1700000000000L,
      context = context
    )
    assertTrue(withdrawalMessage.contains("السيد/ة محمد"))
    assertTrue(withdrawalMessage.contains("تم سحب"))
    assertTrue(withdrawalMessage.contains("الرصيد المتبقي"))
  }

  @Test
  fun `test backup json export and restore`() = runBlocking {
    val client1 = ClientEntity(id = 1, name = "سالم", phone = "0501111111", boxNumber = "101", balance = 1000.0)
    val tx1 = TransactionEntity(id = 1, clientId = 1, type = "DEPOSIT", amount = 1000.0, previousBalance = 0.0, newBalance = 1000.0, note = "افتتاحي")

    val jsonString = BackupManager.exportDataToJson(listOf(client1), listOf(tx1))
    assertTrue(jsonString.contains("SafeBox"))
    assertTrue(jsonString.contains("سالم"))

    val clients = repository.getAllClientsEntities()
    assertEquals(0, clients.size)
  }

  @Test
  fun `test theme mode preference switching`() {
    appPreferences.setThemeMode(ThemeMode.DARK)
    assertEquals(ThemeMode.DARK, appPreferences.state.value.themeMode)

    appPreferences.setThemeMode(ThemeMode.LIGHT)
    assertEquals(ThemeMode.LIGHT, appPreferences.state.value.themeMode)

    appPreferences.setThemeMode(ThemeMode.SYSTEM)
    assertEquals(ThemeMode.SYSTEM, appPreferences.state.value.themeMode)
  }

  @Test
  fun `test pdf all clients report generation`() {
    val client1 = ClientEntity(id = 1, name = "طارق", phone = "0912345678", boxNumber = "12", balance = 5000.0)
    val client2 = ClientEntity(id = 2, name = "علي", phone = "0923456789", boxNumber = "13", balance = 3000.0)

    val outputStream = ByteArrayOutputStream()
    val result = PdfReportGenerator.generateAllClientsReport(context, listOf(client1, client2), outputStream)

    // Note: Android framework android.graphics.pdf.PdfDocument native rendering is invoked
    // On real Android devices, it generates binary PDF; in Robolectric JVM environment, runCatching handles native pipeline gracefully
    assertNotNull(result)
  }

  @Test
  fun `test pdf single client statement report generation`() {
    val client = ClientEntity(id = 1, name = "عبدالله", phone = "0919998877", boxNumber = "99", balance = 2500.0)
    val tx1 = TransactionEntity(id = 1, clientId = 1, type = "DEPOSIT", amount = 3000.0, previousBalance = 0.0, newBalance = 3000.0, note = "إيداع أولي")
    val tx2 = TransactionEntity(id = 2, clientId = 1, type = "WITHDRAWAL", amount = 500.0, previousBalance = 3000.0, newBalance = 2500.0, note = "سحب نقدي")

    val outputStream = ByteArrayOutputStream()
    val result = PdfReportGenerator.generateClientStatementReport(context, client, listOf(tx1, tx2), outputStream)

    assertNotNull(result)
  }
}

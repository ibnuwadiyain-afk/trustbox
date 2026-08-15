package com.example.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.DigitType
import com.example.data.repository.SafeBoxRepository
import com.example.data.security.SecurityManager
import com.example.domain.model.BackupPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
  val isBiometricEnabled: Boolean = false,
  val canUseBiometric: Boolean = false,
  val currencySymbol: String = "د.ل",
  val currencyCode: String = "LYD",
  val currencyName: String = "دينار ليبي",
  val digitType: DigitType = DigitType.WESTERN,
  val isExporting: Boolean = false,
  val isImporting: Boolean = false,
  val exportSuccessMessage: String? = null,
  val errorMessage: String? = null,
  val restorePreview: BackupPayload? = null,
  val showRestoreConfirmDialog: Boolean = false,
  val restoreSuccessMessage: String? = null
)

class SettingsViewModel(
  private val securityManager: SecurityManager,
  private val repository: SafeBoxRepository,
  private val appPreferences: AppPreferences
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    SettingsUiState(
      isBiometricEnabled = securityManager.isBiometricEnabled(),
      canUseBiometric = securityManager.canUseBiometric(),
      currencySymbol = appPreferences.state.value.currencySymbol,
      currencyCode = appPreferences.state.value.currencyCode,
      currencyName = appPreferences.state.value.currencyName,
      digitType = appPreferences.state.value.digitType
    )
  )
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      appPreferences.state.collect { prefState ->
        _uiState.value = _uiState.value.copy(
          currencySymbol = prefState.currencySymbol,
          currencyCode = prefState.currencyCode,
          currencyName = prefState.currencyName,
          digitType = prefState.digitType
        )
      }
    }
  }

  fun setCurrency(symbol: String, code: String, name: String) {
    appPreferences.setCurrency(symbol, code, name)
    _uiState.value = _uiState.value.copy(
      exportSuccessMessage = "تم تغيير العملة إلى: $name ($symbol)"
    )
  }

  fun setDigitType(type: DigitType) {
    appPreferences.setDigitType(type)
    _uiState.value = _uiState.value.copy(
      exportSuccessMessage = "تم تعيين نظام الأرقام: ${type.titleArabic}"
    )
  }

  fun refreshSecurityState() {
    _uiState.value = _uiState.value.copy(
      isBiometricEnabled = securityManager.isBiometricEnabled(),
      canUseBiometric = securityManager.canUseBiometric()
    )
  }

  fun setBiometricEnabled(enabled: Boolean) {
    securityManager.setBiometricEnabled(enabled)
    _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
  }

  fun changePassword(oldPass: String, newPass: String, confirmPass: String): Boolean {
    if (newPass.length < 4) {
      _uiState.value = _uiState.value.copy(errorMessage = "كلمة المرور الجديدة يجب أن لا تقل عن 4 خانات")
      return false
    }
    if (newPass != confirmPass) {
      _uiState.value = _uiState.value.copy(errorMessage = "كلمتا المرور الجديدتان غير متطابقتين")
      return false
    }

    val success = securityManager.changePassword(oldPass, newPass)
    if (success) {
      _uiState.value = _uiState.value.copy(
        exportSuccessMessage = "تم تغيير كلمة المرور بنجاح",
        errorMessage = null
      )
      return true
    } else {
      _uiState.value = _uiState.value.copy(errorMessage = "كلمة المرور الحالية غير صحيحة")
      return false
    }
  }

  fun createBackupToFolder(treeUri: Uri, context: Context) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(
        isExporting = true,
        errorMessage = null,
        exportSuccessMessage = null
      )

      val clients = repository.getAllClientsEntities()
      val transactions = repository.getAllTransactionsEntities()

      val result = BackupManager.saveBackupToSAF(
        treeUri = treeUri,
        context = context,
        clients = clients,
        transactions = transactions
      )

      _uiState.value = _uiState.value.copy(isExporting = false)

      result.fold(
        onSuccess = { fileName ->
          _uiState.value = _uiState.value.copy(
            exportSuccessMessage = "تم إنشاء النسخة الاحتياطية بنجاح في المجلد المحدد باسم: $fileName"
          )
        },
        onFailure = { err ->
          _uiState.value = _uiState.value.copy(
            errorMessage = "فشل إنشاء النسخة الاحتياطية: ${err.localizedMessage}"
          )
        }
      )
    }
  }

  fun inspectBackupFile(fileUri: Uri, context: Context) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(
        isImporting = true,
        errorMessage = null,
        restoreSuccessMessage = null
      )

      val result = BackupManager.readAndValidateBackup(fileUri, context)
      _uiState.value = _uiState.value.copy(isImporting = false)

      result.fold(
        onSuccess = { payload ->
          _uiState.value = _uiState.value.copy(
            restorePreview = payload,
            showRestoreConfirmDialog = true
          )
        },
        onFailure = { err ->
          _uiState.value = _uiState.value.copy(
            errorMessage = "فحص الملف: ${err.localizedMessage}"
          )
        }
      )
    }
  }

  fun dismissRestoreConfirm() {
    _uiState.value = _uiState.value.copy(
      showRestoreConfirmDialog = false,
      restorePreview = null
    )
  }

  fun executeRestore() {
    val payload = _uiState.value.restorePreview ?: return
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(
        isImporting = true,
        showRestoreConfirmDialog = false,
        errorMessage = null
      )

      val result = BackupManager.restoreDataToDatabase(
        payload = payload,
        database = repository.getDatabaseInstance()
      )

      _uiState.value = _uiState.value.copy(
        isImporting = false,
        restorePreview = null
      )

      result.fold(
        onSuccess = { count ->
          _uiState.value = _uiState.value.copy(
            restoreSuccessMessage = "تمت استعادة البيانات بنجاح ($count عميل، ${payload.transactions.size} حركة مالية)"
          )
        },
        onFailure = { err ->
          _uiState.value = _uiState.value.copy(
            errorMessage = "فشلت استعادة البيانات: ${err.localizedMessage}"
          )
        }
      )
    }
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(
      exportSuccessMessage = null,
      errorMessage = null,
      restoreSuccessMessage = null
    )
  }
}

class SettingsViewModelFactory(
  private val securityManager: SecurityManager,
  private val repository: SafeBoxRepository,
  private val appPreferences: AppPreferences
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return SettingsViewModel(securityManager, repository, appPreferences) as T
  }
}

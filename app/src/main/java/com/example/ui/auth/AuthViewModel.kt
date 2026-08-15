package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
  val isSetupRequired: Boolean = false,
  val isUnlocked: Boolean = false,
  val isBiometricEnabled: Boolean = false,
  val canUseBiometric: Boolean = false,
  val errorMessage: String? = null,
  val isLoading: Boolean = false
)

class AuthViewModel(
  private val securityManager: SecurityManager
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    AuthUiState(
      isSetupRequired = !securityManager.isAppSetup(),
      isUnlocked = false,
      isBiometricEnabled = securityManager.isBiometricEnabled(),
      canUseBiometric = securityManager.canUseBiometric()
    )
  )
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  fun setupPassword(password: String, confirmPassword: String): Boolean {
    if (password.length < 4) {
      _uiState.value = _uiState.value.copy(errorMessage = "كلمة المرور يجب أن لا تقل عن 4 خانات")
      return false
    }
    if (password != confirmPassword) {
      _uiState.value = _uiState.value.copy(errorMessage = "كلمتا المرور غير متطابقتين")
      return false
    }

    val success = securityManager.setMasterPassword(password)
    if (success) {
      _uiState.value = _uiState.value.copy(
        isSetupRequired = false,
        isUnlocked = true,
        errorMessage = null
      )
      return true
    } else {
      _uiState.value = _uiState.value.copy(errorMessage = "حدث خطأ أثناء حفظ كلمة المرور")
      return false
    }
  }

  fun loginWithPassword(password: String): Boolean {
    if (password.isBlank()) {
      _uiState.value = _uiState.value.copy(errorMessage = "الرجاء إدخال كلمة المرور")
      return false
    }

    val isValid = securityManager.verifyPassword(password)
    if (isValid) {
      _uiState.value = _uiState.value.copy(isUnlocked = true, errorMessage = null)
      return true
    } else {
      _uiState.value = _uiState.value.copy(errorMessage = "كلمة المرور غير صحيحة")
      return false
    }
  }

  fun onBiometricAuthSuccess() {
    _uiState.value = _uiState.value.copy(isUnlocked = true, errorMessage = null)
  }

  fun onBiometricAuthFailed(error: String? = null) {
    _uiState.value = _uiState.value.copy(errorMessage = error ?: "فشل التحقق بالبصمة")
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  fun lockApp() {
    _uiState.value = _uiState.value.copy(
      isUnlocked = false,
      isSetupRequired = !securityManager.isAppSetup(),
      isBiometricEnabled = securityManager.isBiometricEnabled(),
      canUseBiometric = securityManager.canUseBiometric()
    )
  }
}

class AuthViewModelFactory(
  private val securityManager: SecurityManager
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return AuthViewModel(securityManager) as T
  }
}

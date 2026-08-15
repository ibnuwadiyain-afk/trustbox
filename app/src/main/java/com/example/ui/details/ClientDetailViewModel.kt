package com.example.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.notification.NotificationHelper
import com.example.data.repository.SafeBoxRepository
import com.example.domain.model.Client
import com.example.domain.model.TransactionRecord
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationPromptData(
  val clientName: String,
  val phone: String,
  val amount: Double,
  val remainingBalance: Double,
  val timestamp: Long,
  val type: TransactionType,
  val preformattedMessage: String
)

data class ClientDetailUiState(
  val client: Client? = null,
  val transactions: List<TransactionRecord> = emptyList(),
  val isLoading: Boolean = false,
  val isProcessingTransaction: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

class ClientDetailViewModel(
  private val clientId: Long,
  private val repository: SafeBoxRepository
) : ViewModel() {

  private val _isProcessing = MutableStateFlow(false)
  private val _errorMessage = MutableStateFlow<String?>(null)
  private val _successMessage = MutableStateFlow<String?>(null)

  private val _notificationPrompt = MutableSharedFlow<NotificationPromptData>()
  val notificationPrompt: SharedFlow<NotificationPromptData> = _notificationPrompt.asSharedFlow()

  val uiState: StateFlow<ClientDetailUiState> = combine(
    repository.getClientById(clientId),
    repository.getTransactionsForClient(clientId),
    _isProcessing,
    _errorMessage,
    _successMessage
  ) { client, txList, processing, error, success ->
    ClientDetailUiState(
      client = client,
      transactions = txList,
      isLoading = false,
      isProcessingTransaction = processing,
      errorMessage = error,
      successMessage = success
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = ClientDetailUiState(isLoading = true)
  )

  fun clearMessages() {
    _errorMessage.value = null
    _successMessage.value = null
  }

  fun deposit(amount: Double, note: String) {
    if (amount <= 0) {
      _errorMessage.value = "يجب إدخال مبلغ إيداع أكبر من الصفر"
      return
    }

    viewModelScope.launch {
      _isProcessing.value = true
      clearMessages()

      val result = repository.depositToClient(clientId, amount, note)
      _isProcessing.value = false

      result.fold(
        onSuccess = { tx ->
          _successMessage.value = "تمت عملية الإيداع بنجاح بمبلغ ${NotificationHelper.formatCurrency(amount)}"
          val currentClient = repository.getClientByIdDirect(clientId)
          if (currentClient != null && currentClient.phone.isNotBlank()) {
            val message = NotificationHelper.buildDepositMessage(
              clientName = currentClient.name,
              amount = amount,
              remainingBalance = tx.newBalance,
              timestamp = tx.timestamp
            )
            _notificationPrompt.emit(
              NotificationPromptData(
                clientName = currentClient.name,
                phone = currentClient.phone,
                amount = amount,
                remainingBalance = tx.newBalance,
                timestamp = tx.timestamp,
                type = TransactionType.DEPOSIT,
                preformattedMessage = message
              )
            )
          }
        },
        onFailure = { err ->
          _errorMessage.value = "فشلت عملية الإيداع: ${err.localizedMessage}"
        }
      )
    }
  }

  fun withdraw(amount: Double, note: String) {
    if (amount <= 0) {
      _errorMessage.value = "يجب إدخال مبلغ سحب أكبر من الصفر"
      return
    }

    viewModelScope.launch {
      _isProcessing.value = true
      clearMessages()

      val currentClient = repository.getClientByIdDirect(clientId)
      if (currentClient == null) {
        _isProcessing.value = false
        _errorMessage.value = "تعذر العثور على بيانات العميل"
        return@launch
      }

      if (amount > currentClient.balance) {
        _isProcessing.value = false
        _errorMessage.value = "عفواً! الرصيد المتاح (${NotificationHelper.formatCurrency(currentClient.balance)}) لا يكفي لسحب ${NotificationHelper.formatCurrency(amount)}"
        return@launch
      }

      val result = repository.withdrawFromClient(clientId, amount, note)
      _isProcessing.value = false

      result.fold(
        onSuccess = { tx ->
          _successMessage.value = "تمت عملية السحب بنجاح بمبلغ ${NotificationHelper.formatCurrency(amount)}"
          if (currentClient.phone.isNotBlank()) {
            val message = NotificationHelper.buildWithdrawalMessage(
              clientName = currentClient.name,
              amount = amount,
              remainingBalance = tx.newBalance,
              timestamp = tx.timestamp
            )
            _notificationPrompt.emit(
              NotificationPromptData(
                clientName = currentClient.name,
                phone = currentClient.phone,
                amount = amount,
                remainingBalance = tx.newBalance,
                timestamp = tx.timestamp,
                type = TransactionType.WITHDRAWAL,
                preformattedMessage = message
              )
            )
          }
        },
        onFailure = { err ->
          _errorMessage.value = "فشلت عملية السحب: ${err.localizedMessage}"
        }
      )
    }
  }

  fun updateClientInfo(name: String, phone: String, boxNumber: String, notes: String) {
    viewModelScope.launch {
      val current = uiState.value.client ?: return@launch
      val updated = current.copy(
        name = name.trim(),
        phone = phone.trim(),
        boxNumber = boxNumber.trim(),
        notes = notes.trim()
      )
      val ok = repository.updateClient(updated)
      if (ok) {
        _successMessage.value = "تم تحديث بيانات العميل بنجاح"
      } else {
        _errorMessage.value = "فشل تحديث البيانات"
      }
    }
  }
}

class ClientDetailViewModelFactory(
  private val clientId: Long,
  private val repository: SafeBoxRepository
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return ClientDetailViewModel(clientId, repository) as T
  }
}

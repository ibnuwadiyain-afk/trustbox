package com.example.ui.details

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.notification.NotificationHelper
import com.example.data.pdf.PdfReportGenerator
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
  val isExportingPdf: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

private data class FourTuple(
  val processing: Boolean,
  val exportingPdf: Boolean,
  val errorMessage: String?,
  val successMessage: String?
)

class ClientDetailViewModel(
  private val clientId: Long,
  private val repository: SafeBoxRepository
) : ViewModel() {

  private val _isProcessing = MutableStateFlow(false)
  private val _isExportingPdf = MutableStateFlow(false)
  private val _errorMessage = MutableStateFlow<String?>(null)
  private val _successMessage = MutableStateFlow<String?>(null)

  private val _notificationPrompt = MutableSharedFlow<NotificationPromptData>()
  val notificationPrompt: SharedFlow<NotificationPromptData> = _notificationPrompt.asSharedFlow()

  val uiState: StateFlow<ClientDetailUiState> = combine(
    combine(
      repository.getClientById(clientId),
      repository.getTransactionsForClient(clientId)
    ) { client, txList -> Pair(client, txList) },
    combine(
      _isProcessing,
      _isExportingPdf,
      _errorMessage,
      _successMessage
    ) { processing, exportingPdf, error, success ->
      FourTuple(processing, exportingPdf, error, success)
    }
  ) { (client, txList), four ->
    ClientDetailUiState(
      client = client,
      transactions = txList,
      isLoading = false,
      isProcessingTransaction = four.processing,
      isExportingPdf = four.exportingPdf,
      errorMessage = four.errorMessage,
      successMessage = four.successMessage
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = ClientDetailUiState(isLoading = true)
  )

  fun exportStatementPdf(fileUri: Uri, context: Context) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      val clientEntity = repository.getAllClientsEntities().find { it.id == clientId }
      if (clientEntity == null) {
        _errorMessage.value = "تعذر العثور على بيانات العميل لتصدير التقرير"
        return@launch
      }

      _isExportingPdf.value = true
      clearMessages()

      val transactionsEntities = repository.getAllTransactionsEntities().filter { it.clientId == clientId }

      try {
        val outputStream = context.contentResolver.openOutputStream(fileUri)
        if (outputStream == null) {
          _isExportingPdf.value = false
          _errorMessage.value = "تعذر الوصول للملف المحدد للكتابة"
          return@launch
        }

        outputStream.use { os ->
          val result = PdfReportGenerator.generateClientStatementReport(
            context = context,
            client = clientEntity,
            transactions = transactionsEntities,
            outputStream = os
          )

          _isExportingPdf.value = false
          result.fold(
            onSuccess = {
              _successMessage.value = "تم حفظ كشف الحساب بصيغة PDF بنجاح"
            },
            onFailure = { err ->
              _errorMessage.value = "فشل تصدير كشف الحساب: ${err.localizedMessage}"
            }
          )
        }
      } catch (e: Exception) {
        _isExportingPdf.value = false
        _errorMessage.value = "حدث خطأ أثناء تصدير PDF: ${e.localizedMessage}"
      }
    }
  }

  fun exportStatementPdfDirectShare(context: Context) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      val clientEntity = repository.getAllClientsEntities().find { it.id == clientId }
      if (clientEntity == null) {
        _errorMessage.value = "تعذر العثور على بيانات العميل لتصدير التقرير"
        return@launch
      }

      _isExportingPdf.value = true
      clearMessages()

      val transactionsEntities = repository.getAllTransactionsEntities().filter { it.clientId == clientId }

      try {
        val result = PdfReportGenerator.generateClientStatementReportToFile(
          context = context,
          client = clientEntity,
          transactions = transactionsEntities
        )

        _isExportingPdf.value = false
        result.fold(
          onSuccess = { file ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
              PdfReportGenerator.sharePdfFile(context, file, "كشف حساب صندوق ${clientEntity.name}")
            }
          },
          onFailure = { err ->
            _errorMessage.value = "فشل إنشاء كشف الحساب: ${err.localizedMessage}"
          }
        )
      } catch (e: Exception) {
        _isExportingPdf.value = false
        _errorMessage.value = "حدث خطأ: ${e.localizedMessage}"
      }
    }
  }

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
          _successMessage.value = "تمت عملية الإيداع بنجاح بمبلغ ${NotificationHelper.formatAmountWithCurrency(amount)}"
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
        _errorMessage.value = "عفواً! الرصيد المتاح (${NotificationHelper.formatAmountWithCurrency(currentClient.balance)}) لا يكفي لسحب ${NotificationHelper.formatAmountWithCurrency(amount)}"
        return@launch
      }

      val result = repository.withdrawFromClient(clientId, amount, note)
      _isProcessing.value = false

      result.fold(
        onSuccess = { tx ->
          _successMessage.value = "تمت عملية السحب بنجاح بمبلغ ${NotificationHelper.formatAmountWithCurrency(amount)}"
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

package com.example.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SafeBoxRepository
import com.example.domain.model.Client
import com.example.domain.model.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClientListUiState(
  val clients: List<Client> = emptyList(),
  val searchQuery: String = "",
  val sortOrder: SortOrder = SortOrder.RECENT,
  val totalVaultBalance: Double = 0.0,
  val totalClientsCount: Int = 0,
  val isLoading: Boolean = false,
  val userMessage: String? = null
)

class ClientListViewModel(
  private val repository: SafeBoxRepository
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _sortOrder = MutableStateFlow(SortOrder.RECENT)
  val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

  private val _userMessage = MutableStateFlow<String?>(null)
  val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

  val uiState: StateFlow<ClientListUiState> = combine(
    repository.getAllClients(),
    _searchQuery,
    _sortOrder,
    _userMessage
  ) { allClients, query, sort, message ->
    val filtered = if (query.isBlank()) {
      allClients
    } else {
      val q = query.trim().lowercase()
      allClients.filter { client ->
        client.name.lowercase().contains(q) ||
          client.phone.contains(q) ||
          client.boxNumber.lowercase().contains(q)
      }
    }

    val sorted = when (sort) {
      SortOrder.RECENT -> filtered.sortedByDescending { it.updatedAt }
      SortOrder.NAME_ASC -> filtered.sortedBy { it.name }
      SortOrder.NAME_DESC -> filtered.sortedByDescending { it.name }
      SortOrder.BALANCE_DESC -> filtered.sortedByDescending { it.balance }
      SortOrder.BALANCE_ASC -> filtered.sortedBy { it.balance }
    }

    val totalBalance = allClients.sumOf { it.balance }

    ClientListUiState(
      clients = sorted,
      searchQuery = query,
      sortOrder = sort,
      totalVaultBalance = totalBalance,
      totalClientsCount = allClients.size,
      isLoading = false,
      userMessage = message
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = ClientListUiState(isLoading = true)
  )

  fun onSearchQueryChanged(query: String) {
    _searchQuery.value = query
  }

  fun onSortOrderChanged(order: SortOrder) {
    _sortOrder.value = order
  }

  fun clearUserMessage() {
    _userMessage.value = null
  }

  fun addClient(
    name: String,
    phone: String,
    boxNumber: String,
    initialBalance: Double,
    notes: String,
    onSuccess: (Long) -> Unit
  ) {
    if (name.isBlank()) {
      _userMessage.value = "يرجى إدخال اسم العميل"
      return
    }

    viewModelScope.launch {
      try {
        val client = Client(
          name = name.trim(),
          phone = phone.trim(),
          boxNumber = boxNumber.trim(),
          balance = initialBalance.coerceAtLeast(0.0),
          notes = notes.trim()
        )
        val newId = repository.insertClient(client)
        _userMessage.value = "تمت إضافة الصندوق / العميل بنجاح"
        onSuccess(newId)
      } catch (e: Exception) {
        _userMessage.value = "حدث خطأ أثناء الإضافة: ${e.localizedMessage}"
      }
    }
  }

  fun updateClient(
    client: Client,
    onSuccess: () -> Unit
  ) {
    if (client.name.isBlank()) {
      _userMessage.value = "يرجى إدخال اسم العميل"
      return
    }

    viewModelScope.launch {
      try {
        val updated = repository.updateClient(client)
        if (updated) {
          _userMessage.value = "تم تحديث بيانات العميل بنجاح"
          onSuccess()
        } else {
          _userMessage.value = "تعذر تحديث بيانات العميل"
        }
      } catch (e: Exception) {
        _userMessage.value = "خطأ في التحديث: ${e.localizedMessage}"
      }
    }
  }

  fun deleteClient(clientId: Long) {
    viewModelScope.launch {
      try {
        val deleted = repository.deleteClient(clientId)
        if (deleted) {
          _userMessage.value = "تم حذف الصندوق وسجل الحركات بنجاح"
        }
      } catch (e: Exception) {
        _userMessage.value = "فشل الحذف: ${e.localizedMessage}"
      }
    }
  }
}

class ClientListViewModelFactory(
  private val repository: SafeBoxRepository
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return ClientListViewModel(repository) as T
  }
}

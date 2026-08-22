package com.example.ui.clients

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.notification.NotificationHelper
import com.example.domain.model.Client
import com.example.domain.model.SortOrder
import com.example.ui.theme.DepositGreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.VaultNavy
import com.example.util.ContactPickerHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
  viewModel: ClientListViewModel,
  onClientClick: (Long) -> Unit,
  onSettingsClick: () -> Unit,
  onLockClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  var showAddDialog by remember { mutableStateOf(false) }
  var clientToEdit by remember { mutableStateOf<Client?>(null) }
  var clientToDelete by remember { mutableStateOf<Client?>(null) }
  var showSortMenu by remember { mutableStateOf(false) }

  var contactPickerCallback by remember { mutableStateOf<((String?, String?) -> Unit)?>(null) }
  val contactPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickContact()
  ) { uri: Uri? ->
    uri?.let {
      val contactData = ContactPickerHelper.extractContactData(context, it)
      contactPickerCallback?.invoke(contactData.name, contactData.phoneNumber)
    }
  }

  val launchContactPicker: ((name: String?, phone: String?) -> Unit) -> Unit = { callback ->
    contactPickerCallback = callback
    try {
      contactPickerLauncher.launch(null)
    } catch (e: Exception) {
      Toast.makeText(context, "تعذر فتح دليل الهاتف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
  }

  LaunchedEffect(uiState.userMessage) {
    uiState.userMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearUserMessage()
    }
  }

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Scaffold(
      modifier = modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(EmeraldPrimary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AccountBalance,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "صناديق الأمانات",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "إدارة الحسابات والخزائن المالية",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                )
              }
            }
          },
          actions = {
            IconButton(
              onClick = onSettingsClick,
              modifier = Modifier.testTag("nav_settings_button")
            ) {
              Icon(Icons.Default.Settings, contentDescription = "الإعدادات والنسخ الاحتياطي")
            }
            IconButton(
              onClick = onLockClick,
              modifier = Modifier.testTag("nav_lock_button")
            ) {
              Icon(Icons.Default.Lock, contentDescription = "قفل التطبيق")
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
          )
        )
      },
      floatingActionButton = {
        ExtendedFloatingActionButton(
          onClick = { showAddDialog = true },
          icon = { Icon(Icons.Default.Add, contentDescription = null) },
          text = { Text("صندوق جديد", fontWeight = FontWeight.Bold) },
          containerColor = EmeraldPrimary,
          contentColor = Color.White,
          modifier = Modifier.testTag("add_client_fab")
        )
      }
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .background(MaterialTheme.colorScheme.background)
      ) {
        // Vault Overview Card
        VaultSummaryCard(
          totalBalance = uiState.totalVaultBalance,
          totalClients = uiState.totalClientsCount
        )

        // Search & Filter Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("بحث بالاسم، الهاتف، أو رقم الصندوق...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
              if (uiState.searchQuery.isNotEmpty()) {
                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                  Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("client_search_input")
          )

          Spacer(modifier = Modifier.width(8.dp))

          Box {
            OutlinedButton(
              onClick = { showSortMenu = true },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .height(56.dp)
                .testTag("sort_menu_button")
            ) {
              Icon(Icons.Default.Sort, contentDescription = "فرز")
            }

            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { showSortMenu = false }
            ) {
              SortOrder.values().forEach { order ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = order.titleArabic,
                      fontWeight = if (uiState.sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                      color = if (uiState.sortOrder == order) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                    )
                  },
                  onClick = {
                    viewModel.onSortOrderChanged(order)
                    showSortMenu = false
                  }
                )
              }
            }
          }
        }

        // Clients List
        if (uiState.clients.isEmpty()) {
          EmptyClientsView(
            isSearching = uiState.searchQuery.isNotEmpty(),
            onAddClick = { showAddDialog = true }
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(uiState.clients, key = { it.id }) { client ->
              ClientItemCard(
                client = client,
                onClick = { onClientClick(client.id) },
                onEdit = { clientToEdit = client },
                onDelete = { clientToDelete = client },
                onWhatsAppClick = {
                  if (client.phone.isNotBlank()) {
                    NotificationHelper.sendWhatsAppMessage(
                      context,
                      client.phone,
                      "مرحباً بك سيد/ة ${client.name} من صندوق الأمانات"
                    )
                  }
                }
              )
            }
          }
        }
      }

      // Add Client Dialog
      if (showAddDialog) {
        AddEditClientDialog(
          client = null,
          onDismiss = { showAddDialog = false },
          onPickContact = launchContactPicker,
          onSave = { name, phone, boxNumber, balance, notes ->
            viewModel.addClient(name, phone, boxNumber, balance, notes) {
              showAddDialog = false
            }
          }
        )
      }

      // Edit Client Dialog
      clientToEdit?.let { client ->
        AddEditClientDialog(
          client = client,
          onDismiss = { clientToEdit = null },
          onPickContact = launchContactPicker,
          onSave = { name, phone, boxNumber, _, notes ->
            viewModel.updateClient(
              client.copy(
                name = name,
                phone = phone,
                boxNumber = boxNumber,
                notes = notes
              )
            ) {
              clientToEdit = null
            }
          }
        )
      }

      // Delete Confirmation Dialog
      clientToDelete?.let { client ->
        AlertDialog(
          onDismissRequest = { clientToDelete = null },
          title = { Text("تأكيد حذف الصندوق") },
          text = {
            Text("هل أنت متأكد من رغبتك في حذف صندوق العميل (${client.name}) ورقم الصندوق (${client.boxNumber})؟ سيتم حذف جميع الحركات المالية المرتبطة به نهائياً.")
          },
          confirmButton = {
            Button(
              onClick = {
                viewModel.deleteClient(client.id)
                clientToDelete = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.testTag("confirm_delete_client_button")
            ) {
              Text("حذف نهائي")
            }
          },
          dismissButton = {
            TextButton(onClick = { clientToDelete = null }) {
              Text("إلغاء")
            }
          }
        )
      }
    }
  }
}

@Composable
fun VaultSummaryCard(
  totalBalance: Double,
  totalClients: Int
) {
  val context = LocalContext.current

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(
              EmeraldPrimary,
              VaultNavy
            )
          )
        )
        .padding(20.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "إجمالي أرصدة الخزينة",
              style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = NotificationHelper.formatAmountWithCurrency(totalBalance, context),
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }

          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AccountBalance,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(28.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(DepositGreen)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "إجمالي الصناديق النشطة: ${NotificationHelper.formatDigits(totalClients.toString(), context)}",
              style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
              )
            )
          }

          Text(
            text = "محمي ومشفر",
            style = MaterialTheme.typography.bodySmall.copy(
              color = GoldTertiary,
              fontWeight = FontWeight.Bold
            )
          )
        }
      }
    }
  }
}

@Composable
fun ClientItemCard(
  client: Client,
  onClick: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onWhatsAppClick: () -> Unit
) {
  val context = LocalContext.current

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("client_card_${client.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Avatar / Box indicator
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(EmeraldPrimaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = EmeraldPrimary,
          modifier = Modifier.size(26.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = client.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (client.boxNumber.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.secondaryContainer
            ) {
              Text(
                text = "صندوق #${NotificationHelper.formatDigits(client.boxNumber, context)}",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (client.phone.isNotBlank()) {
          Text(
            text = NotificationHelper.formatDigits(client.phone, context),
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "الرصيد: ${NotificationHelper.formatAmountWithCurrency(client.balance, context)}",
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            color = if (client.balance > 0) DepositGreen else MaterialTheme.colorScheme.onSurface
          )
        )
      }

      // Actions
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (client.phone.isNotBlank()) {
          IconButton(onClick = onWhatsAppClick) {
            Icon(
              imageVector = Icons.Default.Phone,
              contentDescription = "اتصال أو واتساب",
              tint = EmeraldPrimary
            )
          }
        }
        IconButton(onClick = onEdit) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "تعديل",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onDelete) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "حذف",
            tint = MaterialTheme.colorScheme.error
          )
        }
      }
    }
  }
}

@Composable
fun EmptyClientsView(
  isSearching: Boolean,
  onAddClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = if (isSearching) Icons.Default.Search else Icons.Default.Inbox,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(40.dp)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = if (isSearching) "لم يتم العثور على نتائج للبحث" else "لا توجد صناديق أمانات مسجلة بعد",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = if (isSearching)
        "جرّب البحث باسم آخر أو برقم هاتف مختلف"
      else
        "ابدأ بإنشاء أول صندوق أمانة لعميل وتوثيق الإيداعات والحركات المالية",
      style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
      ),
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )

    if (!isSearching) {
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onAddClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
      ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("إضافة صندوق جديد")
      }
    }
  }
}

@Composable
fun AddEditClientDialog(
  client: Client?,
  onDismiss: () -> Unit,
  onPickContact: ((name: String?, phone: String?) -> Unit) -> Unit,
  onSave: (name: String, phone: String, boxNumber: String, initialBalance: Double, notes: String) -> Unit
) {
  val context = LocalContext.current
  var name by remember { mutableStateOf(client?.name ?: "") }
  var phone by remember { mutableStateOf(client?.phone ?: "") }
  var boxNumber by remember { mutableStateOf(client?.boxNumber ?: "") }
  var initialBalanceStr by remember { mutableStateOf(if (client == null) "" else client.balance.toString()) }
  var notes by remember { mutableStateOf(client?.notes ?: "") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val handlePickContact = {
    onPickContact { pickedName, pickedPhone ->
      if (!pickedPhone.isNullOrBlank()) {
        phone = pickedPhone
      }
      if (name.isBlank() && !pickedName.isNullOrBlank()) {
        name = pickedName
        errorMessage = null
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (client == null) "إضافة صندوق أمانة جديد" else "تعديل بيانات العميل والصندوق",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            errorMessage = null
          },
          label = { Text("اسم العميل *") },
          placeholder = { Text("مثال: عبدالله محمد") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("client_name_input")
        )

        Column(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("رقم الهاتف (لواتساب و SMS)") },
            placeholder = { Text("مثال: 0912345678") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
              IconButton(
                onClick = handlePickContact,
                modifier = Modifier.testTag("pick_contact_icon_button")
              ) {
                Icon(
                  imageVector = Icons.Default.ContactPhone,
                  contentDescription = "اختيار من دليل الهاتف",
                  tint = EmeraldPrimary
                )
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("client_phone_input")
          )

          Spacer(modifier = Modifier.height(4.dp))

          Surface(
            onClick = handlePickContact,
            shape = RoundedCornerShape(8.dp),
            color = EmeraldPrimaryContainer.copy(alpha = 0.5f),
            modifier = Modifier
              .align(Alignment.Start)
              .testTag("pick_contact_action_chip")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Contacts,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "اختيار من دليل الهاتف",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = EmeraldPrimary
                )
              )
            }
          }
        }

        OutlinedTextField(
          value = boxNumber,
          onValueChange = { boxNumber = it },
          label = { Text("رقم الصندوق / الخزينة") },
          placeholder = { Text("مثال: A-105") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("client_box_number_input")
        )

        if (client == null) {
          OutlinedTextField(
            value = initialBalanceStr,
            onValueChange = { initialBalanceStr = it },
            label = { Text("الرصيد الافتتاحي (اختياري)") },
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("client_initial_balance_input")
          )
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("ملاحظات إضافية") },
          placeholder = { Text("أي تفاصيل خاصة بالأمانة...") },
          maxLines = 3,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("client_notes_input")
        )

        AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
          Text(
            text = errorMessage ?: "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank()) {
            errorMessage = "يرجى إدخال اسم العميل"
            return@Button
          }
          val initBalance = initialBalanceStr.toDoubleOrNull() ?: 0.0
          if (initBalance < 0) {
            errorMessage = "الرصيد الافتتاحي لا يمكن أن يكون سالباً"
            return@Button
          }
          onSave(name, phone, boxNumber, initBalance, notes)
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("save_client_button")
      ) {
        Text("حفظ")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء")
      }
    }
  )
}

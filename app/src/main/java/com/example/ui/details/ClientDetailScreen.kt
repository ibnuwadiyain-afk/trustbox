package com.example.ui.details

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.notification.NotificationHelper
import com.example.domain.model.Client
import com.example.domain.model.TransactionRecord
import com.example.domain.model.TransactionType
import com.example.ui.theme.DepositGreen
import com.example.ui.theme.DepositGreenLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.VaultNavy
import com.example.ui.theme.WithdrawalRed
import com.example.ui.theme.WithdrawalRedLight
import com.example.util.ContactPickerHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
  viewModel: ClientDetailViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  var showDepositDialog by remember { mutableStateOf(false) }
  var showWithdrawDialog by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }
  var activeNotificationPrompt by remember { mutableStateOf<NotificationPromptData?>(null) }

  LaunchedEffect(Unit) {
    viewModel.notificationPrompt.collect { promptData ->
      activeNotificationPrompt = promptData
    }
  }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { err ->
      snackbarHostState.showSnackbar(err)
      viewModel.clearMessages()
    }
  }

  LaunchedEffect(uiState.successMessage) {
    uiState.successMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessages()
    }
  }

  val smsPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    activeNotificationPrompt?.let { prompt ->
      if (isGranted) {
        val res = NotificationHelper.sendDirectSms(context, prompt.phone, prompt.preformattedMessage)
        res.fold(
          onSuccess = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
          onFailure = { err -> scope.launch { snackbarHostState.showSnackbar(err.localizedMessage ?: "فشل الإرسال") } }
        )
      } else {
        NotificationHelper.openSmsApp(context, prompt.phone, prompt.preformattedMessage)
      }
    }
  }

  val createPdfLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/pdf")
  ) { uri ->
    uri?.let {
      viewModel.exportStatementPdf(it, context)
    }
  }

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

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Scaffold(
      modifier = modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = uiState.client?.name ?: "تفاصيل الصندوق",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          },
          navigationIcon = {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("detail_back_button")
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
          },
          actions = {
            IconButton(
              onClick = {
                try {
                  viewModel.exportStatementPdfDirectShare(context)
                } catch (e: Exception) {
                  scope.launch { snackbarHostState.showSnackbar("حدث خطأ: ${e.localizedMessage}") }
                }
              },
              modifier = Modifier.testTag("export_client_pdf_button")
            ) {
              if (uiState.isExportingPdf) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = EmeraldPrimary,
                  strokeWidth = 2.dp
                )
              } else {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "مشاركة / فتح كشف حساب PDF")
              }
            }
            IconButton(
              onClick = { showEditDialog = true },
              modifier = Modifier.testTag("detail_edit_button")
            ) {
              Icon(Icons.Default.Edit, contentDescription = "تعديل البيانات")
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
          )
        )
      }
    ) { innerPadding ->
      if (uiState.client == null && !uiState.isLoading) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentAlignment = Alignment.Center
        ) {
          Text("لم يتم العثور على بيانات العميل")
        }
      } else {
        val client = uiState.client
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
        ) {
          // Client Detail Card
          client?.let {
            ClientHeaderCard(
              client = it,
              onWhatsAppClick = {
                if (it.phone.isNotBlank()) {
                  NotificationHelper.sendWhatsAppMessage(
                    context,
                    it.phone,
                    "مرحباً بك سيد/ة ${it.name}"
                  )
                }
              }
            )
          }

          // Deposit and Withdraw Action Buttons
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Button(
              onClick = { showDepositDialog = true },
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("deposit_button"),
              colors = ButtonDefaults.buttonColors(containerColor = DepositGreen),
              shape = RoundedCornerShape(14.dp)
            ) {
              Icon(Icons.Default.ArrowUpward, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("إيداع مبلغ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Button(
              onClick = { showWithdrawDialog = true },
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("withdraw_button"),
              colors = ButtonDefaults.buttonColors(containerColor = WithdrawalRed),
              shape = RoundedCornerShape(14.dp)
            ) {
              Icon(Icons.Default.ArrowDownward, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("سحب مبلغ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
          }

          // Transactions Timeline Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = EmeraldPrimary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "سجل العمليات المالية (${NotificationHelper.formatDigits(uiState.transactions.size.toString(), context)})",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground
            )
          }

          if (uiState.transactions.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "لا توجد حركات مالية مسجلة لهذا الصندوق بعد.",
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(uiState.transactions, key = { it.id }) { tx ->
                TransactionItemCard(tx = tx)
              }
            }
          }
        }
      }

      // Deposit Dialog
      if (showDepositDialog && uiState.client != null) {
        TransactionActionDialog(
          title = "إيداع مبلغ في الصندوق",
          actionText = "تأكيد الإيداع",
          actionColor = DepositGreen,
          currentBalance = uiState.client!!.balance,
          isWithdrawal = false,
          onDismiss = { showDepositDialog = false },
          onConfirm = { amount, note ->
            viewModel.deposit(amount, note)
            showDepositDialog = false
          }
        )
      }

      // Withdrawal Dialog
      if (showWithdrawDialog && uiState.client != null) {
        TransactionActionDialog(
          title = "سحب مبلغ من الصندوق",
          actionText = "تأكيد السحب",
          actionColor = WithdrawalRed,
          currentBalance = uiState.client!!.balance,
          isWithdrawal = true,
          onDismiss = { showWithdrawDialog = false },
          onConfirm = { amount, note ->
            viewModel.withdraw(amount, note)
            showWithdrawDialog = false
          }
        )
      }

      // Edit Client Dialog
      if (showEditDialog && uiState.client != null) {
        val client = uiState.client!!
        var name by remember { mutableStateOf(client.name) }
        var phone by remember { mutableStateOf(client.phone) }
        var boxNumber by remember { mutableStateOf(client.boxNumber) }
        var notes by remember { mutableStateOf(client.notes) }

        val handlePickContact = {
          launchContactPicker { pickedName, pickedPhone ->
            if (!pickedPhone.isNullOrBlank()) {
              phone = pickedPhone
            }
            if (name.isBlank() && !pickedName.isNullOrBlank()) {
              name = pickedName
            }
          }
        }

        AlertDialog(
          onDismissRequest = { showEditDialog = false },
          title = { Text("تعديل بيانات العميل") },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم العميل *") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
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
                      onClick = handlePickContact
                    ) {
                      Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = "اختيار من دليل الهاتف",
                        tint = EmeraldPrimary
                      )
                    }
                  },
                  modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                  onClick = handlePickContact,
                  shape = RoundedCornerShape(8.dp),
                  color = EmeraldPrimaryContainer.copy(alpha = 0.5f),
                  modifier = Modifier.align(Alignment.Start)
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
                label = { Text("رقم الصندوق") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              )

              OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات") },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              )
            }
          },
          confirmButton = {
            Button(
              onClick = {
                viewModel.updateClientInfo(name, phone, boxNumber, notes)
                showEditDialog = false
              },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
              Text("حفظ التعديلات")
            }
          },
          dismissButton = {
            TextButton(onClick = { showEditDialog = false }) {
              Text("إلغاء")
            }
          }
        )
      }

      // Post-Withdrawal Notification Bottom Sheet
      activeNotificationPrompt?.let { prompt ->
        PostTransactionNotificationBottomSheet(
          promptData = prompt,
          onDismiss = { activeNotificationPrompt = null },
          onSendWhatsApp = {
            NotificationHelper.sendWhatsAppMessage(context, prompt.phone, prompt.preformattedMessage)
            activeNotificationPrompt = null
          },
          onSendSms = {
            if (NotificationHelper.hasSmsPermission(context)) {
              val res = NotificationHelper.sendDirectSms(context, prompt.phone, prompt.preformattedMessage)
              res.fold(
                onSuccess = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                onFailure = { err -> scope.launch { snackbarHostState.showSnackbar(err.localizedMessage ?: "فشل الإرسال") } }
              )
            } else {
              smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
            activeNotificationPrompt = null
          },
          onOpenSmsApp = {
            NotificationHelper.openSmsApp(context, prompt.phone, prompt.preformattedMessage)
            activeNotificationPrompt = null
          },
          onCopyText = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("إشعار سحب الصندوق", prompt.preformattedMessage)
            clipboard.setPrimaryClip(clip)
            scope.launch {
              snackbarHostState.showSnackbar("تم نسخ نص الإشعار إلى الحافظة بنجاح")
            }
          }
        )
      }
    }
  }
}

@Composable
fun ClientHeaderCard(
  client: Client,
  onWhatsAppClick: () -> Unit
) {
  val context = LocalContext.current

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
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
          verticalAlignment = Alignment.Top
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = client.name,
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (client.boxNumber.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.2f)
              ) {
                Text(
                  text = "رقم الصندوق: #${NotificationHelper.formatDigits(client.boxNumber, context)}",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                  ),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            if (client.phone.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = NotificationHelper.formatDigits(client.phone, context),
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = Color.White.copy(alpha = 0.85f)
                )
              )
            }
          }

          if (client.phone.isNotBlank()) {
            IconButton(
              onClick = onWhatsAppClick,
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
            ) {
              Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "اتصال أو واتساب",
                tint = Color.White
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          Column {
            Text(
              text = "الرصيد المتاح حالياً",
              style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.8f)
              )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = NotificationHelper.formatAmountWithCurrency(client.balance, context),
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }

          if (client.notes.isNotBlank()) {
            Text(
              text = "ملاحظة: ${client.notes}",
              style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp
              ),
              maxLines = 2,
              modifier = Modifier.width(140.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun TransactionItemCard(tx: TransactionRecord) {
  val context = LocalContext.current
  val isDeposit = tx.type == TransactionType.DEPOSIT

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("tx_card_${tx.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Direction Icon
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(if (isDeposit) DepositGreenLight else WithdrawalRedLight),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isDeposit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
          contentDescription = if (isDeposit) "إيداع" else "سحب",
          tint = if (isDeposit) DepositGreen else WithdrawalRed,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isDeposit) "عملية إيداع" else "عملية سحب",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Text(
            text = "${if (isDeposit) "+" else "-"} ${NotificationHelper.formatAmountWithCurrency(tx.amount, context)}",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = if (isDeposit) DepositGreen else WithdrawalRed
            )
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "الرصيد بعد الحركة: ${NotificationHelper.formatAmountWithCurrency(tx.newBalance, context)}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Medium
            )
          )

          Text(
            text = NotificationHelper.formatDateTime(tx.timestamp, context),
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }

        if (tx.note.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "البيان: ${tx.note}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
              fontSize = 11.sp
            )
          )
        }
      }
    }
  }
}

@Composable
fun TransactionActionDialog(
  title: String,
  actionText: String,
  actionColor: Color,
  currentBalance: Double,
  isWithdrawal: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (amount: Double, note: String) -> Unit
) {
  val context = LocalContext.current
  var amountStr by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val amount = amountStr.toDoubleOrNull() ?: 0.0
  val calculatedBalance = if (isWithdrawal) currentBalance - amount else currentBalance + amount

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "الرصيد الحالي: ${NotificationHelper.formatAmountWithCurrency(currentBalance, context)}",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )

        OutlinedTextField(
          value = amountStr,
          onValueChange = {
            amountStr = it
            errorMessage = null
          },
          label = { Text("المبلغ المطلوب") },
          placeholder = { Text("0.00") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_amount_input")
        )

        // Live preview of balance
        if (amount > 0) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isWithdrawal && amount > currentBalance)
              MaterialTheme.colorScheme.errorContainer
            else
              MaterialTheme.colorScheme.surfaceVariant
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "الرصيد بعد الحركة:",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
              )
              Text(
                text = NotificationHelper.formatAmountWithCurrency(calculatedBalance, context),
                style = MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (isWithdrawal && amount > currentBalance)
                    MaterialTheme.colorScheme.error
                  else
                    MaterialTheme.colorScheme.onSurface
                )
              )
            }
          }
        }

        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("البيان / الملاحظة") },
          placeholder = { Text("مثال: سحب نقدي مباشر / إيداع شهري") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_note_input")
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
          if (amount <= 0) {
            errorMessage = "يرجى إدخال مبلغ صحيح أكبر من الصفر"
            return@Button
          }
          if (isWithdrawal && amount > currentBalance) {
            errorMessage = "الرصيد غير كافٍ لإتمام السحب"
            return@Button
          }
          onConfirm(amount, note)
        },
        colors = ButtonDefaults.buttonColors(containerColor = actionColor),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("confirm_transaction_button")
      ) {
        Text(actionText)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostTransactionNotificationBottomSheet(
  promptData: NotificationPromptData,
  onDismiss: () -> Unit,
  onSendWhatsApp: () -> Unit,
  onSendSms: () -> Unit,
  onOpenSmsApp: () -> Unit,
  onCopyText: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(EmeraldPrimaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = null,
              tint = EmeraldPrimary,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "إشعار العميل بالحركة المالية",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "يمكنك إرسال إشعار فوري للعميل عبر واتساب أو SMS",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preformatted Message Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
          )
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "نص الرسالة الجاهزة:",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = promptData.preformattedMessage,
              style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Options
        Button(
          onClick = onSendWhatsApp,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("notify_whatsapp_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = DepositGreen)
        ) {
          Icon(Icons.Default.Phone, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("إرسال عبر تطبيق واتساب", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = onSendSms,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("notify_sms_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
          Icon(Icons.Default.Message, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("إرسال رسالة نصية قصيرة (SMS)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onOpenSmsApp,
            modifier = Modifier
              .weight(1f)
              .height(48.dp),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("فتح تطبيق الرسائل", fontSize = 13.sp)
          }

          OutlinedButton(
            onClick = onCopyText,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("notify_copy_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("نسخ النص", fontSize = 13.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("تخطي الإشعار")
        }
      }
    }
  }
}

package com.example.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.notification.NotificationHelper
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.WithdrawalRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  var showChangePasswordDialog by remember { mutableStateOf(false) }

  // SAF Folder Launcher for Backup
  val backupFolderLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri: Uri? ->
    uri?.let {
      viewModel.createBackupToFolder(it, context)
    }
  }

  // SAF File Launcher for Restore
  val restoreFileLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let {
      viewModel.inspectBackupFile(it, context)
    }
  }

  LaunchedEffect(uiState.exportSuccessMessage) {
    uiState.exportSuccessMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessages()
    }
  }

  LaunchedEffect(uiState.restoreSuccessMessage) {
    uiState.restoreSuccessMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessages()
    }
  }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { err ->
      snackbarHostState.showSnackbar(err)
      viewModel.clearMessages()
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
              text = "الإعدادات والحماية",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          },
          navigationIcon = {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("settings_back_button")
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
          )
        )
      }
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .background(MaterialTheme.colorScheme.background)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Section 1: Security & Authentication
        SettingsSectionHeader(title = "الأمان والمصادقة", icon = Icons.Default.Security)

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            // Biometric Switch
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier.weight(1f),
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
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                  Text(
                    text = "تسجيل الدخول بالبصمة",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                  )
                  Text(
                    text = if (uiState.canUseBiometric)
                      "تفعيل المصادقة البيومترية لتسريع فتح الخزينة"
                    else
                      "مستشعر البصمة غير متوفر أو غير مهيأ بالجهاز",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontSize = 12.sp
                    )
                  )
                }
              }

              Switch(
                checked = uiState.isBiometricEnabled && uiState.canUseBiometric,
                onCheckedChange = { enabled ->
                  viewModel.setBiometricEnabled(enabled)
                },
                enabled = uiState.canUseBiometric,
                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary),
                modifier = Modifier.testTag("biometric_switch")
              )
            }

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Change Password Button
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                  Text(
                    text = "تغيير كلمة المرور",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                  )
                  Text(
                    text = "تحديث كلمة المرور الرئيسية المشفرة",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontSize = 12.sp
                    )
                  )
                }
              }

              OutlinedButton(
                onClick = { showChangePasswordDialog = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("change_password_button")
              ) {
                Text("تغيير")
              }
            }
          }
        }

        // Section 2: Backup & Restore (Google Drive / Storage SAF)
        SettingsSectionHeader(title = "النسخ الاحتياطي والاستعادة", icon = Icons.Default.Storage)

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            // Explanation
            Text(
              text = "حفظ البيانات في Google Drive أو الذاكرة المحلية",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "التطبيق يعمل بالكامل بدون إنترنت. عند الضغط على النسخ الاحتياطي، يمكنك اختيار مجلد 'Google Drive' المتزامن مع هاتفك ليتم رفع الملف تلقائياً، أو حفظه في مجلد التنزيلات.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
              )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Export Button
            Button(
              onClick = {
                // Open SAF Directory picker
                backupFolderLauncher.launch(null)
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("create_backup_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
              enabled = !uiState.isExporting
            ) {
              if (uiState.isExporting) {
                CircularProgressIndicator(
                  color = Color.White,
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("جارٍ إنشاء النسخة...")
              } else {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إنشاء وتصدير نسخة احتياطية (JSON)", fontWeight = FontWeight.Bold)
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Restore Button
            OutlinedButton(
              onClick = {
                // Open SAF File picker
                restoreFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("restore_backup_button"),
              shape = RoundedCornerShape(12.dp),
              enabled = !uiState.isImporting
            ) {
              if (uiState.isImporting) {
                CircularProgressIndicator(
                  color = EmeraldPrimary,
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("جارٍ فحص الملف...")
              } else {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("استعادة البيانات من ملف نسخة سابقة", fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Section 3: App Info & Security Standards
        SettingsSectionHeader(title = "معلومات النظام والأمان", icon = Icons.Default.Info)

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
          )
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(label = "اسم التطبيق", value = "صناديق الأمانات (SafeBox Vault)")
            InfoRow(label = "الإصدار", value = "1.0.0 (Offline-First Secure)")
            InfoRow(label = "قاعدة البيانات", value = "Room SQLite (معاملات ذرية مشفرة)")
            InfoRow(label = "التشفير", value = "SHA-256 + Salt + Android Keystore")
            InfoRow(label = "التكامل", value = "Storage Access Framework (SAF) & Drive")
          }
        }
      }

      // Change Password Dialog
      if (showChangePasswordDialog) {
        ChangePasswordDialog(
          onDismiss = { showChangePasswordDialog = false },
          onConfirm = { oldPass, newPass, confirmPass ->
            val ok = viewModel.changePassword(oldPass, newPass, confirmPass)
            if (ok) {
              showChangePasswordDialog = false
            }
          }
        )
      }

      // Restore Confirmation Dialog
      if (uiState.showRestoreConfirmDialog && uiState.restorePreview != null) {
        val payload = uiState.restorePreview!!
        AlertDialog(
          onDismissRequest = { viewModel.dismissRestoreConfirm() },
          icon = {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = WithdrawalRed,
              modifier = Modifier.size(36.dp)
            )
          },
          title = {
            Text(
              text = "تحذير أمني: استبدال البيانات الحالية",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "أنت على وشك استعادة نسخة احتياطية بتاريخ: ${NotificationHelper.formatDateTime(payload.exportedAt)}",
                style = MaterialTheme.typography.bodyMedium
              )
              Text(
                text = "تحتوي هذه النسخة على:",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "• ${payload.clientCount} حساب وصندوق أمانة\n• ${payload.transactionCount} حركة مالية مسجلة",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "تنبيه: سيتم مسح واستبدال كافة السجلات الموجودة حالياً على الجهاز بالبيانات المستوردة من هذا الملف كمعاملة واحدة ذرية.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp)
              )
            }
          },
          confirmButton = {
            Button(
              onClick = { viewModel.executeRestore() },
              colors = ButtonDefaults.buttonColors(containerColor = WithdrawalRed),
              modifier = Modifier.testTag("confirm_restore_button")
            ) {
              Text("تأكيد واستبدال البيانات")
            }
          },
          dismissButton = {
            TextButton(onClick = { viewModel.dismissRestoreConfirm() }) {
              Text("إلغاء")
            }
          }
        )
      }
    }
  }
}

@Composable
fun SettingsSectionHeader(
  title: String,
  icon: ImageVector
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = EmeraldPrimary,
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = EmeraldPrimary
    )
  }
}

@Composable
fun InfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
      )
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    )
  }
}

@Composable
fun ChangePasswordDialog(
  onDismiss: () -> Unit,
  onConfirm: (oldPass: String, newPass: String, confirmPass: String) -> Unit
) {
  var oldPass by remember { mutableStateOf("") }
  var newPass by remember { mutableStateOf("") }
  var confirmPass by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("تغيير كلمة المرور الرئيسية") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = oldPass,
          onValueChange = {
            oldPass = it
            errorMessage = null
          },
          label = { Text("كلمة المرور الحالية") },
          singleLine = true,
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = newPass,
          onValueChange = {
            newPass = it
            errorMessage = null
          },
          label = { Text("كلمة المرور الجديدة") },
          singleLine = true,
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = confirmPass,
          onValueChange = {
            confirmPass = it
            errorMessage = null
          },
          label = { Text("تأكيد كلمة المرور الجديدة") },
          singleLine = true,
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth()
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
          if (oldPass.isBlank() || newPass.isBlank()) {
            errorMessage = "يرجى تعبئة كافة الحقول"
            return@Button
          }
          if (newPass.length < 4) {
            errorMessage = "كلمة المرور يجب أن لا تقل عن 4 خانات"
            return@Button
          }
          if (newPass != confirmPass) {
            errorMessage = "كلمتا المرور الجديدتان غير متطابقتين"
            return@Button
          }
          onConfirm(oldPass, newPass, confirmPass)
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
      ) {
        Text("حفظ التغيير")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء")
      }
    }
  )
}

package com.example.ui.auth

import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.VaultNavy

@Composable
fun AuthScreen(
  viewModel: AuthViewModel,
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(uiState.isUnlocked) {
    if (uiState.isUnlocked) {
      onAuthSuccess()
    }
  }

  // Trigger biometric prompt automatically if enabled and not first launch
  LaunchedEffect(uiState.isBiometricEnabled, uiState.isSetupRequired) {
    if (!uiState.isSetupRequired && uiState.isBiometricEnabled && uiState.canUseBiometric && !uiState.isUnlocked) {
      val activity = context as? FragmentActivity
      if (activity != null) {
        showBiometricPrompt(
          activity = activity,
          onSuccess = { viewModel.onBiometricAuthSuccess() },
          onError = { err -> viewModel.onBiometricAuthFailed(err) }
        )
      }
    }
  }

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Surface(
      modifier = modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
              )
            )
          )
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          // Vault Shield Emblem
          Box(
            modifier = Modifier
              .size(96.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  colors = listOf(
                    EmeraldPrimary,
                    VaultNavy
                  )
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (uiState.isSetupRequired) Icons.Default.Security else Icons.Default.Lock,
              contentDescription = "أمان الصندوق",
              tint = Color.White,
              modifier = Modifier.size(48.dp)
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = if (uiState.isSetupRequired) "تعيين رمز الحماية" else "صناديق الأمانات والحسابات",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            ),
            textAlign = TextAlign.Center
          )

          Text(
            text = if (uiState.isSetupRequired)
              "مرحباً بك! يرجى تعيين كلمة مرور رئيسية لحماية بيانات الصناديق والعمليات المالية"
            else
              "يرجى إدخال كلمة المرور أو استخدام البصمة لفتح الخزينة",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
          )

          Spacer(modifier = Modifier.height(24.dp))

          if (uiState.isSetupRequired) {
            SetupPasswordCard(
              errorMessage = uiState.errorMessage,
              onSavePassword = { pass, confirm ->
                viewModel.setupPassword(pass, confirm)
              }
            )
          } else {
            LoginPasswordCard(
              errorMessage = uiState.errorMessage,
              canUseBiometric = uiState.canUseBiometric && uiState.isBiometricEnabled,
              onLogin = { pass ->
                viewModel.loginWithPassword(pass)
              },
              onBiometricClick = {
                val activity = context as? FragmentActivity
                if (activity != null) {
                  showBiometricPrompt(
                    activity = activity,
                    onSuccess = { viewModel.onBiometricAuthSuccess() },
                    onError = { err -> viewModel.onBiometricAuthFailed(err) }
                  )
                }
              }
            )
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Offline Security Notice
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = GoldTertiary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "بياناتك المالية مشفرة ومحفوظة محلياً 100% على هذا الجهاز دون اتصال بالإنترنت مع دعم النسخ الاحتياطي المشفر.",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 12.sp,
                  lineHeight = 18.sp
                )
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun SetupPasswordCard(
  errorMessage: String?,
  onSavePassword: (String, String) -> Unit
) {
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "إعداد الحساب للمرة الأولى",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("كلمة المرور الرئيسية") },
        placeholder = { Text("أدخل 4 أرقام أو أحرف على الأقل") },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = "إظهار كلمة المرور"
            )
          }
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("setup_password_input")
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it },
        label = { Text("تأكيد كلمة المرور") },
        placeholder = { Text("أعد كتابة كلمة المرور") },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
          onSavePassword(password, confirmPassword)
        }),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("setup_confirm_password_input")
      )

      AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
        Text(
          text = errorMessage ?: "",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(top = 8.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = { onSavePassword(password, confirmPassword) },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("setup_save_password_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
      ) {
        Icon(Icons.Default.LockOpen, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "حفظ ومتابعة إلى الخزينة",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}

@Composable
fun LoginPasswordCard(
  errorMessage: String?,
  canUseBiometric: Boolean,
  onLogin: (String) -> Unit,
  onBiometricClick: () -> Unit
) {
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "تسجيل الدخول إلى الخزينة",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("كلمة المرور") },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onLogin(password) }),
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = "إظهار كلمة المرور"
            )
          }
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_password_input")
      )

      AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
        Text(
          text = errorMessage ?: "",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(top = 8.dp)
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      Button(
        onClick = { onLogin(password) },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("login_submit_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
      ) {
        Icon(Icons.Default.LockOpen, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "فتح الخزينة",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }

      if (canUseBiometric) {
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
          onClick = onBiometricClick,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("login_biometric_button"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            tint = EmeraldPrimary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "الدخول بواسطة البصمة",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
          )
        }
      }
    }
  }
}

private fun showBiometricPrompt(
  activity: FragmentActivity,
  onSuccess: () -> Unit,
  onError: (String) -> Unit
) {
  val executor = ContextCompat.getMainExecutor(activity)
  val promptInfo = BiometricPrompt.PromptInfo.Builder()
    .setTitle("المصادقة البيومترية")
    .setSubtitle("قم بتأكيد هويتك لفتح صندوق الأمانات")
    .setNegativeButtonText("إلغاء واستخدام كلمة المرور")
    .build()

  val biometricPrompt = BiometricPrompt(
    activity,
    executor,
    object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        onSuccess()
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
          onError(errString.toString())
        }
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        onError("لم يتم التعرف على البصمة، حاول مجدداً")
      }
    }
  )

  try {
    biometricPrompt.authenticate(promptInfo)
  } catch (e: Exception) {
    onError(e.localizedMessage ?: "تعذر تشغيل المصادقة البيومترية")
  }
}

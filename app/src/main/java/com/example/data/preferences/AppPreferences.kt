package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ThemeMode(val id: String, val titleArabic: String) {
  SYSTEM("system", "تلقائي (حسب إعدادات النظام)"),
  LIGHT("light", "الوضع الفاتح (Light Mode)"),
  DARK("dark", "الوضع الداكن (Dark Mode)")
}

enum class DigitType(val id: String, val titleArabic: String, val example: String) {
  WESTERN("western", "أرقام عربية غربية / لاتينية", "1234.50"),
  EASTERN("eastern", "أرقام عربية مشرقية (هندية)", "١٢٣٤.٥٠")
}

data class CurrencyItem(
  val code: String,
  val symbol: String,
  val nameArabic: String
)

val PREDEFINED_CURRENCIES = listOf(
  CurrencyItem("LYD", "د.ل", "دينار ليبي (الافتراضي)"),
  CurrencyItem("SAR", "ر.س", "ريال سعودي"),
  CurrencyItem("USD", "$", "دولار أمريكي"),
  CurrencyItem("EUR", "€", "يورو"),
  CurrencyItem("EGP", "ج.م", "جنيه مصري"),
  CurrencyItem("AED", "د.إ", "درهم إماراتي"),
  CurrencyItem("KWD", "د.ك", "دينار كويتي"),
  CurrencyItem("QAR", "ر.ق", "ريال قطري"),
  CurrencyItem("BHD", "د.ب", "دينار بحريني"),
  CurrencyItem("OMR", "ر.ع", "ريال عماني"),
  CurrencyItem("JOD", "د.أ", "دينار أردني"),
  CurrencyItem("IQD", "د.ع", "دينار عراقي"),
  CurrencyItem("TND", "د.ت", "دينار تونسي"),
  CurrencyItem("DZD", "د.ج", "دينار جزائري"),
  CurrencyItem("MAD", "د.م", "درهم مغربي"),
  CurrencyItem("TRY", "₺", "ليرة تركية"),
  CurrencyItem("GBP", "£", "جنيه إسترليني")
)

data class AppPreferencesState(
  val currencySymbol: String = "د.ل",
  val currencyCode: String = "LYD",
  val currencyName: String = "دينار ليبي",
  val digitType: DigitType = DigitType.WESTERN,
  val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class AppPreferences(private val context: Context) {

  private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _state = MutableStateFlow(loadState())
  val state: StateFlow<AppPreferencesState> = _state.asStateFlow()

  companion object {
    private const val PREFS_NAME = "safebox_app_preferences"
    private const val KEY_CURRENCY_SYMBOL = "pref_currency_symbol"
    private const val KEY_CURRENCY_CODE = "pref_currency_code"
    private const val KEY_CURRENCY_NAME = "pref_currency_name"
    private const val KEY_DIGIT_TYPE = "pref_digit_type"
    private const val KEY_THEME_MODE = "pref_theme_mode"

    @Volatile
    private var INSTANCE: AppPreferences? = null

    fun getInstance(context: Context): AppPreferences {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: AppPreferences(context.applicationContext).also { INSTANCE = it }
      }
    }
  }

  init {
    INSTANCE = this
  }

  private fun loadState(): AppPreferencesState {
    val symbol = prefs.getString(KEY_CURRENCY_SYMBOL, "د.ل") ?: "د.ل"
    val code = prefs.getString(KEY_CURRENCY_CODE, "LYD") ?: "LYD"
    val name = prefs.getString(KEY_CURRENCY_NAME, "دينار ليبي") ?: "دينار ليبي"
    val digitTypeId = prefs.getString(KEY_DIGIT_TYPE, DigitType.WESTERN.id) ?: DigitType.WESTERN.id
    val digitType = if (digitTypeId == DigitType.EASTERN.id) DigitType.EASTERN else DigitType.WESTERN
    val themeModeId = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.id) ?: ThemeMode.SYSTEM.id
    val themeMode = ThemeMode.values().find { it.id == themeModeId } ?: ThemeMode.SYSTEM

    return AppPreferencesState(
      currencySymbol = symbol,
      currencyCode = code,
      currencyName = name,
      digitType = digitType,
      themeMode = themeMode
    )
  }

  fun setThemeMode(mode: ThemeMode) {
    prefs.edit()
      .putString(KEY_THEME_MODE, mode.id)
      .apply()

    _state.value = _state.value.copy(themeMode = mode)
  }

  fun setCurrency(symbol: String, code: String, name: String) {
    prefs.edit()
      .putString(KEY_CURRENCY_SYMBOL, symbol)
      .putString(KEY_CURRENCY_CODE, code)
      .putString(KEY_CURRENCY_NAME, name)
      .apply()

    _state.value = _state.value.copy(
      currencySymbol = symbol,
      currencyCode = code,
      currencyName = name
    )
  }

  fun setDigitType(type: DigitType) {
    prefs.edit()
      .putString(KEY_DIGIT_TYPE, type.id)
      .apply()

    _state.value = _state.value.copy(digitType = type)
  }

  fun formatDigits(input: String): String {
    val isEastern = _state.value.digitType == DigitType.EASTERN
    if (!isEastern) return input

    val easternDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val builder = StringBuilder(input.length)
    for (ch in input) {
      if (ch in '0'..'9') {
        builder.append(easternDigits[ch - '0'])
      } else {
        builder.append(ch)
      }
    }
    return builder.toString()
  }

  fun formatAmount(amount: Double): String {
    val decimalFormatter = DecimalFormat("#,##0.00")
    val formatted = decimalFormatter.format(amount)
    return formatDigits(formatted)
  }

  fun formatAmountWithCurrency(amount: Double): String {
    val amountFormatted = formatAmount(amount)
    val symbol = _state.value.currencySymbol
    return "$amountFormatted $symbol"
  }

  fun formatDateTime(timestamp: Long): String {
    val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    val formatted = dateFormatter.format(Date(timestamp))
    return formatDigits(formatted)
  }
}

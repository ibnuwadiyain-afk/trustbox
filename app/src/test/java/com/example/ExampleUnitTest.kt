package com.example

import com.example.util.ContactPickerHelper
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPhoneNumberNormalization() {
    // Normal international
    assertEquals("+966501234567", ContactPickerHelper.cleanAndNormalizePhoneNumber("+966 50 123 4567"))
    // Normal local with formatting
    assertEquals("0501234567", ContactPickerHelper.cleanAndNormalizePhoneNumber("(050) 123-4567"))
    // Arabic-Indic digits
    assertEquals("+966501234567", ContactPickerHelper.cleanAndNormalizePhoneNumber("+٩٦٦ ٥٠ ١٢٣ ٤٥٦٧"))
    // Persian digits
    assertEquals("0912345678", ContactPickerHelper.cleanAndNormalizePhoneNumber("۰۹۱۲۳۴۵۶۷۸"))
  }
}

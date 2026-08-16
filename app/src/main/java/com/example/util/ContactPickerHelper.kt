package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

object ContactPickerHelper {

  data class ContactData(
    val name: String? = null,
    val phoneNumber: String? = null
  )

  /**
   * Extracts name and phone number from a picked contact URI
   */
  fun extractContactData(context: Context, contactUri: Uri): ContactData {
    var name: String? = null
    var rawPhone: String? = null

    try {
      // 1. Try querying as Phone URI (ContactsContract.CommonDataKinds.Phone)
      context.contentResolver.query(
        contactUri,
        arrayOf(
          ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
          ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null,
        null,
        null
      )?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
          val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
          if (nameIdx != -1) name = cursor.getString(nameIdx)
          if (phoneIdx != -1) rawPhone = cursor.getString(phoneIdx)
        }
      }

      // 2. Fallback query if rawPhone is still null (e.g. if URI is ContactsContract.Contacts)
      if (rawPhone.isNullOrBlank()) {
        val contactId = contactUri.lastPathSegment
        if (contactId != null) {
          context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
              ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
              ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ? OR ${ContactsContract.CommonDataKinds.Phone._ID} = ?",
            arrayOf(contactId, contactId),
            null
          )?.use { phoneCursor ->
            if (phoneCursor.moveToFirst()) {
              val nameIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
              val phoneIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
              if (name.isNullOrBlank() && nameIdx != -1) name = phoneCursor.getString(nameIdx)
              if (phoneIdx != -1) rawPhone = phoneCursor.getString(phoneIdx)
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e("ContactPickerHelper", "Error extracting contact data: ${e.localizedMessage}", e)
    }

    // Clean phone number format (keep leading +, remove dashes, spaces, brackets)
    val cleanedPhone = cleanPhoneNumber(rawPhone)

    return ContactData(
      name = name?.trim(),
      phoneNumber = cleanedPhone
    )
  }

  private fun cleanPhoneNumber(phone: String?): String? {
    if (phone.isNullOrBlank()) return null
    val trimmed = phone.trim()
    val hasPlus = trimmed.startsWith("+")
    val digitsOnly = trimmed.filter { it.isDigit() }
    return if (digitsOnly.isEmpty()) {
      trimmed
    } else if (hasPlus) {
      "+$digitsOnly"
    } else {
      digitsOnly
    }
  }
}

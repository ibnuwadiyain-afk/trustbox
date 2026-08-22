package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract

object ContactPickerHelper {

  data class ContactData(
    val name: String? = null,
    val phoneNumber: String? = null
  )

  /**
   * Dedicated contract for picking phone numbers from contacts.
   * Uses Intent.ACTION_PICK with CommonDataKinds.Phone.CONTENT_URI.
   * This presents only contacts with phone numbers, allows specific number selection,
   * and provides granted read access to the selected record on all Android versions.
   */
  class PickPhoneContactContract : ActivityResultContract<Void?, Uri?>() {
    override fun createIntent(context: Context, input: Void?): Intent {
      return Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
      return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
  }

  /**
   * Robustly extracts name and phone number from a picked contact URI.
   * Handles Phone CONTENT_URI, Contacts CONTENT_URI, and lookup URIs.
   */
  fun extractContactData(context: Context, contactUri: Uri): ContactData {
    var name: String? = null
    var rawPhone: String? = null

    try {
      // 1. Attempt to query Phone details directly from the returned URI
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

      // 2. Fallback: If rawPhone is still null, query through Contacts table
      if (rawPhone.isNullOrBlank()) {
        var contactId: String? = null

        context.contentResolver.query(
          contactUri,
          arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
          ),
          null,
          null,
          null
        )?.use { cursor ->
          if (cursor.moveToFirst()) {
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            if (idIdx != -1) contactId = cursor.getString(idIdx)
            if (name.isNullOrBlank() && nameIdx != -1) name = cursor.getString(nameIdx)
          }
        }

        if (contactId == null) {
          contactId = contactUri.lastPathSegment
        }

        if (!contactId.isNullOrBlank()) {
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

    val cleanedPhone = cleanAndNormalizePhoneNumber(rawPhone)

    return ContactData(
      name = name?.trim(),
      phoneNumber = cleanedPhone
    )
  }

  /**
   * Cleans and normalizes phone numbers:
   * - Converts Eastern Arabic (٠-٩) and Persian (۰-۹) numerals to ASCII digits.
   * - Preserves leading '+' for international numbers.
   * - Removes non-digit characters such as spaces, dashes, parentheses.
   */
  fun cleanAndNormalizePhoneNumber(phone: String?): String? {
    if (phone.isNullOrBlank()) return null
    val trimmed = phone.trim()
    val builder = StringBuilder()
    for (i in trimmed.indices) {
      val ch = trimmed[i]
      when {
        ch in '0'..'9' -> builder.append(ch)
        ch in '٠'..'٩' -> builder.append('0' + (ch - '٠'))
        ch in '۰'..'۹' -> builder.append('0' + (ch - '۰'))
        ch == '+' && builder.isEmpty() -> builder.append('+')
      }
    }
    val result = builder.toString()
    return if (result.isEmpty() || result == "+") trimmed else result
  }
}

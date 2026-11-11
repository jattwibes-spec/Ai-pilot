package com.example.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.ai.ToolExecutionResult
import java.net.URLEncoder

data class ContactEntry(
    val name: String,
    val number: String
)

class CommunicationController(private val context: Context) {

    fun draftSms(phoneNumber: String, message: String): ToolExecutionResult {
        return try {
            val uri = Uri.parse("smsto:${phoneNumber.trim()}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message.trim())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                isSuccess = true,
                message = "Opened SMS composer with draft to $phoneNumber.",
                data = mapOf("phone" to phoneNumber, "draft" to message)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Failed to open SMS composer: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    fun dialPhoneNumber(phoneNumber: String): ToolExecutionResult {
        return try {
            val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
            val uri = Uri.parse("tel:$cleanPhone")
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                isSuccess = true,
                message = "Opened Android dialer for $cleanPhone.",
                data = mapOf("phone" to cleanPhone)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Failed to launch phone dialer: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    fun searchContacts(query: String): ToolExecutionResult {
        val permission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            return ToolExecutionResult(
                isSuccess = false,
                message = "Contacts permission (READ_CONTACTS) is required to search device contacts.",
                error = "PermissionDenied"
            )
        }

        return try {
            val contacts = mutableListOf<ContactEntry>()
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")

            val cursor: Cursor? = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = it.getString(nameIdx) ?: "Unknown"
                    val number = it.getString(numberIdx) ?: ""
                    contacts.add(ContactEntry(name, number))
                }
            }

            if (contacts.isEmpty()) {
                ToolExecutionResult(
                    isSuccess = true,
                    message = "No contacts found matching '$query'.",
                    data = mapOf("count" to 0)
                )
            } else {
                val summary = contacts.take(5).joinToString("\n") { "• ${it.name}: ${it.number}" }
                ToolExecutionResult(
                    isSuccess = true,
                    message = "Found ${contacts.size} contact(s) matching '$query'.",
                    data = mapOf("contacts" to contacts.map { mapOf("name" to it.name, "number" to it.number) }),
                    rawOutput = summary
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Contact search failed: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    fun openWhatsAppChat(phoneNumber: String?, message: String?): ToolExecutionResult {
        return try {
            val phone = phoneNumber?.replace(Regex("[^0-9]"), "") ?: ""
            val encodedMessage = if (!message.isNullOrBlank()) URLEncoder.encode(message, "UTF-8") else ""
            val url = if (phone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$phone&text=$encodedMessage"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMessage"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                isSuccess = true,
                message = "Opened WhatsApp message composer.",
                data = mapOf("phone" to phone, "draft" to (message ?: ""))
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Failed to launch WhatsApp: ${e.message}. Verify that WhatsApp is installed.",
                error = e.localizedMessage
            )
        }
    }

    fun openTelegramChat(message: String?): ToolExecutionResult {
        return try {
            val encoded = if (!message.isNullOrBlank()) URLEncoder.encode(message, "UTF-8") else ""
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://msg?text=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                isSuccess = true,
                message = "Opened Telegram message composer.",
                data = mapOf("draft" to (message ?: ""))
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Failed to launch Telegram: ${e.message}. Verify that Telegram is installed.",
                error = e.localizedMessage
            )
        }
    }
}

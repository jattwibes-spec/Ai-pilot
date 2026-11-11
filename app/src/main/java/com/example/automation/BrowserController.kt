package com.example.automation

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.ai.ToolExecutionResult
import java.net.URLEncoder

class BrowserController(private val context: Context) {

    fun openUrl(url: String): ToolExecutionResult {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                isSuccess = true,
                message = "Opened web browser navigating to $cleanUrl.",
                data = mapOf("url" to cleanUrl)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Failed to open URL '$cleanUrl': ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    fun webSearch(query: String): ToolExecutionResult {
        val trimmed = query.trim()
        return try {
            // Try standard WEB_SEARCH intent first
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, trimmed)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback to Google Search URL
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }

            ToolExecutionResult(
                isSuccess = true,
                message = "Initiated web search for query: '$trimmed'.",
                data = mapOf("query" to trimmed)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Search failed: ${e.message}",
                error = e.localizedMessage
            )
        }
    }
}

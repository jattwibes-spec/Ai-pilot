package com.example.files

import android.content.Context
import android.os.Environment
import com.example.ai.ToolExecutionResult
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

data class DiscoveredFile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val extension: String,
    val lastModified: Long
)

class FileManager(private val context: Context) {

    fun getDirectory(category: String): File {
        return when (category.lowercase().trim()) {
            "downloads", "download" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            "documents", "document" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            "pictures", "images" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            "dcim" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            else -> context.getExternalFilesDir(null) ?: context.filesDir
        }
    }

    fun searchFiles(extension: String?, query: String?, directoryCategory: String = "downloads"): ToolExecutionResult {
        return try {
            val baseDir = getDirectory(directoryCategory)
            if (!baseDir.exists() || !baseDir.canRead()) {
                // Check internal files directory fallback
                val internalDir = context.getExternalFilesDir(null) ?: context.filesDir
                val files = scanDirectory(internalDir, extension, query)
                return ToolExecutionResult(
                    isSuccess = true,
                    message = "Scanned app storage: found ${files.size} file(s).",
                    data = mapOf("count" to files.size, "files" to files.map { it.name }),
                    rawOutput = files.take(20).joinToString("\n") { "${it.name} (${formatSize(it.sizeBytes)})" }
                )
            }

            val files = scanDirectory(baseDir, extension, query)
            ToolExecutionResult(
                isSuccess = true,
                message = "Found ${files.size} file(s) in ${baseDir.name} matching '${extension ?: query ?: "all"}'.",
                data = mapOf(
                    "count" to files.size,
                    "directory" to baseDir.absolutePath,
                    "files" to files.map { mapOf("name" to it.name, "path" to it.path, "size" to it.sizeBytes) }
                ),
                rawOutput = if (files.isEmpty()) "No files found." else files.take(20).joinToString("\n") { "${it.name} (${formatSize(it.sizeBytes)})" }
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "File search error: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    private fun scanDirectory(dir: File, extension: String?, query: String?): List<DiscoveredFile> {
        val results = mutableListOf<DiscoveredFile>()
        val extClean = extension?.trim()?.removePrefix(".")?.lowercase()

        dir.walkTopDown().maxDepth(3).forEach { file ->
            if (file.isFile) {
                val matchesExt = extClean.isNullOrBlank() || file.extension.equals(extClean, ignoreCase = true)
                val matchesQuery = query.isNullOrBlank() || file.name.contains(query, ignoreCase = true)
                if (matchesExt && matchesQuery) {
                    results.add(
                        DiscoveredFile(
                            name = file.name,
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            extension = file.extension,
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }
        return results
    }

    fun calculateStorageUsage(extension: String = "all", directoryCategory: String = "downloads"): ToolExecutionResult {
        return try {
            val baseDir = getDirectory(directoryCategory)
            val extClean = if (extension.equals("all", true)) null else extension.trim().removePrefix(".").lowercase()
            val files = scanDirectory(baseDir, extClean, null)

            val totalBytes = files.sumOf { it.sizeBytes }
            val formattedSize = formatSize(totalBytes)

            ToolExecutionResult(
                isSuccess = true,
                message = "Total storage occupied by ${if (extClean != null) ".$extClean" else "all"} files in ${baseDir.name}: $formattedSize (${files.size} files).",
                data = mapOf(
                    "totalBytes" to totalBytes,
                    "formattedSize" to formattedSize,
                    "fileCount" to files.size,
                    "directory" to baseDir.name
                ),
                rawOutput = "Directory: ${baseDir.absolutePath}\nFilter: ${extClean ?: "all"}\nFile Count: ${files.size}\nTotal Size: $formattedSize ($totalBytes bytes)"
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Storage calculation failed: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    fun createFolder(folderName: String, parentCategory: String = "downloads"): ToolExecutionResult {
        return try {
            val baseDir = getDirectory(parentCategory)
            val newDir = File(baseDir, folderName)
            if (newDir.exists()) {
                return ToolExecutionResult(
                    isSuccess = true,
                    message = "Folder '${newDir.name}' already exists at ${newDir.absolutePath}.",
                    data = mapOf("path" to newDir.absolutePath, "created" to false)
                )
            }
            val created = newDir.mkdirs()
            if (created || newDir.exists()) {
                ToolExecutionResult(
                    isSuccess = true,
                    message = "Successfully created folder '${newDir.name}' in ${baseDir.name}.",
                    data = mapOf("path" to newDir.absolutePath, "created" to true)
                )
            } else {
                ToolExecutionResult(
                    isSuccess = false,
                    message = "Could not create folder '${newDir.name}'. Storage permissions may be required.",
                    error = "FolderCreationFailed"
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Failed to create folder: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    fun findDuplicateFiles(directoryCategory: String = "downloads"): ToolExecutionResult {
        return try {
            val baseDir = getDirectory(directoryCategory)
            val allFiles = scanDirectory(baseDir, null, null)

            // Group by length first for efficiency
            val bySize = allFiles.groupBy { it.sizeBytes }.filter { it.value.size > 1 && it.key > 0 }
            val duplicates = mutableListOf<List<DiscoveredFile>>()

            for ((_, group) in bySize) {
                val byHash = group.groupBy { computeFileHash(File(it.path)) }
                for ((_, hashGroup) in byHash) {
                    if (hashGroup.size > 1) {
                        duplicates.add(hashGroup)
                    }
                }
            }

            val duplicateCount = duplicates.sumOf { it.size - 1 }
            val potentialSavingsBytes = duplicates.sumOf { (it.size - 1) * it[0].sizeBytes }

            val summary = if (duplicates.isEmpty()) {
                "No duplicate files detected in ${baseDir.name}."
            } else {
                val lines = duplicates.map { group ->
                    "${group[0].name} (x${group.size}):\n" + group.joinToString("\n") { "  - ${it.path}" }
                }
                "Found $duplicateCount duplicate file(s) across ${duplicates.size} sets. Potential reclaimable space: ${formatSize(potentialSavingsBytes)}.\n\n" + lines.joinToString("\n\n")
            }

            ToolExecutionResult(
                isSuccess = true,
                message = "Duplicate scan completed: $duplicateCount duplicate(s) found.",
                data = mapOf(
                    "duplicateSets" to duplicates.size,
                    "reclaimableBytes" to potentialSavingsBytes,
                    "formattedReclaimable" to formatSize(potentialSavingsBytes)
                ),
                rawOutput = summary
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Duplicate search failed: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    fun deleteFile(filePath: String): ToolExecutionResult {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return ToolExecutionResult(
                    isSuccess = false,
                    message = "File does not exist: $filePath",
                    error = "FileNotFound"
                )
            }
            val deleted = file.delete()
            if (deleted) {
                ToolExecutionResult(
                    isSuccess = true,
                    message = "Successfully deleted file: ${file.name}",
                    data = mapOf("path" to filePath, "deleted" to true)
                )
            } else {
                ToolExecutionResult(
                    isSuccess = false,
                    message = "Unable to delete file '${file.name}'. File may be read-only or in use.",
                    error = "DeleteFailed"
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Deletion error: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    private fun computeFileHash(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val fis = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L
            // Read up to 2MB to keep hash calculation fast on mobile
            while (fis.read(buffer).also { bytesRead = it } != -1 && totalRead < 2 * 1024 * 1024) {
                digest.update(buffer, 0, bytesRead)
                totalRead += bytesRead
            }
            fis.close()
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            file.length().toString()
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return "%.2f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}

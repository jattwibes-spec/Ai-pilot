package com.example.files

import android.content.Context
import com.example.ai.ToolExecutionResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileOrganizer(
    private val context: Context,
    private val fileManager: FileManager
) {

    fun organizeFiles(
        extension: String,
        targetFolderName: String,
        sourceCategory: String = "downloads"
    ): ToolExecutionResult {
        return try {
            val sourceDir = fileManager.getDirectory(sourceCategory)
            val extClean = extension.trim().removePrefix(".").lowercase()
            val targetFolder = File(sourceDir, targetFolderName)

            if (!targetFolder.exists()) {
                targetFolder.mkdirs()
            }

            // Find matching files in source directory (excluding targetFolder itself)
            val filesToMove = sourceDir.listFiles { file ->
                file.isFile &&
                    file.extension.equals(extClean, ignoreCase = true) &&
                    file.parentFile?.absolutePath != targetFolder.absolutePath
            }?.toList() ?: emptyList()

            if (filesToMove.isEmpty()) {
                return ToolExecutionResult(
                    isSuccess = true,
                    message = "No .$extClean files found in ${sourceDir.name} needing migration into '$targetFolderName'.",
                    data = mapOf("movedCount" to 0, "targetFolder" to targetFolder.name)
                )
            }

            var movedCount = 0
            val movedFileNames = mutableListOf<String>()

            for (sourceFile in filesToMove) {
                val destFile = File(targetFolder, sourceFile.name)
                val success = if (sourceFile.renameTo(destFile)) {
                    true
                } else {
                    // Fallback copy + delete
                    copyAndDelete(sourceFile, destFile)
                }

                if (success) {
                    movedCount++
                    movedFileNames.add(sourceFile.name)
                }
            }

            ToolExecutionResult(
                isSuccess = true,
                message = "Organized $movedCount .$extClean file(s) into '$targetFolderName' successfully.",
                data = mapOf(
                    "movedCount" to movedCount,
                    "targetFolder" to targetFolder.name,
                    "files" to movedFileNames
                ),
                rawOutput = "Destination: ${targetFolder.absolutePath}\nOrganized files:\n" + movedFileNames.joinToString("\n") { "• $it" }
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                message = "Organization failed: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    private fun copyAndDelete(source: File, dest: File): Boolean {
        return try {
            FileInputStream(source).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            source.delete()
            true
        } catch (_: Exception) {
            false
        }
    }
}

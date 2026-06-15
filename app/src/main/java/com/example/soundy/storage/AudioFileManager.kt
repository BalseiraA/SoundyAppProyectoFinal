package com.example.soundy.storage

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class AudioFileManager(private val context: Context) {

    companion object {
        const val MAX_DURATION_MS = 60_000L
    }

    fun getDisplayName(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) {
                return it.getString(nameIndex) ?: "sonido"
            }
        }
        return uri.lastPathSegment ?: "sonido"
    }

    fun getDurationMs(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }

    fun copySoundToInternalStorage(uri: Uri, buttonName: String): File {
        val soundsDir = File(context.filesDir, "sounds")
        if (!soundsDir.exists()) {
            soundsDir.mkdirs()
        }

        val originalName = getDisplayName(uri)
        val extension = originalName.substringAfterLast('.', "mp3")
            .lowercase(Locale.ROOT)
            .takeIf { it.length in 2..5 }
            ?: "mp3"

        val safeButtonName = buttonName
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9áéíóúñ]+"), "_")
            .trim('_')
            .ifBlank { "boton" }

        val destination = File(
            soundsDir,
            "${safeButtonName}_${System.currentTimeMillis()}.$extension"
        )

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("No se pudo leer el archivo seleccionado")

        return destination
    }

    fun deleteLocalSound(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists() && file.delete()
    }
}

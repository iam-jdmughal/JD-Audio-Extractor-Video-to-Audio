package com.jd.audioextractor.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MediaStoreSaver(private val context: Context) {

    fun createTempOutputFile(baseName: String, extension: String): File {
        val dir = File(context.cacheDir, "extracted_audio").apply {
            if (!exists()) mkdirs()
        }
        val cleanName = baseName.substringBeforeLast(".")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(64)
        return File(dir, "${cleanName}_${System.currentTimeMillis()}.$extension")
    }

    suspend fun saveToMusicLibrary(
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/JDAudioExtractor")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues) ?: return@withContext null

            try {
                resolver.openOutputStream(itemUri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
                itemUri
            } catch (e: Exception) {
                resolver.delete(itemUri, null, null)
                null
            }
        } else {
            // Legacy Android 8 - 9 (API 24-28)
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val targetFolder = File(musicDir, "JDAudioExtractor").apply { if (!exists()) mkdirs() }
            val destFile = File(targetFolder, displayName)

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { out ->
                    input.copyTo(out)
                }
            }

            var scannedUri: Uri? = null
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf(mimeType)
            ) { _, uri ->
                scannedUri = uri
            }
            scannedUri ?: Uri.fromFile(destFile)
        }
    }

    fun shareAudio(file: File, title: String) {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension.equals("webm", ignoreCase = true)) "audio/webm" else "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Extracted Audio").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

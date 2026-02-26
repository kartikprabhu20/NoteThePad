package com.mintanable.notethepad.core.file

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val folderName = "NoteAttachments"

    fun getMediaDir(): File {
        val dir = File(context.getExternalFilesDir(null), folderName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun saveMediaToStorage(uri: Uri, prefix: String?): String? {
        return withContext(Dispatchers.IO) {
            try {
                val extension = getExtensionFromUri(uri)
                val fileName = (if(prefix.isNullOrBlank()) "media" else "$prefix")+ "_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
                val mediaDir = getMediaDir()
                if (!mediaDir.exists()) mediaDir.mkdirs()
                val destFile = File(mediaDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.absolutePath
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getExtensionFromUri(uri: Uri): String {
        val extension = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val mimeType = context.contentResolver.getType(uri)
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        } else {
            MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        }
        return if (extension.isNullOrBlank()) "bin" else extension
    }

    suspend fun deleteFiles(list: List<String>) {
        withContext(Dispatchers.IO){
            for(path in list){
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                   Log.e("kptest", "Error while delete files: $e")
                }
            }
        }
    }

    fun createTempUri(extension: String): Uri? {
        return try {
            val file = File.createTempFile("TEMP_", ".$extension", getMediaDir())
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: IOException) {
            Log.e("kptest", "Error while createTempUri: $e")
            null
        }
    }

    fun createTempFile(extension: String): File? {
        return try {
            File.createTempFile("TEMP_", ".$extension", getMediaDir())
        } catch (e: IOException) {
            Log.e("kptest", "Error while createTempUri: $e")
            null
        }
    }
}
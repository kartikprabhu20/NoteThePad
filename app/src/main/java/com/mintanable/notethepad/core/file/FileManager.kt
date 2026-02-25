package com.mintanable.notethepad.core.file

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val folderName = "NoteAttachments"

    private fun getMediaDir(): File {
        val dir = File(context.getExternalFilesDir(null), folderName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun saveMediaToStorage(uri: Uri, extension: String = "jpg"): String? {
        return withContext(Dispatchers.IO) {
            try {

                val fileName = "media_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
                val destFile = File(getMediaDir(), fileName)

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

    suspend fun deleteFileFromUris(list: List<Uri>){
        deleteFilesFromPaths(list.map { it.toString() })
    }

    suspend fun deleteFilesFromPaths(list: List<String>) {
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
}
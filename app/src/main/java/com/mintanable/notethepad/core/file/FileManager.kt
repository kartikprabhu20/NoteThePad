package com.mintanable.notethepad.core.file

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

class FileManager @Inject constructor(
    @ApplicationContext applicationContext: Context
) {
    suspend fun copyFileToInternalStorage(uri: Uri, context: Context): String {
        return withContext(Dispatchers.IO) {
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            val destFile = File(context.filesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        }
    }

    suspend fun deleteFileFromUris(list: List<Uri>){
        deleteFilesFromPaths(list.map { it.toString() })
    }

    suspend fun deleteFilesFromPaths(list: List<String>) {
        withContext(Dispatchers.IO){
            for(path in list){
                try {
                    val file = File(path.toString())
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                   Log.e("kptest", "Error wile delete files: $e")
                }
            }
        }
    }


}
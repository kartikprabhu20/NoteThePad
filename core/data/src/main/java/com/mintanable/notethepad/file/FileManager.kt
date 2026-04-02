package com.mintanable.notethepad.file

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.mintanable.notethepad.database.db.entity.AttachmentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import androidx.core.net.toUri

class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val folderName = "NoteAttachments"
    val authority = "${context.packageName}.fileprovider"

    fun getMediaDir(): File {
        val dir = File(context.getExternalFilesDir(null), folderName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun saveMediaToStorage(uri: Uri, prefix: String?): String? {
        return withContext(Dispatchers.IO) {
            try {
                val internalFile = getFileFromInternalUri(uri)
                if (internalFile != null && internalFile.exists()) {
                    return@withContext internalFile.absolutePath
                }
                val extension = getExtensionFromUri(uri)
                val fileName = (if(prefix.isNullOrBlank()) "media" else "$prefix")+ "_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
                val mediaDir = getMediaDir()
                val destFile = File(mediaDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.absolutePath
            } catch (e: Exception) {
                Log.e("FileManager", "Error saving media", e)
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
                    val file = getFileFromUri(path)
                    if (file?.exists() == true) {
                        file.delete()
                    }
                } catch (e: Exception) {
                   Log.e("kptest", "Error while delete files: $e")
                }
            }
        }
    }

    fun createUri(extension: String,prefix: String?): Uri? {
        return try {
            val file = createFile(extension, prefix) ?: return null
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            Log.e("kptest", "Error while createUri: $e")
            null
        }
    }

    fun createFile(extension: String,prefix: String?): File? {
        return try {
            File.createTempFile(if(prefix.isNullOrBlank()) "media_" else "$prefix"+"_", ".$extension", getMediaDir())
        } catch (e: IOException) {
            Log.e("kptest", "Error while createFile: $e")
            null
        }
    }

    fun isInternalUri(uri: Uri): Boolean = uri.toString().contains(context.packageName)

    fun getAttachmentType(uri: Uri): AttachmentType {
        return AttachmentHelper.getAttachmentType(context, uri)
    }

    /**
     * Attempts to resolve any URI string (content://, file://, or raw path) to a File object.
     * Specifically handles FileProvider URIs and absolute paths that might belong to 
     * a different device/installation by falling back to the filename in the current media dir.
     */
    fun getFileFromUri(uriString: String): File? {
        if (uriString.isBlank()) return null
        return try {
            val uri = uriString.toUri()

            // Check if it's a raw path or file:// URI
            if (uri.scheme == null || uri.scheme == "file") {
                val rawPath = uri.path ?: uriString
                val decodedPath = Uri.decode(rawPath).removePrefix("file:")
                val file = File(decodedPath)
                
                if (file.exists()) return file
                
                // Fallback: If the absolute path is wrong (e.g. from a different device),
                // but it's inside a folder named "NoteAttachments", try to find it locally.
                if (decodedPath.contains(folderName)) {
                    val fileName = decodedPath.substringAfterLast('/')
                    val fallbackFile = File(getMediaDir(), fileName)
                    if (fallbackFile.exists()) return fallbackFile
                }
            }

            // Check if it's our own FileProvider URI (content://)
            if (uri.scheme == "content" && isInternalUri(uri)) {
                val fileName = uri.lastPathSegment ?: uriString.substringAfterLast('/')
                val file = File(getMediaDir(), fileName)
                if (file.exists()) return file
            }

            // Handle external content URIs by copying to a temp file in cache
            if (uri.scheme == "content") {
                val tempFile = File(context.cacheDir, "temp_${uri.lastPathSegment ?: UUID.randomUUID()}")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return tempFile
            }

            null
        } catch (e: Exception) {
            Log.e("FileManager", "Error resolving $uriString", e)
            null
        }
    }

    /**
     * Resolves a URI to a File ONLY if it points to our app's internal NoteAttachments folder.
     */
    private fun getFileFromInternalUri(uri: Uri): File? {
        if (!isInternalUri(uri)) return null
        
        return when (uri.scheme) {
            "file" -> File(uri.path ?: return null)
            "content" -> {
                val fileName = uri.lastPathSegment ?: return null
                File(getMediaDir(), fileName)
            }
            null -> File(uri.toString()) // Assume raw path
            else -> null
        }
    }

    fun uriToContent(uri: Uri): Uri? = try {
        if (uri.scheme == "content" && uri.authority == authority) uri
        else if (uri.scheme == "file" || (uri.scheme == null && uri.path != null)) {
            val file = java.io.File(uri.path ?: return null)
            androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        } else if (uri.scheme == "content") {
            uri
        } else null
    } catch (e: Exception) {
        Log.e("ShareNote", "Could not convert URI: $uri", e)
        null
    }
}

package com.mintanable.notethepad.core.common

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.mintanable.notethepad.core.model.note.AttachmentType

object AttachmentHelper {

    fun getAttachmentType(context: Context, uri: Uri): AttachmentType {
        val mimeType = getMimeType(context, uri)
        Log.d("AttachmentHelper", "mimeType: $mimeType")
        return when {
            mimeType?.startsWith("image") == true -> AttachmentType.IMAGE
            mimeType?.startsWith("video") == true -> AttachmentType.VIDEO
            else -> AttachmentType.AUDIO
        }
    }

    fun getFileExtension(context: Context, uri: Uri): String? {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val mime = MimeTypeMap.getSingleton()
            mime.getExtensionFromMimeType(context.contentResolver.getType(uri))
        } else {
            MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        }
    }

    fun getMimeType(path: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.getType(uri)
        } else {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        }
    }
}

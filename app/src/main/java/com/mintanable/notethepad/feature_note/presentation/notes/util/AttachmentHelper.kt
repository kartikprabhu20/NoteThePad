package com.mintanable.notethepad.feature_note.presentation.notes.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.mintanable.notethepad.feature_note.domain.util.AttachmentType

object AttachmentHelper {

    fun getAttachmentType(context: Context, uri: Uri): AttachmentType {
        val mimeType = getMimeType(context, uri)
        Log.d("kptest", "mimeType: $mimeType")
        return when {
            mimeType?.startsWith("image") == true -> AttachmentType.IMAGE
            mimeType?.startsWith("video") == true -> AttachmentType.VIDEO
            else -> AttachmentType.AUDIO
        }
    }

    fun getFileExtension(context: Context, uri: Uri): String? {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            // If it's a content URI, ask the system for the MIME type
            val mime = MimeTypeMap.getSingleton()
            mime.getExtensionFromMimeType(context.contentResolver.getType(uri))
        } else {
            // If it's a file URI, just pull it from the path string
            MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        }
    }

    fun getMimeType(path: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
        return mime
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            // 1. For Picker/Gallery URIs: Ask the ContentResolver
            context.contentResolver.getType(uri)
        } else {
            // 2. For Internal/File URIs: Check the extension
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        }
    }
}
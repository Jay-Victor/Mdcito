package com.mdcito.app.data.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class FileOperations @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    suspend fun readFileContent(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                val content = reader.readText()
                Timber.tag("FileOps").d("readFileContent: uri=%s, contentLength=%d", uri, content.length)
                content
            }
        } ?: run {
            Timber.tag("FileOps").w("readFileContent: inputStream is null for uri=%s", uri)
            ""
        }
    }

    suspend fun writeFileContent(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        Timber.tag("FileOps").i("writeFileContent: uri=%s, contentLength=%d", uri, content.length)
        try {
            context.contentResolver.openOutputStream(uri, "rwt")?.use { outputStream ->
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            Timber.tag("FileOps").e(e, "writeFileContent: failed for uri=%s", uri)
            false
        }
    }

    suspend fun createFileInDirectory(parentUri: Uri, fileName: String, mimeType: String = "text/markdown"): Uri? =
        withContext(Dispatchers.IO) {
            Timber.tag("FileOps").i("createFileInDirectory: parentUri=%s, fileName=%s", parentUri, fileName)
            try {
                DocumentFile.fromTreeUri(context, parentUri)?.createFile(mimeType, fileName)?.uri
            } catch (e: Exception) {
                Timber.tag("FileOps").e(e, "createFileInDirectory: failed for parentUri=%s, fileName=%s", parentUri, fileName)
                null
            }
        }

    suspend fun createFolderInDirectory(parentUri: Uri, folderName: String): Uri? =
        withContext(Dispatchers.IO) {
            Timber.tag("FileOps").i("createFolderInDirectory: parentUri=%s, folderName=%s", parentUri, folderName)
            try {
                DocumentFile.fromTreeUri(context, parentUri)?.createDirectory(folderName)?.uri
            } catch (e: Exception) {
                Timber.tag("FileOps").e(e, "createFolderInDirectory: failed for parentUri=%s, folderName=%s", parentUri, folderName)
                null
            }
        }

    suspend fun deleteDocument(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        Timber.tag("FileOps").w("deleteDocument: uri=%s", uri)
        try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: Exception) {
            Timber.tag("FileOps").e(e, "deleteDocument: failed for uri=%s", uri)
            false
        }
    }

    suspend fun renameDocument(uri: Uri, newName: String): Uri? = withContext(Dispatchers.IO) {
        Timber.tag("FileOps").i("renameDocument: uri=%s, newName=%s", uri, newName)
        try {
            DocumentsContract.renameDocument(context.contentResolver, uri, newName)
        } catch (e: Exception) {
            Timber.tag("FileOps").e(e, "renameDocument: failed for uri=%s, newName=%s", uri, newName)
            null
        }
    }

    suspend fun listFilesInDirectory(treeUri: Uri): List<FileInfo> = withContext(Dispatchers.IO) {
        try {
            val documentFile = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList<FileInfo>().also {
                Timber.tag("FileOps").w("listFilesInDirectory: DocumentFile is null for treeUri=%s", treeUri)
            }
            val result = documentFile.listFiles().mapNotNull { doc ->
                FileInfo(
                    name = doc.name ?: return@mapNotNull null,
                    uri = doc.uri,
                    isDirectory = doc.isDirectory,
                    size = doc.length(),
                    lastModified = doc.lastModified(),
                    mimeType = doc.type ?: "application/octet-stream",
                )
            }
            Timber.tag("FileOps").d("listFilesInDirectory: treeUri=%s, fileCount=%d", treeUri, result.size)
            result
        } catch (e: Exception) {
            Timber.tag("FileOps").e(e, "listFilesInDirectory: failed for treeUri=%s", treeUri)
            emptyList()
        }
    }

    fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            Timber.tag("FileOps").e(e, "getFileName: failed for uri=%s", uri)
            null
        }
    }

    fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
            } ?: 0L
        } catch (e: Exception) {
            Timber.tag("FileOps").e(e, "getFileSize: failed for uri=%s", uri)
            0L
        }
    }

    fun takePersistablePermission(uri: Uri) {
        Timber.tag("FileOps").d("takePersistablePermission: uri=%s", uri)
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            Timber.tag("FileOps").w(e, "takePersistablePermission: failed for uri=%s", uri)
        }
    }

    fun releasePersistablePermission(uri: Uri) {
        Timber.tag("FileOps").d("releasePersistablePermission: uri=%s", uri)
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            Timber.tag("FileOps").w(e, "releasePersistablePermission: failed for uri=%s", uri)
        }
    }

    fun hasPersistablePermission(uri: Uri): Boolean {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
    }

    suspend fun copyFileToWorkspace(sourceUri: Uri, targetDirUri: Uri, fileName: String): Uri? =
        withContext(Dispatchers.IO) {
            Timber.tag("FileOps").i("copyFileToWorkspace: source=%s, targetDir=%s, fileName=%s", sourceUri, targetDirUri, fileName)
            try {
                val content = readFileContent(sourceUri)
                createFileInDirectory(targetDirUri, fileName)?.let { newUri ->
                    if (writeFileContent(newUri, content)) newUri else null
                }
            } catch (e: Exception) {
                Timber.tag("FileOps").e(e, "copyFileToWorkspace: failed for source=%s, targetDir=%s", sourceUri, targetDirUri)
                null
            }
        }

    suspend fun exportFile(sourceUri: Uri, content: String, format: ExportFormat): ByteArray? =
        withContext(Dispatchers.IO) {
            Timber.tag("FileOps").i("exportFile: source=%s, format=%s", sourceUri, format)
            try {
                when (format) {
                    ExportFormat.MARKDOWN, ExportFormat.TXT -> content.toByteArray(Charsets.UTF_8)
                    ExportFormat.HTML -> {
                        val html = com.mdcito.app.markdown.MarkdownRenderer.renderToHtml(content)
                        com.mdcito.app.markdown.MarkdownRenderer.wrapHtml(html).toByteArray(Charsets.UTF_8)
                    }
                    ExportFormat.PDF -> {
                        val html = com.mdcito.app.markdown.MarkdownRenderer.renderToHtml(content)
                        com.mdcito.app.markdown.MarkdownRenderer.wrapHtml(html).toByteArray(Charsets.UTF_8)
                    }
                    ExportFormat.DOCX, ExportFormat.IMAGE_PNG, ExportFormat.ZIP ->
                        content.toByteArray(Charsets.UTF_8)
                }
            } catch (e: Exception) {
                Timber.tag("FileOps").e(e, "exportFile: failed for source=%s, format=%s", sourceUri, format)
                null
            }
        }
}

data class FileInfo(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
)

enum class ExportFormat(val extension: String, val mimeType: String) {
    MARKDOWN("md", "text/markdown"),
    TXT("txt", "text/plain"),
    HTML("html", "text/html"),
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    IMAGE_PNG("png", "image/png"),
    ZIP("zip", "application/zip"),
}

package com.gradtrack.app.ui.common

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.gradtrack.app.R
import com.gradtrack.app.data.repository.UploadFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_UPLOAD_BYTES = 25L * 1024 * 1024

/** Opens the system file picker and reads the chosen file, rejecting anything over 25 MB. */
@Composable
fun rememberFilePicker(onPicked: (UploadFile) -> Unit): () -> Unit {
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            var name = "file"
            var size = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) }
                    cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = cursor.getLong(it) }
                }
            }
            scope.launch {
                if (size > MAX_UPLOAD_BYTES) {
                    snackbar.showSnackbar(resources.getString(R.string.error_file_too_large))
                    return@launch
                }
                val bytes = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() }
                if (bytes == null) snackbar.showSnackbar(resources.getString(R.string.error_file_read)) else onPicked(UploadFile(name, context.contentResolver.getType(uri), bytes))
            }
        }
    return { launcher.launch(arrayOf("*/*")) }
}

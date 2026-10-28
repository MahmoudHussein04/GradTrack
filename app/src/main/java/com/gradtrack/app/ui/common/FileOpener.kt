package com.gradtrack.app.ui.common

import android.content.ActivityNotFoundException
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.FileProvider
import com.gradtrack.app.R
import com.gradtrack.app.data.Resource
import com.gradtrack.app.data.model.Submission
import kotlinx.coroutines.launch

@Composable
fun rememberSubmissionOpener(): (Submission) -> Unit {
    val context = LocalContext.current
    val resources = LocalResources.current
    val files = LocalContainer.current.files
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    return { submission ->
        scope.launch {
            when (val result = files.submissionFile(submission)) {
                is Resource.Failure -> snackbar.showSnackbar(result.error.message(context))
                is Resource.Success -> {
                    val file = result.data
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "application/octet-stream"
                    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    try {
                        context.startActivity(Intent.createChooser(intent, resources.getString(R.string.open_file)))
                    } catch (_: ActivityNotFoundException) {
                        snackbar.showSnackbar(resources.getString(R.string.error_no_viewer))
                    }
                }
            }
        }
    }
}

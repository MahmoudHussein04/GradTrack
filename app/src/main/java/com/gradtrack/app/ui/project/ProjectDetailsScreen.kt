package com.gradtrack.app.ui.project

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.gradtrack.app.data.Resource
import com.gradtrack.app.data.model.MilestoneStatus
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.Submission
import com.gradtrack.app.data.model.SubmissionStatus
import com.gradtrack.app.data.repository.UploadFile
import com.gradtrack.app.ui.common.LocalSnackbar
import com.gradtrack.app.ui.components.ConfirmDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.model.ProjectOverview
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.repository.ProjectRepository
import com.gradtrack.app.ui.Routes
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.LocalContainer
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.common.rememberSubmissionOpener
import com.gradtrack.app.ui.components.EmptyState
import com.gradtrack.app.ui.components.FeedbackRow
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.MilestoneTimeline
import com.gradtrack.app.ui.components.MutedText
import com.gradtrack.app.ui.components.PageHeader
import com.gradtrack.app.ui.components.Pill
import com.gradtrack.app.ui.components.PrimaryButton
import com.gradtrack.app.ui.components.ProgressBar
import com.gradtrack.app.ui.components.SecondaryButton
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.components.SubmissionRow
import com.gradtrack.app.ui.components.TextPromptDialog
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType

data class ProjectData(val overview: ProjectOverview, val submissions: List<Submission>)

class ProjectDetailsViewModel(private val repository: ProjectRepository) : LoadableViewModel<ProjectData>({
        when (val overview = repository.overview()) {
            is Resource.Failure -> overview
            is Resource.Success ->
                if (overview.data.project == null) {
                    Resource.Success(ProjectData(overview.data, emptyList()))
                } else {
                    when (val submissions = repository.submissions()) {
                        is Resource.Failure -> submissions
                        is Resource.Success -> Resource.Success(ProjectData(overview.data, submissions.data))
                    }
                }
        }
    }) {
    fun reply(text: String) =
        act({ repository.reply(text) }) { feedback ->
            patch { it.copy(overview = it.overview.copy(feedback = it.overview.feedback + feedback)) }
            send(UiEvent.Message(R.string.reply_sent))
        }

    fun upload(milestoneId: String, file: UploadFile) =
        act({ repository.upload(milestoneId, file) }) {
            send(UiEvent.Message(R.string.deliverable_uploaded))
            refresh()
        }
}

private class PickedFile(val uri: Uri, val name: String, val mime: String?)

private const val MAX_FILE_BYTES = 25L * 1024 * 1024

@Composable
fun ProjectDetailsScreen(onNavigate: (String) -> Unit) {
    val vm = appViewModel { ProjectDetailsViewModel(it.projects) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val user by LocalContainer.current.session.currentUser.collectAsStateWithLifecycle()
    val isLeader = user?.role == Role.LEADER
    var replying by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<PickedFile?>(null) }
    val openFile = rememberSubmissionOpener()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    EventsEffect(vm)

    val target = (state as? UiState.Success)?.data?.overview?.milestones?.firstOrNull { it.status == MilestoneStatus.IN_PROGRESS }

    val pickFile =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            var name = "deliverable"
            var size = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) }
                    cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = cursor.getLong(it) }
                }
            }
            if (size > MAX_FILE_BYTES) {
                scope.launch { snackbar.showSnackbar(resources.getString(R.string.error_file_too_large)) }
            } else {
                picked = PickedFile(uri, name, context.contentResolver.getType(uri))
            }
        }

    val file = picked
    if (file != null && target != null) {
        ConfirmDialog(
            title = stringResource(R.string.upload_deliverable),
            message = stringResource(R.string.upload_confirm, file.name, target.name),
            confirm = stringResource(R.string.upload),
            onDismiss = { picked = null },
            onConfirm = {
                picked = null
                scope.launch {
                    val bytes = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() } }.getOrNull() }
                    if (bytes == null) snackbar.showSnackbar(resources.getString(R.string.error_file_read)) else vm.upload(target.id, UploadFile(file.name, file.mime, bytes))
                }
            },
        )
    }
    LifecycleResumeEffect(vm) {
        if (vm.state.value is UiState.Success) vm.refresh()
        onPauseOrDispose {}
    }

    if (replying) {
        TextPromptDialog(
            title = stringResource(R.string.reply_to_feedback),
            label = stringResource(R.string.hint_reply),
            confirm = stringResource(R.string.send),
            multiline = true,
            onDismiss = { replying = false },
            onConfirm = {
                replying = false
                vm.reply(it)
            },
        )
    }

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { all ->
        val data = all.overview
        val project = data.project
        if (project == null) {
            item {
                GtCard {
                    EmptyState(
                        icon = R.drawable.ic_project,
                        title = stringResource(R.string.no_project_title),
                        body = stringResource(if (isLeader) R.string.no_project_leader else R.string.no_project_student),
                        primary = if (isLeader) stringResource(R.string.register_project) else null,
                        onPrimary = { onNavigate(Routes.REGISTER_PROJECT) },
                    )
                }
            }
            return@LoadableScreen
        }
        item {
            PageHeader(
                title = project.title,
                overline = stringResource(R.string.project_overview_overline),
                subtitle = stringResource(R.string.project_meta, project.category, project.academicYear.orEmpty()),
            )
            Pill(project.status.pill(), Modifier.padding(top = 12.dp))
        }
        item {
            GtCard {
                SectionTitle(stringResource(R.string.project_progress)) {
                    Text(stringResource(R.string.percent, project.progress), style = GtType.Headline.copy(color = Gt.Primary))
                }
                ProgressBar(project.progress, Modifier.padding(top = 10.dp))
                Text(stringResource(R.string.milestones_completed, project.completedMilestones, project.totalMilestones), style = GtType.Caption, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.title_team), { onNavigate(Routes.TEAM) }, Modifier.weight(1f), icon = R.drawable.ic_team)
                SecondaryButton(stringResource(R.string.title_proposal), { onNavigate(Routes.PROPOSAL_STATUS) }, Modifier.weight(1f), icon = R.drawable.ic_proposals)
            }
        }
        item {
            GtCard {
                SectionTitle(stringResource(R.string.milestones_deliverables))
                Text(stringResource(R.string.milestones_hint), style = GtType.Caption, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
                MilestoneTimeline(data.milestones)
            }
        }
        item {
            GtCard {
                SectionTitle(stringResource(R.string.supervisor_feedback))
                if (data.feedback.isEmpty()) MutedText(stringResource(R.string.no_feedback_yet), Modifier.padding(top = 8.dp))
                data.feedback.forEach { FeedbackRow(it) }
                SecondaryButton(stringResource(R.string.reply_to_feedback), { replying = true }, Modifier.padding(top = 8.dp), enabled = !busy)
            }
        }
        item {
            GtCard {
                SectionTitle(stringResource(R.string.submitted_deliverables))
                if (all.submissions.isEmpty()) MutedText(stringResource(R.string.no_submissions_yet), Modifier.padding(top = 8.dp))
                all.submissions.forEach { submission -> SubmissionRow(submission) { openFile(submission) } }
                val waiting = target != null && all.submissions.any { it.milestoneId == target.id && it.status == SubmissionStatus.REVIEW }
                if (isLeader) {
                    if (waiting) {
                        Text(stringResource(R.string.deliverable_in_review, target.name), style = GtType.Caption, modifier = Modifier.padding(top = 10.dp))
                    } else if (project.status == ProjectStatus.IN_PROGRESS && target != null) {
                        PrimaryButton(
                            stringResource(R.string.upload_deliverable_for, target.name),
                            { pickFile.launch(arrayOf("application/pdf", "application/zip", "image/*")) },
                            Modifier.fillMaxWidth().padding(top = 14.dp),
                            loading = busy,
                            icon = R.drawable.ic_upload,
                        )
                    } else if (project.status != ProjectStatus.IN_PROGRESS && project.status != ProjectStatus.COMPLETED) {
                        Text(stringResource(R.string.deliverables_after_approval), style = GtType.Caption, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
    }
}

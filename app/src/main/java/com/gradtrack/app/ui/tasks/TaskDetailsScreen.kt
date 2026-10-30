package com.gradtrack.app.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.Resource
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.TaskItem
import com.gradtrack.app.data.model.TaskStatus
import com.gradtrack.app.data.repository.TaskRepository
import com.gradtrack.app.data.repository.UploadFile
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.LocalContainer
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.common.rememberFileOpener
import com.gradtrack.app.ui.common.rememberFilePicker
import com.gradtrack.app.ui.components.ConfirmDialog
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.GtTextField
import com.gradtrack.app.ui.components.IconTile
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.MutedText
import com.gradtrack.app.ui.components.Pill
import com.gradtrack.app.ui.components.PrimaryButton
import com.gradtrack.app.ui.components.SecondaryButton
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType

/** Opening a to-do task as its assignee starts it, so the leader sees it move to In progress. */
class TaskDetailsViewModel(private val repository: TaskRepository, private val id: String, myId: String?) :
    LoadableViewModel<TaskItem>({
        when (val task = repository.task(id)) {
            is Resource.Success -> if (task.data.assigneeId == myId && task.data.status == TaskStatus.TODO) repository.start(id) else task
            is Resource.Failure -> task
        }
    }) {
    fun submit(note: String, file: UploadFile?) =
        act({ repository.submit(id, note, file) }) { updated ->
            patch { updated }
            send(UiEvent.Message(R.string.solution_submitted))
        }
}

@Composable
fun TaskDetailsScreen(taskId: String) {
    val container = LocalContainer.current
    val myId = container.session.user?.id
    val isLeader = container.session.user?.role == Role.LEADER
    val vm = appViewModel(key = "task-$taskId") { TaskDetailsViewModel(it.tasks, taskId, myId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val open = rememberFileOpener()
    var note by rememberSaveable { mutableStateOf("") }
    var file by remember { mutableStateOf<UploadFile?>(null) }
    var noteError by remember { mutableStateOf<String?>(null) }
    var confirming by remember { mutableStateOf(false) }
    val pickFile = rememberFilePicker { file = it }
    val emptySolution = stringResource(R.string.error_solution_empty)
    EventsEffect(vm)

    if (confirming) {
        ConfirmDialog(stringResource(R.string.submit_solution), stringResource(R.string.submit_solution_confirm), stringResource(R.string.submit), { confirming = false }, {
            confirming = false
            vm.submit(note, file)
        })
    }

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { task ->
        val isAssignee = task.assigneeId == myId
        item {
            GtCard {
                Pill(task.status.pill())
                Text(task.title, style = GtType.Headline, modifier = Modifier.padding(top = 10.dp))
                Text(
                    if (isLeader) stringResource(R.string.task_meta_due, task.assigneeName, task.dueDate?.asDate() ?: "—") else stringResource(R.string.task_due, task.dueDate?.asDate() ?: "—"),
                    style = GtType.Caption,
                    modifier = Modifier.padding(top = 4.dp),
                )
                task.description?.let { Text(it, style = GtType.Body, modifier = Modifier.padding(top = 12.dp)) }
            }
        }
        task.attachmentName?.let { name ->
            item {
                GtCard(padding = 16.dp) {
                    SectionTitle(stringResource(R.string.task_attachment))
                    FileRow(name, stringResource(R.string.tap_to_open)) { open { container.files.taskAttachment(task) } }
                }
            }
        }
        item {
            GtCard {
                SectionTitle(stringResource(R.string.solution), Modifier.padding(bottom = 8.dp))
                when {
                    task.status == TaskStatus.DONE -> {
                        val on = task.completedAt?.asDate() ?: "—"
                        MutedText(if (isAssignee) stringResource(R.string.solution_submitted_by_me, on) else stringResource(R.string.solution_submitted_on, task.assigneeName, on))
                        task.solutionNote?.let { Text(it, style = GtType.Body, modifier = Modifier.padding(top = 10.dp)) }
                        task.solutionFileName?.let { name ->
                            FileRow(name, stringResource(R.string.tap_to_open)) { open { container.files.taskSolution(task) } }
                        }
                    }
                    isAssignee -> {
                        GtTextField(note, {
                            note = it
                            noteError = null
                        }, stringResource(R.string.field_solution_note), error = noteError, singleLine = false, minLines = 3)
                        val picked = file
                        if (picked == null) {
                            SecondaryButton(stringResource(R.string.attach_file), pickFile, Modifier.fillMaxWidth().padding(top = 8.dp), icon = R.drawable.ic_upload)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { FileRow(picked.name, stringResource(R.string.attached)) {} }
                                IconButton(onClick = { file = null }) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.remove_file), tint = Gt.TextSecondary) }
                            }
                        }
                        PrimaryButton(stringResource(R.string.submit_solution), {
                            if (note.isBlank() && file == null) noteError = emptySolution else confirming = true
                        }, Modifier.fillMaxWidth().padding(top = 12.dp), loading = busy)
                    }
                    else -> MutedText(stringResource(R.string.waiting_for_solution, task.assigneeName))
                }
            }
        }
    }
}

@Composable
private fun FileRow(name: String, caption: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(R.drawable.ic_file)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(name, style = GtType.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, style = GtType.Caption)
        }
    }
}

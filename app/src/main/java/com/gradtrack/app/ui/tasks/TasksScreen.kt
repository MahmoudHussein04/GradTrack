package com.gradtrack.app.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.Resource
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.TaskItem
import com.gradtrack.app.data.model.TaskRequest
import com.gradtrack.app.data.model.TaskStatus
import com.gradtrack.app.data.model.TeamMember
import com.gradtrack.app.data.repository.TaskRepository
import com.gradtrack.app.data.repository.TeamRepository
import com.gradtrack.app.ui.Routes
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.LocalContainer
import com.gradtrack.app.ui.common.LocalTabBarSpace
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.asShortDate
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.components.ConfirmDialog
import com.gradtrack.app.ui.components.DropdownField
import com.gradtrack.app.ui.components.EmptyState
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.GtTextField
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.PageHeader
import com.gradtrack.app.ui.components.Pill
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.components.StatCard
import com.gradtrack.app.ui.components.StatRow
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TasksData(val tasks: List<TaskItem>, val members: List<TeamMember>)

class TasksViewModel(
    private val repository: TaskRepository,
    teams: TeamRepository,
    isLeader: Boolean,
) : LoadableViewModel<TasksData>({
        val tasks = repository.tasks()
        val members = if (isLeader) teams.myTeam() else null
        when {
            tasks is Resource.Failure -> tasks
            members is Resource.Failure -> members
            else -> Resource.Success(TasksData((tasks as Resource.Success).data, (members as? Resource.Success)?.data?.team?.members.orEmpty()))
        }
    }) {
    private val _filter = MutableStateFlow<TaskStatus?>(null)
    val filter: StateFlow<TaskStatus?> = _filter.asStateFlow()

    fun setFilter(status: TaskStatus?) {
        _filter.value = status
    }

    fun setStatus(task: TaskItem, status: TaskStatus) = act({ repository.setStatus(task.id, status) }) { updated -> replace(updated) }

    fun create(request: TaskRequest) =
        act({ repository.create(request) }) { created ->
            patch { it.copy(tasks = listOf(created) + it.tasks) }
            send(UiEvent.Message(R.string.task_created))
        }

    fun update(id: String, request: TaskRequest) =
        act({ repository.update(id, request) }) { updated ->
            replace(updated)
            send(UiEvent.Message(R.string.task_saved))
        }

    fun cancel(task: TaskItem) =
        act({ repository.cancel(task.id) }) {
            patch { data -> data.copy(tasks = data.tasks.filterNot { it.id == task.id }) }
            send(UiEvent.Message(R.string.task_cancelled_msg))
        }

    private fun replace(updated: TaskItem) = patch { data -> data.copy(tasks = data.tasks.map { if (it.id == updated.id) updated else it }) }
}

private sealed interface TaskDialog {
    data class Edit(val task: TaskItem?) : TaskDialog

    data class Cancel(val task: TaskItem) : TaskDialog
}

@Composable
fun TasksScreen(onNavigate: (String) -> Unit) {
    val user by LocalContainer.current.session.currentUser.collectAsStateWithLifecycle()
    val isLeader = user?.role == Role.LEADER
    val vm = appViewModel { TasksViewModel(it.tasks, it.team, it.session.user?.role == Role.LEADER) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<TaskDialog?>(null) }
    EventsEffect(vm)

    val members = (state as? UiState.Success)?.data?.members.orEmpty()
    when (val d = dialog) {
        is TaskDialog.Edit ->
            TaskEditDialog(d.task, members, onDismiss = { dialog = null }) { request ->
                dialog = null
                if (d.task == null) vm.create(request) else vm.update(d.task.id, request)
            }
        is TaskDialog.Cancel ->
            ConfirmDialog(stringResource(R.string.cancel_task), stringResource(R.string.cancel_task_confirm, d.task.title), stringResource(R.string.cancel_task), { dialog = null }, {
                dialog = null
                vm.cancel(d.task)
            }, danger = true)
        null -> Unit
    }

    Box(Modifier.fillMaxSize()) {
        LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { data ->
            val tasks = data.tasks
            val done = tasks.count { it.status == TaskStatus.DONE }
            item { PageHeader(stringResource(R.string.tasks_heading), stringResource(R.string.crumb_tasks), stringResource(R.string.tasks_subtitle)) }
            item {
                StatRow {
                    StatCard(stringResource(R.string.stat_total_tasks), tasks.size.toString(), Modifier.weight(1f), stringResource(R.string.stat_for_this_project), R.drawable.ic_task_list)
                    StatCard(stringResource(R.string.task_in_progress), tasks.count { it.status == TaskStatus.IN_PROGRESS }.toString(), Modifier.weight(1f), stringResource(R.string.stat_keep_moving), R.drawable.ic_clock)
                    StatCard(
                        stringResource(R.string.stat_completed),
                        done.toString(),
                        Modifier.weight(1f),
                        stringResource(R.string.stat_percent_complete, if (tasks.isEmpty()) 0 else done * 100 / tasks.size),
                        R.drawable.ic_check_circle,
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(null to R.string.filter_all, TaskStatus.TODO to R.string.task_todo, TaskStatus.IN_PROGRESS to R.string.task_in_progress, TaskStatus.DONE to R.string.task_done)
                    options.forEach { (status, label) ->
                        item {
                            FilterChip(
                                selected = filter == status,
                                onClick = { vm.setFilter(status) },
                                label = { Text(stringResource(label)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gt.PrimaryContainer, selectedLabelColor = Gt.PrimaryDark),
                            )
                        }
                    }
                }
            }
            item {
                GtCard(padding = 12.dp) {
                    SectionTitle(stringResource(R.string.team_task_board), Modifier.padding(start = 4.dp, bottom = 4.dp))
                    val shown = if (filter == null) tasks else tasks.filter { it.status == filter }
                    when {
                        isLeader && data.members.isEmpty() ->
                            EmptyState(R.drawable.ic_team, stringResource(R.string.empty_tasks_no_team_title), stringResource(R.string.empty_tasks_no_team_body), primary = stringResource(R.string.go_to_team), onPrimary = { onNavigate(Routes.TEAM) })
                        tasks.isEmpty() && isLeader ->
                            EmptyState(R.drawable.ic_check_circle, stringResource(R.string.empty_tasks_title), stringResource(R.string.empty_tasks_leader_body), primary = stringResource(R.string.add_task), onPrimary = { dialog = TaskDialog.Edit(null) })
                        tasks.isEmpty() -> EmptyState(R.drawable.ic_check_circle, stringResource(R.string.empty_tasks_title), stringResource(R.string.empty_tasks_member_body))
                        shown.isEmpty() -> EmptyState(R.drawable.ic_check_circle, stringResource(R.string.empty_filter_title), stringResource(R.string.empty_filter_body))
                        else ->
                            shown.forEach { task ->
                                TaskRow(
                                    task = task,
                                    canChange = isLeader || task.assigneeId == user?.id,
                                    isLeader = isLeader,
                                    onStatus = { if (task.status != it) vm.setStatus(task, it) },
                                    onEdit = { dialog = TaskDialog.Edit(task) },
                                    onCancel = { dialog = TaskDialog.Cancel(task) },
                                )
                            }
                    }
                }
            }
        }

        val data = (state as? UiState.Success)?.data
        if (isLeader && data != null && data.members.isNotEmpty() && data.tasks.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { dialog = TaskDialog.Edit(null) },
                icon = { Icon(painterResource(R.drawable.ic_add), null) },
                text = { Text(stringResource(R.string.add_task)) },
                containerColor = Gt.Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = LocalTabBarSpace.current + 8.dp),
            )
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskItem,
    canChange: Boolean,
    isLeader: Boolean,
    onStatus: (TaskStatus) -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
) {
    val done = task.status == TaskStatus.DONE
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onStatus(if (done) TaskStatus.TODO else TaskStatus.DONE) }, enabled = canChange) {
            Icon(
                painterResource(if (done) R.drawable.ic_check_circle else R.drawable.ic_circle_outline),
                stringResource(R.string.mark_done),
                tint = if (done) Gt.Primary else Gt.TextSecondary,
            )
        }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(task.title, style = GtType.Label.copy(textDecoration = if (done) TextDecoration.LineThrough else null))
            Text(
                if (done) stringResource(R.string.task_meta_done, task.assigneeName, task.completedAt.asShortDate()) else stringResource(R.string.task_meta_due, task.assigneeName, task.dueDate?.asShortDate() ?: "—"),
                style = GtType.Caption,
            )
            Pill(task.status.pill(), Modifier.padding(top = 6.dp))
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(painterResource(R.drawable.ic_more), stringResource(R.string.task_options), tint = Gt.TextSecondary) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf(TaskStatus.TODO to R.string.task_todo, TaskStatus.IN_PROGRESS to R.string.task_in_progress, TaskStatus.DONE to R.string.task_done).forEach { (status, label) ->
                    DropdownMenuItem(text = { Text(stringResource(label)) }, enabled = canChange, onClick = {
                        menu = false
                        onStatus(status)
                    })
                }
                if (isLeader) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit_task)) }, onClick = {
                        menu = false
                        onEdit()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.cancel_task), color = Gt.Error) }, onClick = {
                        menu = false
                        onCancel()
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditDialog(
    task: TaskItem?,
    members: List<TeamMember>,
    onDismiss: () -> Unit,
    onSave: (TaskRequest) -> Unit,
) {
    var title by remember { mutableStateOf(task?.title.orEmpty()) }
    var description by remember { mutableStateOf(task?.description.orEmpty()) }
    var assignee by remember { mutableStateOf(members.firstOrNull { it.id == task?.assigneeId }) }
    var due by remember { mutableStateOf(task?.dueDate) }
    var picking by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var assigneeError by remember { mutableStateOf<String?>(null) }
    val required = stringResource(R.string.error_required)

    if (picking) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = due?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() })
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { due = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    picking = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = pickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (task == null) R.string.add_task else R.string.edit_task), style = GtType.Title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GtTextField(title, {
                    title = it
                    titleError = null
                }, stringResource(R.string.field_task_title), error = titleError)
                GtTextField(description, { description = it }, stringResource(R.string.field_task_description), singleLine = false, minLines = 2)
                DropdownField(stringResource(R.string.field_assignee), members, assignee, { it.fullName }, {
                    assignee = it
                    assigneeError = null
                }, error = assigneeError)
                Box {
                    GtTextField(due?.asDate().orEmpty(), {}, stringResource(R.string.field_due_date), enabled = false, leadingIcon = R.drawable.ic_calendar)
                    Box(Modifier.matchParentSize().padding(bottom = 4.dp)) {
                        TextButton(onClick = { picking = true }, modifier = Modifier.fillMaxSize()) {}
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                titleError = if (title.isBlank()) required else null
                assigneeError = if (assignee == null) required else null
                val who = assignee
                if (titleError == null && who != null) onSave(TaskRequest(title.trim(), description.trim().ifEmpty { null }, who.id, due))
            }) { Text(stringResource(R.string.save), style = GtType.Label.copy(color = Gt.Primary)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), style = GtType.Label.copy(color = Gt.TextSecondary)) } },
        containerColor = Gt.Surface,
    )
}

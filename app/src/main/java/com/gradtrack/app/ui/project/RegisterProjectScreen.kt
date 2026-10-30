package com.gradtrack.app.ui.project

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.Resource
import com.gradtrack.app.data.model.Project
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.RegisterProjectRequest
import com.gradtrack.app.data.model.TeamInfo
import com.gradtrack.app.data.repository.ProjectRepository
import com.gradtrack.app.data.repository.TeamRepository
import com.gradtrack.app.data.repository.UploadFile
import com.gradtrack.app.ui.Routes
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.LocalSnackbar
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.components.DropdownField
import com.gradtrack.app.ui.components.EmptyState
import com.gradtrack.app.ui.components.ErrorState
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.GtTextField
import com.gradtrack.app.ui.components.MemberRow
import com.gradtrack.app.ui.components.PageHeader
import com.gradtrack.app.ui.components.PrimaryButton
import com.gradtrack.app.ui.components.ScreenList
import com.gradtrack.app.ui.components.SecondaryButton
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.components.SkeletonPage
import com.gradtrack.app.ui.components.TextAction
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RegisterData(val team: TeamInfo?, val project: Project?)

data class ProjectDraft(
    val title: String = "",
    val category: String = "",
    val problem: String = "",
    val objectives: String = "",
    val technologies: List<String> = emptyList(),
    val fileName: String? = null,
    /** A newly picked file; null keeps whatever was uploaded with the last submission. */
    val fileUri: Uri? = null,
    val targetUsers: String = "",
    val duration: String = "",
)

class RegisterProjectViewModel(
    private val projects: ProjectRepository,
    teams: TeamRepository,
) : LoadableViewModel<RegisterData>({
        val team = teams.myTeam()
        val overview = projects.overview()
        when {
            team is Resource.Failure -> team
            overview is Resource.Failure -> overview
            else ->
                Resource.Success(
                    RegisterData((team as Resource.Success).data.team, (overview as Resource.Success).data.project),
                )
        }
    }) {
    var step by mutableStateOf(0)
        private set
    var draft by mutableStateOf(ProjectDraft())
    private var prefilled = false

    fun prefill(project: Project?) {
        if (project == null || prefilled) return
        prefilled = true
        draft =
            ProjectDraft(
                title = project.title,
                category = project.category,
                problem = project.problemStatement.orEmpty(),
                objectives = project.objectives.orEmpty(),
                technologies = project.technologies,
                fileName = project.proposalFileName,
                targetUsers = project.targetUsers.orEmpty(),
                duration = project.durationMonths?.toString().orEmpty(),
            )
    }

    fun goTo(step: Int) {
        this.step = step.coerceIn(0, 2)
    }

    fun submit(file: UploadFile?) {
        val d = draft
        act({
            val registered =
                projects.register(
                    RegisterProjectRequest(d.title, d.category, d.problem, d.objectives, d.technologies, d.fileName, d.targetUsers, d.duration.toIntOrNull() ?: 0),
                )
            if (registered is Resource.Success && file != null) projects.uploadProposal(file) else registered
        }) { send(UiEvent.Done(R.string.proposal_submitted)) }
    }
}

private class StepOneErrors(
    val title: String? = null,
    val category: String? = null,
    val problem: String? = null,
    val objectives: String? = null,
    val targetUsers: String? = null,
    val duration: String? = null,
    val file: String? = null,
) {
    val any get() = listOf(title, category, problem, objectives, targetUsers, duration, file).any { it != null }
}

private const val MAX_FILE_BYTES = 25L * 1024 * 1024

@Composable
fun RegisterProjectScreen(onSubmitted: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = appViewModel { RegisterProjectViewModel(it.projects, it.team) }
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    EventsEffect(vm) { onSubmitted() }

    when (val current = state) {
        is UiState.Loading -> SkeletonPage()
        is UiState.Error -> ErrorState(current.error, vm::load)
        is UiState.Success -> {
            val data = current.data
            if (data.team == null) {
                ScreenList {
                    item { PageHeader(stringResource(R.string.register_heading), stringResource(R.string.crumb_register), stringResource(R.string.register_subtitle)) }
                    item {
                        EmptyState(
                            R.drawable.ic_team,
                            stringResource(R.string.empty_create_team_title),
                            stringResource(R.string.register_needs_team),
                            primary = stringResource(R.string.go_to_team),
                            onPrimary = { onNavigate(Routes.TEAM) },
                        )
                    }
                }
                return
            }
            val locked = data.project?.let { it.status != ProjectStatus.CHANGES_REQUESTED && it.status != ProjectStatus.REJECTED } == true
            if (locked) {
                ScreenList {
                    item { PageHeader(stringResource(R.string.register_heading), stringResource(R.string.crumb_register), stringResource(R.string.register_subtitle)) }
                    item { EmptyState(R.drawable.ic_lock, stringResource(R.string.proposal_locked_title), stringResource(R.string.proposal_locked_body)) }
                }
                return
            }
            LaunchedEffect(data.project) { vm.prefill(data.project) }
            Wizard(vm, data, busy, onNavigate)
        }
    }
}

@Composable
private fun Wizard(vm: RegisterProjectViewModel, data: RegisterData, busy: Boolean, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var errors by remember { mutableStateOf(StepOneErrors()) }
    val step = vm.step
    val d = vm.draft
    val required = stringResource(R.string.error_required)
    val tooLarge = stringResource(R.string.error_file_too_large)
    val fixFields = stringResource(R.string.error_fix_fields)
    val errTitle = stringResource(R.string.error_title_short)
    val errProblem = stringResource(R.string.error_problem_short)
    val errDuration = stringResource(R.string.error_duration)
    val errPdf = stringResource(R.string.error_pdf_required)
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current

    val pickPdf =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            var name = resources.getString(R.string.proposal_pdf_default_name)
            var size = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) }
                    cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = cursor.getLong(it) }
                }
            }
            if (size > MAX_FILE_BYTES) {
                errors = StepOneErrors(file = tooLarge)
            } else {
                vm.draft = vm.draft.copy(fileName = name, fileUri = uri)
                errors = StepOneErrors()
            }
        }

    fun validate(): Boolean {
        val duration = d.duration.toIntOrNull()
        errors =
            StepOneErrors(
                title = if (d.title.trim().length < 5) errTitle else null,
                category = if (d.category.isEmpty()) required else null,
                problem = if (d.problem.trim().length < 20) errProblem else null,
                objectives = if (d.objectives.isBlank()) required else null,
                targetUsers = if (d.targetUsers.isBlank()) required else null,
                duration = if (duration == null || duration !in 1..24) errDuration else null,
                file = if (d.fileName == null) errPdf else null,
            )
        return !errors.any
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            ScreenList {
                item { PageHeader(stringResource(R.string.register_heading), stringResource(R.string.crumb_register), stringResource(R.string.register_subtitle)) }
                item { StepPills(step) }
                when (step) {
                    0 ->
                        item {
                            StepOne(
                                draft = d,
                                onDraft = { vm.draft = it },
                                errors = errors,
                                supervisorName = data.team?.supervisorName,
                                onPickPdf = { pickPdf.launch(arrayOf("application/pdf")) },
                            )
                            if (errors.any) Text(fixFields, style = GtType.Caption.copy(color = Gt.Error), modifier = Modifier.padding(top = 8.dp))
                        }
                    1 ->
                        item {
                            SectionTitle(stringResource(R.string.step2_title))
                            Text(stringResource(R.string.step2_hint), style = GtType.Caption, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
                            GtCard(padding = 14.dp) { data.team?.members?.forEach { MemberRow(it) } }
                            TextAction(stringResource(R.string.manage_team), { onNavigate(Routes.TEAM) })
                        }
                    else -> item { Review(d, data) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Gt.Surface).navigationBarsPadding().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (step > 0) SecondaryButton(stringResource(R.string.back), { vm.goTo(step - 1) }, Modifier.weight(1f), enabled = !busy)
            val editing = data.project != null
            PrimaryButton(
                text = stringResource(if (step == 2) (if (editing) R.string.save_and_send else R.string.submit_proposal) else R.string.next),
                onClick = {
                    when (step) {
                        0 -> if (validate()) vm.goTo(1)
                        1 -> vm.goTo(2)
                        else ->
                            scope.launch {
                                val uri = d.fileUri
                                val bytes = uri?.let { withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(it)?.use { s -> s.readBytes() } }.getOrNull() } }
                                if (uri != null && bytes == null) {
                                    snackbar.showSnackbar(resources.getString(R.string.error_file_read))
                                } else {
                                    vm.submit(if (uri != null && bytes != null) UploadFile(d.fileName ?: "proposal.pdf", context.contentResolver.getType(uri), bytes) else null)
                                }
                            }
                    }
                },
                modifier = Modifier.weight(2f),
                loading = busy,
            )
        }
    }
}

@Composable
private fun StepPills(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(R.string.step_project_info, R.string.step_team_members, R.string.step_review_submit).forEachIndexed { index, label ->
            val (fg, bg) =
                when {
                    index == step -> Color.White to Gt.Primary
                    index < step -> Gt.PrimaryDark to Gt.PrimaryContainer
                    else -> Gt.TextSecondary to Gt.Surface
                }
            Text(
                stringResource(label),
                style = GtType.Pill.copy(color = fg),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(bg).padding(vertical = 9.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepOne(
    draft: ProjectDraft,
    onDraft: (ProjectDraft) -> Unit,
    errors: StepOneErrors,
    supervisorName: String?,
    onPickPdf: () -> Unit,
) {
    var tech by remember { mutableStateOf("") }
    val categories = stringArrayResource(R.array.project_categories).toList()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.field_required_hint), style = GtType.Caption)
        GtTextField(draft.title, { onDraft(draft.copy(title = it)) }, stringResource(R.string.field_project_title), error = errors.title)
        DropdownField(stringResource(R.string.field_category), categories, draft.category.ifEmpty { null }, { it }, { onDraft(draft.copy(category = it)) }, error = errors.category)
        GtTextField(supervisorName ?: stringResource(R.string.supervisor_not_assigned), {}, stringResource(R.string.field_supervisor), enabled = false, leadingIcon = R.drawable.ic_person)
        GtTextField(draft.problem, { onDraft(draft.copy(problem = it)) }, stringResource(R.string.field_problem), error = errors.problem, singleLine = false, minLines = 4)
        GtTextField(
            draft.objectives,
            { onDraft(draft.copy(objectives = it)) },
            stringResource(R.string.field_objectives),
            error = errors.objectives,
            helper = stringResource(R.string.objectives_helper),
            singleLine = false,
            minLines = 4,
        )
        GtTextField(draft.targetUsers, { onDraft(draft.copy(targetUsers = it)) }, stringResource(R.string.field_target_users), error = errors.targetUsers)
        GtTextField(
            draft.duration,
            { onDraft(draft.copy(duration = it.filter(Char::isDigit).take(2))) },
            stringResource(R.string.field_duration),
            error = errors.duration,
            keyboardType = KeyboardType.Number,
            suffix = stringResource(R.string.months),
        )

        Text(stringResource(R.string.field_technologies), style = GtType.Label, modifier = Modifier.padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GtTextField(tech, { tech = it }, stringResource(R.string.hint_technology), Modifier.weight(1f))
            SecondaryButton(stringResource(R.string.add), {
                val value = tech.trim()
                if (value.isNotEmpty() && draft.technologies.none { it.equals(value, ignoreCase = true) }) onDraft(draft.copy(technologies = draft.technologies + value))
                tech = ""
            })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            draft.technologies.forEach { item ->
                InputChip(
                    selected = false,
                    onClick = { onDraft(draft.copy(technologies = draft.technologies - item)) },
                    label = { Text(item) },
                    trailingIcon = { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.remove), modifier = Modifier.size(16.dp)) },
                )
            }
        }

        Text(stringResource(R.string.field_proposal_pdf), style = GtType.Label, modifier = Modifier.padding(top = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(BorderStroke(1.5.dp, if (errors.file != null) Gt.Error else Gt.Primary), RoundedCornerShape(14.dp))
                .background(Gt.Surface)
                .clickable(onClick = onPickPdf)
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_upload), null, tint = Gt.Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(10.dp))
            Text(draft.fileName ?: stringResource(R.string.upload_pdf_prompt), style = GtType.Label.copy(color = Gt.Primary))
        }
        if (errors.file != null) Text(errors.file, style = GtType.Caption.copy(color = Gt.Error))
    }
}

@Composable
private fun Review(d: ProjectDraft, data: RegisterData) {
    GtCard {
        SectionTitle(stringResource(R.string.step3_title))
        ReviewLine(R.string.field_project_title, d.title)
        ReviewLine(R.string.field_category, d.category)
        ReviewLine(R.string.field_supervisor, data.team?.supervisorName ?: stringResource(R.string.supervisor_not_assigned))
        ReviewLine(R.string.field_problem, d.problem)
        ReviewLine(R.string.field_objectives, d.objectives)
        ReviewLine(R.string.field_target_users, d.targetUsers)
        val months = d.duration.toIntOrNull()
        ReviewLine(R.string.field_duration, months?.let { LocalResources.current.getQuantityString(R.plurals.months_count, it, it) }.orEmpty())
        ReviewLine(R.string.field_technologies, d.technologies.joinToString(", ").ifEmpty { "—" })
        ReviewLine(R.string.field_proposal_pdf, d.fileName.orEmpty())
        data.team?.let { ReviewLine(R.string.title_team, stringResource(R.string.team_summary, it.members.size, it.maxMembers)) }
    }
}

@Composable
private fun ReviewLine(label: Int, value: String) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(stringResource(label).uppercase(), style = GtType.Overline)
        Text(value, style = GtType.Body)
    }
}


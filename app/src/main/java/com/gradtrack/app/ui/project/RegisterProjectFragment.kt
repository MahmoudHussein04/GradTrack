package com.gradtrack.app.ui.project

import com.gradtrack.app.ui.common.GradFragment
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.material.chip.Chip
import com.gradtrack.app.R
import com.gradtrack.app.data.Resource
import com.gradtrack.app.data.model.Project
import com.gradtrack.app.data.model.RegisterProjectRequest
import com.gradtrack.app.data.model.SupervisorOption
import com.gradtrack.app.data.model.TeamInfo
import com.gradtrack.app.data.repository.ProjectRepository
import com.gradtrack.app.data.repository.TeamRepository
import com.gradtrack.app.databinding.FragmentRegisterProjectBinding
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.applyPill
import com.gradtrack.app.ui.common.PillStyle
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.hideKeyboard
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.setVisible
import com.gradtrack.app.ui.common.showMessage
import com.gradtrack.app.ui.team.TeamMemberAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RegisterData(val team: TeamInfo?, val supervisors: List<SupervisorOption>, val project: Project?)

/** What the leader has typed so far. Kept in the ViewModel so the wizard survives rotation. */
data class ProjectDraft(
    val title: String = "",
    val category: String = "",
    val supervisorId: String? = null,
    val problem: String = "",
    val objectives: String = "",
    val technologies: List<String> = emptyList(),
    val fileName: String? = null,
    val targetUsers: String = "",
    val duration: Int? = null,
    val prefilled: Boolean = false,
)

class RegisterProjectViewModel(
    private val projects: ProjectRepository,
    teams: TeamRepository,
) : LoadableViewModel<RegisterData>({
        val team = teams.myTeam()
        val supervisors = projects.supervisors()
        val overview = projects.overview()
        when {
            team is Resource.Failure -> team
            supervisors is Resource.Failure -> supervisors
            overview is Resource.Failure -> overview
            else ->
                Resource.Success(
                    RegisterData((team as Resource.Success).data.team, (supervisors as Resource.Success).data, (overview as Resource.Success).data.project),
                )
        }
    }) {
    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step.asStateFlow()

    var draft = ProjectDraft()

    fun goTo(step: Int) {
        _step.value = step.coerceIn(0, 2)
    }

    fun submit() {
        val d = draft
        act({
            projects.register(
                RegisterProjectRequest(d.title, d.category, d.problem, d.objectives, d.technologies, d.supervisorId, d.fileName, d.targetUsers, d.duration ?: 0),
            )
        }) { send(UiEvent.Done(R.string.proposal_submitted)) }
    }
}

class RegisterProjectFragment : GradFragment() {
    private var _binding: FragmentRegisterProjectBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { RegisterProjectViewModel(it.projects, it.team) }
    private val memberAdapter = TeamMemberAdapter()
    private var supervisors: List<SupervisorOption> = emptyList()

    private val pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::onFilePicked) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.members.adapter = memberAdapter
        binding.category.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, resources.getStringArray(R.array.project_categories)))
        binding.uploadZone.setOnClickListener { pickPdf.launch(arrayOf("application/pdf")) }
        binding.addTech.setOnClickListener { addTechnology() }
        binding.techInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) addTechnology()
            actionId == EditorInfo.IME_ACTION_DONE
        }
        binding.manageTeam.setOnClickListener { findNavController().navigate(R.id.teamFragment) }
        binding.back.setOnClickListener { viewModel.goTo(viewModel.step.value - 1) }
        binding.next.setOnClickListener { onNext() }

        restoreDraft()

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) {
                if (state.data.team == null) {
                    showBlocked()
                    return@collect
                }
                prefill(state.data.project)
                supervisors = state.data.supervisors
                binding.supervisorLayout.helperText = if (supervisors.isEmpty()) getString(R.string.no_supervisors_yet) else null
                binding.next.tag = state.data.project != null
                binding.supervisor.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, supervisors.map { it.fullName }))
                viewModel.draft.supervisorId?.let { id -> supervisors.firstOrNull { it.id == id }?.let { binding.supervisor.setText(it.fullName, false) } }
                memberAdapter.submitList(state.data.team.members)
                renderStep(viewModel.step.value)
                binding.actionBar.setVisible(true)
            } else {
                binding.actionBar.setVisible(false)
            }
        }
        collect(viewModel.step) { renderStep(it) }
        collect(viewModel.busy) { busy ->
            binding.next.isEnabled = !busy
            binding.back.isEnabled = !busy
        }
        observeEvents(viewModel) {
            findNavController().navigate(R.id.proposalStatusFragment, null, navOptions { popUpTo(R.id.registerProjectFragment) { inclusive = true } })
        }
    }

    /** Leaders without a team cannot register a project yet: say so and point to the team screen. */
    private fun showBlocked() {
        listOf(binding.stepsRow, binding.step1, binding.step2, binding.step3, binding.actionBar).forEach { it.setVisible(false) }
        binding.blockedEmpty.setVisible(true)
        binding.blockedEmpty.bind(
            R.drawable.ic_team,
            getString(R.string.empty_create_team_title),
            getString(R.string.register_needs_team),
            getString(R.string.go_to_team),
            onPrimary = { findNavController().navigate(R.id.teamFragment) },
        )
    }

    /** Editing or resubmitting: start from what was sent last time (once per screen). */
    private fun prefill(project: Project?) {
        if (project == null || viewModel.draft.prefilled) return
        viewModel.draft =
            ProjectDraft(
                title = project.title,
                category = project.category,
                supervisorId = project.supervisorId,
                problem = project.problemStatement.orEmpty(),
                objectives = project.objectives.orEmpty(),
                technologies = project.technologies,
                fileName = project.proposalFileName,
                targetUsers = project.targetUsers.orEmpty(),
                duration = project.durationMonths,
                prefilled = true,
            )
        val d = viewModel.draft
        binding.projectTitle.setText(d.title)
        binding.problem.setText(d.problem)
        binding.objectives.setText(d.objectives)
        binding.targetUsers.setText(d.targetUsers)
        binding.duration.setText(d.duration?.toString())
        binding.techChips.removeAllViews()
        restoreDraft()
    }

    private fun restoreDraft() {
        val d = viewModel.draft
        binding.category.setText(d.category, false)
        d.technologies.forEach(::addChip)
        renderFile(d.fileName)
    }

    private fun readDraft() {
        viewModel.draft =
            viewModel.draft.copy(
                title = binding.projectTitle.text?.toString().orEmpty().trim(),
                category = binding.category.text?.toString().orEmpty().trim(),
                supervisorId = supervisors.firstOrNull { it.fullName == binding.supervisor.text?.toString() }?.id,
                problem = binding.problem.text?.toString().orEmpty().trim(),
                objectives = binding.objectives.text?.toString().orEmpty().trim(),
                targetUsers = binding.targetUsers.text?.toString().orEmpty().trim(),
                duration = binding.duration.text?.toString()?.toIntOrNull(),
            )
    }

    private fun addTechnology() {
        val text = binding.techInput.text?.toString().orEmpty().trim()
        if (text.isEmpty() || viewModel.draft.technologies.any { it.equals(text, ignoreCase = true) }) return
        viewModel.draft = viewModel.draft.copy(technologies = viewModel.draft.technologies + text)
        addChip(text)
        binding.techInput.text = null
    }

    private fun addChip(text: String) {
        val chip =
            Chip(requireContext()).apply {
                this.text = text
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    binding.techChips.removeView(this)
                    viewModel.draft = viewModel.draft.copy(technologies = viewModel.draft.technologies - text)
                }
            }
        binding.techChips.addView(chip)
    }

    private fun onFilePicked(uri: Uri) {
        val resolver = requireContext().contentResolver
        var name = getString(R.string.proposal_pdf_default_name)
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = cursor.getLong(it) }
            }
        }
        if (size > MAX_FILE_BYTES) {
            binding.uploadError.text = getString(R.string.error_file_too_large)
            binding.uploadError.setVisible(true)
            return
        }
        viewModel.draft = viewModel.draft.copy(fileName = name)
        renderFile(name)
    }

    private fun renderFile(name: String?) {
        binding.uploadLabel.text = name ?: getString(R.string.upload_pdf_prompt)
        binding.uploadError.setVisible(false)
    }

    private fun onNext() {
        hideKeyboard()
        when (viewModel.step.value) {
            0 -> {
                readDraft()
                if (validateStepOne()) viewModel.goTo(1)
            }
            1 -> viewModel.goTo(2)
            else -> viewModel.submit()
        }
    }

    private fun validateStepOne(): Boolean {
        val d = viewModel.draft
        val required = getString(R.string.error_required)
        binding.titleLayout.error = if (d.title.length < 5) getString(R.string.error_title_short) else null
        binding.categoryLayout.error = if (d.category.isEmpty()) required else null
        binding.supervisorLayout.error = if (d.supervisorId == null) required else null
        binding.problemLayout.error = if (d.problem.length < 20) getString(R.string.error_problem_short) else null
        binding.objectivesLayout.error = if (d.objectives.isEmpty()) required else null
        binding.targetUsersLayout.error = if (d.targetUsers.isEmpty()) required else null
        binding.durationLayout.error = if (d.duration == null || d.duration !in 1..24) getString(R.string.error_duration) else null
        val noFile = d.fileName == null
        binding.uploadError.text = getString(R.string.error_pdf_required)
        binding.uploadError.setVisible(noFile)
        val ok = listOf(binding.titleLayout, binding.categoryLayout, binding.supervisorLayout, binding.problemLayout, binding.objectivesLayout, binding.targetUsersLayout, binding.durationLayout).all { it.error == null } && !noFile
        if (!ok) showMessage(getString(R.string.error_fix_fields))
        return ok
    }

    private fun renderStep(step: Int) {
        binding.step1.setVisible(step == 0)
        binding.step2.setVisible(step == 1)
        binding.step3.setVisible(step == 2)
        binding.back.setVisible(step > 0)
        val editing = binding.next.tag == true
        binding.next.setText(if (step == 2) (if (editing) R.string.save_and_send else R.string.submit_proposal) else R.string.next)
        listOf(binding.stepOne, binding.stepTwo, binding.stepThree).forEachIndexed { index, view ->
            view.applyPill(
                if (index == step) {
                    PillStyle(0, R.color.white, R.color.primary)
                } else if (index < step) {
                    PillStyle(0, R.color.primary_dark, R.color.primary_container)
                } else {
                    PillStyle(0, R.color.text_secondary, R.color.surface)
                },
            )
        }
        if (step == 2) renderReview()
    }

    private fun renderReview() {
        val d = viewModel.draft
        binding.reviewTitle.text = d.title
        binding.reviewCategory.text = d.category
        binding.reviewSupervisor.text = supervisors.firstOrNull { it.id == d.supervisorId }?.fullName
        binding.reviewProblem.text = d.problem
        binding.reviewObjectives.text = d.objectives
        binding.reviewTech.text = d.technologies.joinToString(", ").ifEmpty { "—" }
        binding.reviewFile.text = d.fileName
        val team = (viewModel.state.value as? UiState.Success)?.data?.team
        binding.reviewTeam.text = team?.let { getString(R.string.team_summary, it.members.size, it.maxMembers) }
        binding.reviewTargetUsers.text = d.targetUsers
        binding.reviewDuration.text = d.duration?.let { resources.getQuantityString(R.plurals.months_count, it, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MAX_FILE_BYTES = 25L * 1024 * 1024
    }
}

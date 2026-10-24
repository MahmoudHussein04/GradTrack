package com.gradtrack.app.ui.milestones

import com.gradtrack.app.ui.common.GradFragment
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gradtrack.app.R
import com.gradtrack.app.data.Resource
import com.gradtrack.app.data.model.Milestone
import com.gradtrack.app.data.model.MilestoneStatus
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.Submission
import com.gradtrack.app.data.repository.ProjectRepository
import com.gradtrack.app.data.repository.UploadFile
import com.gradtrack.app.databinding.FragmentMilestonesBinding
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.container
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.setVisible
import com.gradtrack.app.ui.common.showMessage
import com.gradtrack.app.ui.project.MilestoneAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MilestonesData(val milestones: List<Milestone>, val submissions: List<Submission>)

class MilestonesViewModel(private val repository: ProjectRepository) : LoadableViewModel<MilestonesData>({
        val milestones = repository.milestones()
        val submissions = repository.submissions()
        when {
            milestones is Resource.Failure -> milestones
            submissions is Resource.Failure -> submissions
            else -> Resource.Success(MilestonesData((milestones as Resource.Success).data, (submissions as Resource.Success).data))
        }
    }) {
    fun upload(milestoneId: String, file: UploadFile) =
        act({ repository.upload(milestoneId, file) }) { created ->
            patch { data ->
                data.copy(submissions = data.submissions.filterNot { it.milestoneId == created.milestoneId && it.submittedAt == null } + created)
            }
            send(UiEvent.Message(R.string.deliverable_uploaded))
        }
}

class MilestonesFragment : GradFragment() {
    private var _binding: FragmentMilestonesBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { MilestonesViewModel(it.projects) }
    private val milestoneAdapter = MilestoneAdapter()
    private val submissionAdapter = SubmissionAdapter()
    private var target: Milestone? = null

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::onFilePicked) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMilestonesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.milestones.adapter = milestoneAdapter
        binding.submissions.adapter = submissionAdapter
        binding.refresh.setOnRefreshListener { viewModel.refresh() }
        // Only the team leader submits deliverables (Use Case matrix, docs/gradtrack §2).
        binding.upload.setVisible(container.session.user?.role == Role.LEADER)
        binding.upload.setOnClickListener { pickFile.launch(arrayOf("application/pdf", "application/zip", "image/*")) }

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) {
                target = state.data.milestones.firstOrNull { it.status == MilestoneStatus.IN_PROGRESS } ?: state.data.milestones.firstOrNull { it.status == MilestoneStatus.UPCOMING }
                val noProject = state.data.milestones.isEmpty()
                binding.empty.setVisible(noProject)
                binding.timelineCard.setVisible(!noProject)
                binding.submissionsCard.setVisible(!noProject)
                if (noProject) binding.empty.bind(R.drawable.ic_flag, getString(R.string.empty_milestones_title), getString(R.string.empty_milestones_body))
                milestoneAdapter.submitList(state.data.milestones)
                submissionAdapter.submitList(state.data.submissions)
                binding.submissionsEmpty.setVisible(state.data.submissions.isEmpty())
            }
        }
        collect(viewModel.refreshing) { binding.refresh.isRefreshing = it }
        collect(viewModel.busy) { binding.upload.isEnabled = !it }
        observeEvents(viewModel)
    }

    private fun onFilePicked(uri: Uri) {
        val milestone = target
        if (milestone == null) {
            showMessage(getString(R.string.error_no_open_milestone))
            return
        }
        val resolver = requireContext().contentResolver
        var name = "deliverable"
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = cursor.getLong(it) }
            }
        }
        if (size > MAX_FILE_BYTES) {
            showMessage(getString(R.string.error_file_too_large))
            return
        }
        val mime = resolver.getType(uri)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.upload_deliverable)
            .setMessage(getString(R.string.upload_confirm, name, milestone.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.upload) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val bytes = withContext(Dispatchers.IO) { runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() }
                    if (bytes == null) showMessage(getString(R.string.error_file_read)) else viewModel.upload(milestone.id, UploadFile(name, mime, bytes))
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MAX_FILE_BYTES = 25L * 1024 * 1024
    }
}

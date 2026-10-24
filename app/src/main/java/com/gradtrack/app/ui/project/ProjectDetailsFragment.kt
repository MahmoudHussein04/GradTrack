package com.gradtrack.app.ui.project

import com.gradtrack.app.ui.common.GradFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gradtrack.app.R
import com.gradtrack.app.data.model.ProjectOverview
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.repository.ProjectRepository
import com.gradtrack.app.databinding.FragmentProjectDetailsBinding
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.applyPill
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.container
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.common.promptText
import com.gradtrack.app.ui.common.setVisible

class ProjectDetailsViewModel(private val repository: ProjectRepository) : LoadableViewModel<ProjectOverview>({ repository.overview() }) {
    fun reply(text: String) =
        act({ repository.reply(text) }) { feedback ->
            patch { it.copy(feedback = it.feedback + feedback) }
            send(UiEvent.Message(R.string.reply_sent))
        }
}

class ProjectDetailsFragment : GradFragment() {
    private var _binding: FragmentProjectDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { ProjectDetailsViewModel(it.projects) }
    private val milestoneAdapter = MilestoneAdapter()
    private val feedbackAdapter = FeedbackAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProjectDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isLeader = container.session.user?.role == Role.LEADER
        val nav = findNavController()

        binding.milestones.adapter = milestoneAdapter
        binding.feedback.adapter = feedbackAdapter
        binding.refresh.setOnRefreshListener { viewModel.refresh() }
        binding.editDetails.setVisible(isLeader)
        binding.editDetails.setOnClickListener { nav.navigate(R.id.registerProjectFragment) }
        binding.registerProject.setVisible(isLeader)
        binding.registerProject.setOnClickListener { nav.navigate(R.id.registerProjectFragment) }
        binding.emptyBody.setText(if (isLeader) R.string.no_project_leader else R.string.no_project_student)
        binding.openTeam.setOnClickListener { nav.navigate(R.id.teamFragment) }
        binding.openProposal.setOnClickListener { nav.navigate(R.id.proposalStatusFragment) }
        binding.viewSubmissions.setOnClickListener { nav.navigate(R.id.milestonesFragment) }
        binding.reply.setOnClickListener { promptText(R.string.reply_to_feedback, R.string.hint_reply, R.string.send) { viewModel.reply(it) } }

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) bind(state.data)
        }
        collect(viewModel.refreshing) { binding.refresh.isRefreshing = it }
        collect(viewModel.busy) { binding.reply.isEnabled = !it }
        observeEvents(viewModel)
    }

    private fun bind(data: ProjectOverview) {
        val project = data.project
        binding.emptyCard.setVisible(project == null)
        binding.content.setVisible(project != null)
        if (project == null) return

        binding.title.text = project.title
        binding.meta.text = getString(R.string.project_meta, project.category, project.academicYear.orEmpty())
        binding.status.applyPill(project.status.pill())
        binding.percent.text = "${project.progress}%"
        binding.progress.setProgressCompat(project.progress, true)
        binding.milestonesDone.text = getString(R.string.milestones_completed, project.completedMilestones, project.totalMilestones)

        milestoneAdapter.submitList(data.milestones)
        feedbackAdapter.submitList(data.feedback)
        binding.feedbackEmpty.setVisible(data.feedback.isEmpty())

        val submission = data.latestSubmission
        binding.submissionEmpty.setVisible(submission == null)
        binding.submissionGroup.setVisible(submission != null)
        if (submission != null) {
            binding.submissionName.text = submission.fileName
            binding.submissionDate.text = getString(R.string.uploaded_on, submission.submittedAt.asDate())
            binding.submissionStatus.applyPill(submission.status.pill())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

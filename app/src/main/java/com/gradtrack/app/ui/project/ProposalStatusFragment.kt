package com.gradtrack.app.ui.project

import com.gradtrack.app.ui.common.GradFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gradtrack.app.R
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.ProposalStatusInfo
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.repository.ProjectRepository
import com.gradtrack.app.databinding.FragmentProposalStatusBinding
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.applyPill
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.container
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.common.setVisible

class ProposalStatusViewModel(repository: ProjectRepository) : LoadableViewModel<ProposalStatusInfo>({ repository.proposalStatus() })

class ProposalStatusFragment : GradFragment() {
    private var _binding: FragmentProposalStatusBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { ProposalStatusViewModel(it.projects) }
    private val stepAdapter = StepAdapter()
    private val feedbackAdapter = FeedbackAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProposalStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.steps.adapter = stepAdapter
        binding.feedback.adapter = feedbackAdapter
        binding.refresh.setOnRefreshListener { viewModel.refresh() }
        binding.revise.setOnClickListener { findNavController().navigate(R.id.registerProjectFragment) }

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) bind(state.data)
        }
        collect(viewModel.refreshing) { binding.refresh.isRefreshing = it }
        observeEvents(viewModel)
    }

    private fun bind(data: ProposalStatusInfo) {
        binding.status.applyPill(data.status.pill())
        binding.title.text = data.title
        binding.dates.text = getString(R.string.proposal_dates, data.submittedAt.asDate(), data.updatedAt.asDate())
        binding.message.setText(
            when (data.status) {
                ProjectStatus.UNDER_REVIEW, ProjectStatus.SUBMITTED -> R.string.proposal_msg_under_review
                ProjectStatus.CHANGES_REQUESTED -> R.string.proposal_msg_changes
                ProjectStatus.REJECTED -> R.string.proposal_msg_rejected
                ProjectStatus.DRAFT -> R.string.proposal_msg_draft
                else -> R.string.proposal_msg_approved
            },
        )
        binding.documentRow.setVisible(data.documentName != null)
        binding.documentName.text = data.documentName
        stepAdapter.submitList(data.steps)
        feedbackAdapter.submitList(data.feedback)
        binding.feedbackEmpty.setVisible(data.feedback.isEmpty())
        binding.revise.setVisible(data.status == ProjectStatus.CHANGES_REQUESTED && container.session.user?.role == Role.LEADER)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

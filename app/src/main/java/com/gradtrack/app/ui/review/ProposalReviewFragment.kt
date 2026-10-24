package com.gradtrack.app.ui.review

import com.gradtrack.app.ui.common.GradFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.gradtrack.app.R
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.ProposalDetail
import com.gradtrack.app.data.model.ReviewDecision
import com.gradtrack.app.data.repository.ReviewRepository
import com.gradtrack.app.databinding.FragmentProposalReviewBinding
import com.gradtrack.app.databinding.ItemDocumentBinding
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.applyPill
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.hideKeyboard
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.common.setVisible
import com.gradtrack.app.ui.team.TeamMemberAdapter

class ProposalReviewViewModel(
    private val repository: ReviewRepository,
    private val proposalId: String,
) : LoadableViewModel<ProposalDetail>({ repository.proposal(proposalId) }) {
    fun review(decision: ReviewDecision, comment: String) =
        act({ repository.reviewProposal(proposalId, decision, comment) }) {
            send(UiEvent.Message(R.string.decision_sent))
            refresh()
        }
}

class ProposalReviewFragment : GradFragment() {
    private var _binding: FragmentProposalReviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { ProposalReviewViewModel(it.reviews, requireArguments().getString("proposalId").orEmpty()) }
    private val memberAdapter = TeamMemberAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProposalReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.members.adapter = memberAdapter
        binding.tabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) = showTab(tab.position)

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            },
        )
        showTab(binding.tabs.selectedTabPosition.coerceAtLeast(0))

        binding.approve.setOnClickListener { decide(ReviewDecision.APPROVE, R.string.approve_project, R.string.approve_confirm) }
        binding.requestChanges.setOnClickListener { decide(ReviewDecision.CHANGES, R.string.request_changes, R.string.changes_confirm) }
        binding.reject.setOnClickListener { decide(ReviewDecision.REJECT, R.string.reject_proposal, R.string.reject_confirm) }

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) bind(state.data)
        }
        collect(viewModel.busy) { busy ->
            binding.approve.isEnabled = !busy
            binding.requestChanges.isEnabled = !busy
            binding.reject.isEnabled = !busy
        }
        observeEvents(viewModel)
    }

    private fun showTab(position: Int) {
        binding.detailsCard.setVisible(position == 0)
        binding.teamCard.setVisible(position == 1)
        binding.documentsCard.setVisible(position == 2)
    }

    /** Request changes and reject need an explanation for the team; approving may go without one. */
    private fun decide(decision: ReviewDecision, @androidx.annotation.StringRes title: Int, @androidx.annotation.StringRes message: Int) {
        val comment = binding.comment.text?.toString().orEmpty().trim()
        if (decision != ReviewDecision.APPROVE && comment.isEmpty()) {
            binding.commentLayout.error = getString(R.string.error_feedback_required)
            return
        }
        binding.commentLayout.error = null
        hideKeyboard()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ -> viewModel.review(decision, comment) }
            .show()
    }

    private fun bind(data: ProposalDetail) {
        binding.status.applyPill(data.status.pill())
        binding.title.text = data.title
        binding.meta.text = getString(R.string.review_meta, data.teamName, data.submittedAt.asDate())
        binding.category.text = data.category
        binding.targetUsers.text = data.targetUsers
        binding.duration.text = data.duration
        binding.description.text = data.description
        binding.objectives.text = data.objectives.joinToString("\n") { "•  $it" }
        binding.techChips.removeAllViews()
        data.technologies.forEach { tech -> binding.techChips.addView(Chip(requireContext()).apply { text = tech }) }
        binding.notes.text = data.notes
        binding.notes.setVisible(!data.notes.isNullOrBlank())
        binding.notesTitle.setVisible(!data.notes.isNullOrBlank())
        memberAdapter.submitList(data.members)
        binding.documents.removeAllViews()
        data.documents.forEach { doc ->
            val row = ItemDocumentBinding.inflate(layoutInflater, binding.documents, false)
            row.name.text = doc.name
            row.size.text = doc.sizeLabel
            binding.documents.addView(row.root)
        }

        val pending = data.status == ProjectStatus.UNDER_REVIEW || data.status == ProjectStatus.SUBMITTED
        binding.decisionGroup.setVisible(pending)
        binding.decided.setVisible(!pending)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

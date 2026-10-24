package com.gradtrack.app.ui.review

import com.gradtrack.app.ui.common.GradFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gradtrack.app.R
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.ProposalSummary
import com.gradtrack.app.data.repository.ReviewRepository
import com.gradtrack.app.databinding.FragmentProposalListBinding
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.setVisible
import com.gradtrack.app.ui.dashboard.ProposalAdapter

class ProposalListViewModel(repository: ReviewRepository) : LoadableViewModel<List<ProposalSummary>>({ repository.proposals() })

class ProposalListFragment : GradFragment() {
    private var _binding: FragmentProposalListBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { ProposalListViewModel(it.reviews) }
    private val adapter = ProposalAdapter { findNavController().navigate(R.id.proposalReviewFragment, bundleOf("proposalId" to it.id)) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProposalListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.adapter = adapter
        binding.empty.bind(R.drawable.ic_proposals, getString(R.string.empty_proposals_title), getString(R.string.no_proposals))
        binding.refresh.setOnRefreshListener { viewModel.refresh() }

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) {
                // Proposals waiting for a decision come first.
                adapter.submitList(state.data.sortedByDescending { it.status == ProjectStatus.UNDER_REVIEW })
                binding.empty.setVisible(state.data.isEmpty())
            }
        }
        collect(viewModel.refreshing) { binding.refresh.isRefreshing = it }
        observeEvents(viewModel)
    }

    override fun onResume() {
        super.onResume()
        // Coming back from a review: the list must show the new decision.
        if (viewModel.state.value is UiState.Success) viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

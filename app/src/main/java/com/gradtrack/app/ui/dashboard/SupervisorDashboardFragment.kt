package com.gradtrack.app.ui.dashboard

import com.gradtrack.app.ui.common.GradFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gradtrack.app.R
import com.gradtrack.app.data.model.SupervisorDashboard
import com.gradtrack.app.data.repository.DashboardRepository
import com.gradtrack.app.databinding.FragmentSupervisorDashboardBinding
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.container
import com.gradtrack.app.ui.common.greetingName
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.setVisible
import java.time.LocalTime

class SupervisorDashboardViewModel(repository: DashboardRepository) : LoadableViewModel<SupervisorDashboard>({ repository.supervisor() })

class SupervisorDashboardFragment : GradFragment() {
    private var _binding: FragmentSupervisorDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { SupervisorDashboardViewModel(it.dashboard) }
    private val proposalAdapter = ProposalAdapter { findNavController().navigate(R.id.proposalReviewFragment, bundleOf("proposalId" to it.id)) }
    private val deadlineAdapter = DeadlineAdapter()
    private val activityAdapter = ActivityAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSupervisorDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name = container.session.user?.greetingName()
        val hour = LocalTime.now().hour
        binding.greeting.text =
            when {
                name == null -> getString(if (hour < 12) R.string.greeting_morning_plain else if (hour < 18) R.string.greeting_afternoon_plain else R.string.greeting_evening_plain)
                hour < 12 -> getString(R.string.greeting_morning_dr, name)
                hour < 18 -> getString(R.string.greeting_afternoon_dr, name)
                else -> getString(R.string.greeting_evening_dr, name)
            }

        binding.proposals.adapter = proposalAdapter
        binding.deadlines.adapter = deadlineAdapter
        binding.activity.adapter = activityAdapter
        binding.refresh.setOnRefreshListener { viewModel.refresh() }
        binding.viewAll.setOnClickListener { findNavController().navigate(R.id.proposalListFragment) }

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) bind(state.data)
        }
        collect(viewModel.refreshing) { binding.refresh.isRefreshing = it }
        observeEvents(viewModel)
    }

    private fun bind(data: SupervisorDashboard) {
        val nav = findNavController()
        val hero = binding.heroCard
        if (data.pendingProposals > 0) {
            hero.showNextStep(
                R.drawable.ic_proposals,
                resources.getQuantityString(R.plurals.proposals_waiting, data.pendingProposals, data.pendingProposals),
                getString(R.string.proposals_waiting_body),
                getString(R.string.review_now),
            ) { nav.navigate(R.id.proposalListFragment) }
        } else {
            hero.showNextStep(R.drawable.ic_check_circle, getString(R.string.all_caught_up), getString(R.string.all_caught_up_body), getString(R.string.view_proposals)) {
                nav.navigate(R.id.proposalListFragment)
            }
        }
        binding.statActive.bind(getString(R.string.stat_projects_short), data.activeProjects.toString(), getString(R.string.stat_active), R.drawable.ic_project)
        binding.statSubmissions.bind(getString(R.string.stat_to_review_short), data.submissionsToReview.toString(), getString(R.string.stat_submissions), R.drawable.ic_submissions)
        binding.statDeadlines.bind(getString(R.string.stat_deadlines_short), data.upcomingDeadlines.toString(), getString(R.string.stat_next_two_weeks), R.drawable.ic_calendar)
        proposalAdapter.submitList(data.proposals)
        binding.proposalsEmpty.setVisible(data.proposals.isEmpty())
        deadlineAdapter.submitList(data.deadlines)
        binding.deadlinesEmpty.setVisible(data.deadlines.isEmpty())
        activityAdapter.submitList(data.activity)
        binding.activityEmpty.setVisible(data.activity.isEmpty())
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.state.value is UiState.Success) viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

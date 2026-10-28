package com.gradtrack.app.ui.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.Resource
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.ProposalSummary
import com.gradtrack.app.data.repository.ReviewRepository
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.components.EmptyState
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.PageHeader
import com.gradtrack.app.ui.dashboard.ProposalRow
import androidx.lifecycle.compose.LifecycleResumeEffect

private val awaitingDecision = setOf(ProjectStatus.UNDER_REVIEW, ProjectStatus.SUBMITTED)

class ProposalListViewModel(private val repository: ReviewRepository) : LoadableViewModel<List<ProposalSummary>>({
        when (val result = repository.proposals()) {
            is Resource.Success -> Resource.Success(result.data.sortedBy { it.status !in awaitingDecision })
            is Resource.Failure -> result
        }
    })

@Composable
fun ProposalListScreen(onOpen: (String) -> Unit) {
    val vm = appViewModel { ProposalListViewModel(it.reviews) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    EventsEffect(vm)
    LifecycleResumeEffect(vm) {
        vm.refresh()
        onPauseOrDispose {}
    }

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { proposals ->
        item { PageHeader(stringResource(R.string.title_proposal_reviews), stringResource(R.string.crumb_proposals), stringResource(R.string.proposal_list_subtitle)) }
        if (proposals.isEmpty()) {
            item { EmptyState(R.drawable.ic_file, stringResource(R.string.empty_proposals_title), stringResource(R.string.no_proposals)) }
        } else {
            item {
                GtCard(padding = 16.dp) {
                    proposals.forEach { proposal -> ProposalRow(proposal) { onOpen(proposal.id) } }
                }
            }
        }
    }
}

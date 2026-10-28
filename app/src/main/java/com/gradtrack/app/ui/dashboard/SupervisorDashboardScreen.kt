package com.gradtrack.app.ui.dashboard

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.model.SupervisorDashboard
import com.gradtrack.app.data.repository.DashboardRepository
import com.gradtrack.app.ui.Routes
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.LocalContainer
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.MutedText
import com.gradtrack.app.ui.components.NextStepHero
import com.gradtrack.app.ui.components.PageHeader
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.components.StatCard
import com.gradtrack.app.ui.components.StatRow
import com.gradtrack.app.ui.components.TextAction

class SupervisorDashboardViewModel(repository: DashboardRepository) : LoadableViewModel<SupervisorDashboard>({ repository.supervisor() })

@Composable
fun SupervisorDashboardScreen(onNavigate: (String) -> Unit) {
    val vm = appViewModel { SupervisorDashboardViewModel(it.dashboard) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val user by LocalContainer.current.session.currentUser.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    EventsEffect(vm)
    LifecycleResumeEffect(vm) {
        if (vm.state.value is UiState.Success) vm.refresh()
        onPauseOrDispose {}
    }

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { data: SupervisorDashboard ->
        item {
            PageHeader(
                title = greeting(user, doctor = true),
                overline = stringResource(R.string.supervisor_workspace),
                subtitle = stringResource(R.string.dashboard_subtitle_supervisor),
            )
        }
        item {
            if (data.pendingProposals > 0) {
                NextStepHero(
                    R.drawable.ic_proposals,
                    resources.getQuantityString(R.plurals.proposals_waiting, data.pendingProposals, data.pendingProposals),
                    stringResource(R.string.proposals_waiting_body),
                    stringResource(R.string.review_now),
                ) { onNavigate(Routes.PROPOSALS) }
            } else {
                NextStepHero(R.drawable.ic_check_circle, stringResource(R.string.all_caught_up), stringResource(R.string.all_caught_up_body), stringResource(R.string.view_proposals)) {
                    onNavigate(Routes.PROPOSALS)
                }
            }
        }
        item {
            StatRow {
                StatCard(stringResource(R.string.stat_projects_short), data.activeProjects.toString(), Modifier.weight(1f), stringResource(R.string.stat_active), R.drawable.ic_project)
                StatCard(stringResource(R.string.stat_to_review_short), data.submissionsToReview.toString(), Modifier.weight(1f), stringResource(R.string.stat_submissions), R.drawable.ic_submissions)
                StatCard(stringResource(R.string.stat_deadlines_short), data.upcomingDeadlines.toString(), Modifier.weight(1f), stringResource(R.string.stat_next_two_weeks), R.drawable.ic_calendar)
            }
        }
        item {
            GtCard {
                SectionTitle(stringResource(R.string.pending_proposals)) { TextAction(stringResource(R.string.view_all), { onNavigate(Routes.PROPOSALS) }) }
                if (data.proposals.isEmpty()) {
                    MutedText(stringResource(R.string.no_pending_proposals), Modifier.padding(top = 4.dp))
                } else {
                    data.proposals.forEach { ProposalRow(it) { onNavigate(Routes.proposalReview(it.id)) } }
                }
            }
        }
        deadlinesAndActivity(
            data.deadlines.isEmpty(),
            { data.deadlines.forEach { DeadlineRow(it) } },
            data.activity.isEmpty(),
            { data.activity.forEach { ActivityRow(it) } },
            deadlinesTitle = R.string.deadline_reminders,
        )
    }
}

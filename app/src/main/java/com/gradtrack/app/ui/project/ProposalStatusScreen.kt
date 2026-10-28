package com.gradtrack.app.ui.project

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.ProposalStatusInfo
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.StepState
import com.gradtrack.app.data.repository.ProjectRepository
import com.gradtrack.app.ui.Routes
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.LocalContainer
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.components.FeedbackRow
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.MutedText
import com.gradtrack.app.ui.components.Pill
import com.gradtrack.app.ui.components.PrimaryButton
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.components.TimelineRow
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType

class ProposalStatusViewModel(repository: ProjectRepository) : LoadableViewModel<ProposalStatusInfo>({ repository.proposalStatus() })

@Composable
fun ProposalStatusScreen(onNavigate: (String) -> Unit) {
    val vm = appViewModel { ProposalStatusViewModel(it.projects) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val user by LocalContainer.current.session.currentUser.collectAsStateWithLifecycle()
    EventsEffect(vm)

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { data ->
        item {
            GtCard {
                Pill(data.status.pill())
                Text(data.title, style = GtType.Headline, modifier = Modifier.padding(top = 10.dp))
                Text(stringResource(R.string.proposal_dates, data.submittedAt.asDate(), data.updatedAt.asDate()), style = GtType.Caption, modifier = Modifier.padding(top = 4.dp))
                Text(
                    stringResource(
                        when (data.status) {
                            ProjectStatus.UNDER_REVIEW, ProjectStatus.SUBMITTED -> R.string.proposal_msg_under_review
                            ProjectStatus.CHANGES_REQUESTED -> R.string.proposal_msg_changes
                            ProjectStatus.REJECTED -> R.string.proposal_msg_rejected
                            ProjectStatus.DRAFT -> R.string.proposal_msg_draft
                            else -> R.string.proposal_msg_approved
                        },
                    ),
                    style = GtType.Body,
                    modifier = Modifier.padding(top = 12.dp),
                )
                data.documentName?.let { name ->
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_file), null, tint = Gt.Primary, modifier = Modifier.size(20.dp))
                        Text(name, style = GtType.Label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                val canRevise = user?.role == Role.LEADER && (data.status == ProjectStatus.CHANGES_REQUESTED || data.status == ProjectStatus.REJECTED)
                if (canRevise) {
                    PrimaryButton(stringResource(R.string.revise_resubmit), { onNavigate(Routes.REGISTER_PROJECT) }, Modifier.fillMaxWidth().padding(top = 14.dp))
                }
            }
        }
        item {
            GtCard {
                SectionTitle(stringResource(R.string.review_timeline))
                Text(stringResource(R.string.review_timeline_hint), style = GtType.Caption, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
                data.steps.forEachIndexed { index, step ->
                    TimelineRow(
                        title = step.label,
                        subtitle =
                            stringResource(
                                when (step.state) {
                                    StepState.DONE -> R.string.step_completed
                                    StepState.CURRENT -> R.string.step_in_progress
                                    StepState.WAITING -> R.string.step_waiting
                                },
                            ),
                        state = step.state,
                        isLast = index == data.steps.lastIndex,
                    )
                }
            }
        }
        item {
            GtCard {
                SectionTitle(stringResource(R.string.supervisor_feedback))
                if (data.feedback.isEmpty()) MutedText(stringResource(R.string.no_feedback_after_review), Modifier.padding(top = 8.dp))
                data.feedback.forEach { FeedbackRow(it) }
            }
        }
    }
}

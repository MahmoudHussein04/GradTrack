package com.gradtrack.app.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.ProposalDetail
import com.gradtrack.app.data.model.ReviewDecision
import com.gradtrack.app.data.repository.ReviewRepository
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.LocalContainer
import com.gradtrack.app.ui.common.rememberFileOpener
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.components.ConfirmDialog
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.GtTextField
import com.gradtrack.app.ui.components.IconTile
import com.gradtrack.app.ui.components.LabeledValue
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.MemberRow
import com.gradtrack.app.ui.components.MutedText
import com.gradtrack.app.ui.components.Pill
import com.gradtrack.app.ui.components.PrimaryButton
import com.gradtrack.app.ui.components.SecondaryButton
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import com.gradtrack.app.ui.theme.Tone

class ProposalReviewViewModel(private val repository: ReviewRepository, private val id: String) : LoadableViewModel<ProposalDetail>({ repository.proposal(id) }) {
    fun review(decision: ReviewDecision, comment: String) =
        act({ repository.reviewProposal(id, decision, comment) }) {
            send(UiEvent.Message(R.string.decision_sent))
            refresh()
        }
}

@Composable
fun ProposalReviewScreen(proposalId: String) {
    val vm = appViewModel(key = "proposal-$proposalId") { ProposalReviewViewModel(it.reviews, proposalId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var comment by rememberSaveable { mutableStateOf("") }
    var commentError by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<ReviewDecision?>(null) }
    val feedbackRequired = stringResource(R.string.error_feedback_required)
    val files = LocalContainer.current.files
    val open = rememberFileOpener()
    EventsEffect(vm)

    pending?.let { decision ->
        val (title, message) =
            when (decision) {
                ReviewDecision.APPROVE -> R.string.approve_project to R.string.approve_confirm
                // Rejecting covers "needs changes" too: the team revises it or sends a new idea.
                ReviewDecision.CHANGES, ReviewDecision.REJECT -> R.string.reject_proposal to R.string.reject_confirm
            }
        ConfirmDialog(stringResource(title), stringResource(message), stringResource(title), { pending = null }, {
            pending = null
            vm.review(decision, comment.trim())
        }, danger = decision == ReviewDecision.REJECT)
    }

    fun decide(decision: ReviewDecision) {
        if (decision != ReviewDecision.APPROVE && comment.isBlank()) commentError = feedbackRequired else pending = decision
    }

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { proposal ->
        item {
            GtCard {
                Pill(proposal.status.pill())
                Text(proposal.title, style = GtType.Headline, modifier = Modifier.padding(top = 10.dp))
                Text(stringResource(R.string.review_meta, proposal.teamName, proposal.submittedAt.asDate()), style = GtType.Caption, modifier = Modifier.padding(top = 4.dp))
                Text(stringResource(R.string.review_intro), style = GtType.Body.copy(color = Gt.TextSecondary), modifier = Modifier.padding(top = 10.dp))
            }
        }
        item {
            PrimaryTabRow(
                selectedTabIndex = tab,
                containerColor = Gt.Background,
                contentColor = Gt.Primary,
                indicator = { TabRowDefaults.PrimaryIndicator(Modifier.tabIndicatorOffset(tab), color = Gt.Primary) },
            ) {
                listOf(R.string.tab_details, R.string.tab_team, R.string.tab_documents).forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = {
                        Text(stringResource(label), style = GtType.Label.copy(color = if (tab == index) Gt.Primary else Gt.TextSecondary))
                    })
                }
            }
        }
        item {
            when (tab) {
                0 -> DetailsTab(proposal)
                1 ->
                    GtCard(padding = 16.dp) {
                        SectionTitle(stringResource(R.string.tab_team))
                        proposal.members.forEach { MemberRow(it) }
                    }
                else ->
                    GtCard(padding = 16.dp) {
                        SectionTitle(stringResource(R.string.tab_documents))
                        if (proposal.documents.isEmpty()) MutedText("—", Modifier.padding(top = 8.dp))
                        proposal.documents.forEach { document ->
                            Row(Modifier.fillMaxWidth().clickable { open { files.proposal(proposalId, document.name) } }.padding(vertical = 10.dp)) {
                                IconTile(R.drawable.ic_file)
                                androidx.compose.foundation.layout.Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(document.name, style = GtType.Label)
                                    Text(document.sizeLabel ?: stringResource(R.string.tap_to_open), style = GtType.Caption)
                                }
                            }
                        }
                    }
            }
        }
        item {
            GtCard {
                SectionTitle(stringResource(R.string.review_decision), Modifier.padding(bottom = 8.dp))
                if (proposal.status != ProjectStatus.UNDER_REVIEW && proposal.status != ProjectStatus.SUBMITTED) {
                    MutedText(stringResource(R.string.decision_recorded))
                } else {
                    GtTextField(comment, {
                        comment = it
                        commentError = null
                    }, stringResource(R.string.field_feedback), error = commentError, helper = stringResource(R.string.feedback_helper), singleLine = false, minLines = 3)
                    PrimaryButton(stringResource(R.string.approve_project), { decide(ReviewDecision.APPROVE) }, Modifier.fillMaxWidth().padding(top = 12.dp), loading = busy)
                    SecondaryButton(stringResource(R.string.reject_proposal), { decide(ReviewDecision.REJECT) }, Modifier.fillMaxWidth().padding(top = 10.dp), enabled = !busy, danger = true)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsTab(proposal: ProposalDetail) {
    GtCard {
        LabeledValue(stringResource(R.string.category_label), proposal.category)
        LabeledValue(stringResource(R.string.target_users), proposal.targetUsers)
        LabeledValue(stringResource(R.string.estimated_duration), proposal.duration)
        SectionTitle(stringResource(R.string.description), Modifier.padding(top = 12.dp))
        Text(proposal.description, style = GtType.Body, modifier = Modifier.padding(top = 4.dp))
        if (proposal.objectives.isNotEmpty()) {
            SectionTitle(stringResource(R.string.field_objectives), Modifier.padding(top = 12.dp))
            proposal.objectives.forEach { Text("•  $it", style = GtType.Body, modifier = Modifier.padding(top = 4.dp)) }
        }
        if (proposal.technologies.isNotEmpty()) {
            SectionTitle(stringResource(R.string.technologies_tools), Modifier.padding(top = 12.dp))
            FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                proposal.technologies.forEach { Pill(it, Tone.Green) }
            }
        }
        proposal.notes?.takeIf { it.isNotBlank() }?.let {
            SectionTitle(stringResource(R.string.additional_notes), Modifier.padding(top = 12.dp))
            Text(it, style = GtType.Body, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

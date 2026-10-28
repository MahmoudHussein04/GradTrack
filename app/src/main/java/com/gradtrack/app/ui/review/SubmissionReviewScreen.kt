package com.gradtrack.app.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.model.ReviewDecision
import com.gradtrack.app.data.model.Submission
import com.gradtrack.app.data.model.SubmissionStatus
import com.gradtrack.app.data.model.SubmissionsOverview
import com.gradtrack.app.data.repository.ReviewRepository
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.common.asShortDate
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.common.rememberSubmissionOpener
import com.gradtrack.app.ui.components.EmptyState
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.GtTextField
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.PageHeader
import com.gradtrack.app.ui.components.Pill
import com.gradtrack.app.ui.components.PrimaryButton
import com.gradtrack.app.ui.components.SecondaryButton
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.components.StatCard
import com.gradtrack.app.ui.components.StatRow
import com.gradtrack.app.ui.components.SubmissionRow
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType

class SubmissionReviewViewModel(private val repository: ReviewRepository) : LoadableViewModel<SubmissionsOverview>({ repository.submissions() }) {
    fun review(submission: Submission, decision: ReviewDecision, comment: String) =
        act({ repository.reviewSubmission(submission.id, decision, comment) }) {
            send(UiEvent.Message(R.string.decision_sent))
            refresh()
        }
}

@Composable
fun SubmissionReviewScreen() {
    val vm = appViewModel { SubmissionReviewViewModel(it.reviews) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf<Submission?>(null) }
    val openFile = rememberSubmissionOpener()
    EventsEffect(vm)

    open?.let { submission ->
        SubmissionReviewDialog(submission, onOpen = { openFile(submission) }, onDismiss = { open = null }) { decision, comment ->
            open = null
            vm.review(submission, decision, comment)
        }
    }

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { data ->
        item { PageHeader(stringResource(R.string.title_submission_reviews), stringResource(R.string.crumb_submissions), stringResource(R.string.submission_review_subtitle)) }
        item {
            StatRow {
                StatCard(stringResource(R.string.stat_awaiting_review_short), data.awaitingReview.toString(), Modifier.weight(1f), icon = R.drawable.ic_file)
                StatCard(stringResource(R.string.stat_reviewed_week), data.reviewedThisWeek.toString(), Modifier.weight(1f), icon = R.drawable.ic_check)
                StatCard(stringResource(R.string.stat_need_revisions), data.needRevision.toString(), Modifier.weight(1f), icon = R.drawable.ic_flag)
            }
        }
        if (data.submissions.isEmpty()) {
            item { EmptyState(R.drawable.ic_file, stringResource(R.string.empty_submissions_title), stringResource(R.string.no_submissions_to_review)) }
        } else {
            item {
                GtCard(padding = 16.dp) {
                    SectionTitle(stringResource(R.string.recent_submissions))
                    data.submissions.forEach { submission -> SubmissionRow(submission, showTeam = true) { open = submission } }
                }
            }
        }
    }
}

@Composable
private fun SubmissionReviewDialog(submission: Submission, onOpen: () -> Unit, onDismiss: () -> Unit, onDecide: (ReviewDecision, String) -> Unit) {
    var comment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val required = stringResource(R.string.error_feedback_required)
    val reviewable = submission.status == SubmissionStatus.REVIEW
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(submission.fileName, style = GtType.Title) },
        text = {
            Column {
                Text(
                    stringResource(R.string.submission_meta, submission.fileName, submission.teamName ?: "—", submission.type ?: "—", submission.submittedAt?.asShortDate() ?: "—"),
                    style = GtType.Caption,
                )
                Pill(submission.status.pill(), Modifier.padding(top = 8.dp, bottom = 8.dp))
                SecondaryButton(stringResource(R.string.open_file), onOpen, Modifier.fillMaxWidth().padding(bottom = 8.dp), icon = R.drawable.ic_file)
                if (reviewable) {
                    GtTextField(comment, {
                        comment = it
                        error = null
                    }, stringResource(R.string.field_feedback), error = error, helper = stringResource(R.string.revision_helper), singleLine = false, minLines = 3)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(stringResource(R.string.request_revision), {
                            if (comment.isBlank()) error = required else onDecide(ReviewDecision.CHANGES, comment.trim())
                        }, Modifier.weight(1f))
                        PrimaryButton(stringResource(R.string.approve), { onDecide(ReviewDecision.APPROVE, comment.trim()) }, Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), style = GtType.Label.copy(color = Gt.TextSecondary)) } },
        containerColor = Gt.Surface,
    )
}

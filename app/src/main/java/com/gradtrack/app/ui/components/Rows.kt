package com.gradtrack.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Feedback
import com.gradtrack.app.data.model.Milestone
import com.gradtrack.app.data.model.MilestoneStatus
import com.gradtrack.app.data.model.StepState
import com.gradtrack.app.data.model.Submission
import com.gradtrack.app.data.model.TeamMember
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.asShortDate
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import com.gradtrack.app.ui.theme.Tone

@Composable
fun MilestoneTimeline(milestones: List<Milestone>, onEdit: ((Milestone) -> Unit)? = null) {
    milestones.forEachIndexed { index, milestone ->
        TimelineRow(
            title = milestone.name,
            subtitle = milestone.dueDate.asDate(),
            state =
                when (milestone.status) {
                    MilestoneStatus.COMPLETED -> StepState.DONE
                    MilestoneStatus.IN_PROGRESS -> StepState.CURRENT
                    MilestoneStatus.UPCOMING -> StepState.WAITING
                },
            isLast = index == milestones.lastIndex,
            pill = milestone.status.pill(),
            onEdit = if (onEdit != null && milestone.status != MilestoneStatus.COMPLETED) ({ onEdit(milestone) }) else null,
        )
    }
}

@Composable
fun FeedbackRow(feedback: Feedback) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Avatar(feedback.authorName, size = 36.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(feedback.authorName, style = GtType.Label)
            Text(feedback.createdAt.asDate(), style = GtType.Caption)
            Text(feedback.body, style = GtType.Body, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun SubmissionRow(
    submission: Submission,
    showTeam: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val meta =
        when {
            submission.submittedAt == null -> stringResource(R.string.not_submitted_yet)
            showTeam -> listOfNotNull(submission.teamName, submission.type, submission.submittedAt.asShortDate()).joinToString(" · ")
            else -> listOfNotNull(submission.submittedAt.asShortDate(), submission.reviewerName).joinToString(" · ")
        }
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(R.drawable.ic_file)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(submission.fileName, style = GtType.Label)
            Text(meta, style = GtType.Caption)
        }
        Pill(submission.status.pill(), Modifier.padding(start = 8.dp))
    }
}

@Composable
fun MemberRow(member: TeamMember, onRemove: ((TeamMember) -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(member.fullName, member.avatarUrl)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(member.fullName, style = GtType.Label)
            Text(listOfNotNull(member.universityId, member.roleLabel).joinToString(" · "), style = GtType.Caption)
        }
        Pill(member.status, if (member.status.equals("Active", ignoreCase = true)) Tone.Green else Tone.Gray, Modifier.padding(start = 8.dp))
        if (onRemove != null && !member.isLeader) {
            IconButton(onClick = { onRemove(member) }) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.remove_member), tint = Gt.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

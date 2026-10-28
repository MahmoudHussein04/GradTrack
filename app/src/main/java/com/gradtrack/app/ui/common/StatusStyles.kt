package com.gradtrack.app.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gradtrack.app.R
import com.gradtrack.app.data.model.MilestoneStatus
import com.gradtrack.app.data.model.NotificationType
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.SubmissionStatus
import com.gradtrack.app.data.model.TaskStatus
import com.gradtrack.app.ui.theme.Tone

class PillStyle(@StringRes val label: Int, val tone: Tone)

fun ProjectStatus.pill(): PillStyle =
    when (this) {
        ProjectStatus.DRAFT -> PillStyle(R.string.status_draft, Tone.Gray)
        ProjectStatus.SUBMITTED -> PillStyle(R.string.status_submitted, Tone.Blue)
        ProjectStatus.UNDER_REVIEW -> PillStyle(R.string.status_under_review, Tone.Yellow)
        ProjectStatus.CHANGES_REQUESTED -> PillStyle(R.string.status_changes_requested, Tone.Orange)
        ProjectStatus.APPROVED -> PillStyle(R.string.status_approved, Tone.Green)
        ProjectStatus.IN_PROGRESS -> PillStyle(R.string.status_in_progress, Tone.Green)
        ProjectStatus.REJECTED -> PillStyle(R.string.status_rejected, Tone.Red)
        ProjectStatus.COMPLETED -> PillStyle(R.string.status_completed, Tone.Green)
    }

fun MilestoneStatus.pill(): PillStyle =
    when (this) {
        MilestoneStatus.COMPLETED -> PillStyle(R.string.status_completed, Tone.Green)
        MilestoneStatus.IN_PROGRESS -> PillStyle(R.string.status_in_progress, Tone.Yellow)
        MilestoneStatus.UPCOMING -> PillStyle(R.string.status_upcoming, Tone.Gray)
    }

fun SubmissionStatus.pill(): PillStyle =
    when (this) {
        SubmissionStatus.APPROVED -> PillStyle(R.string.status_approved, Tone.Green)
        SubmissionStatus.REVIEW -> PillStyle(R.string.status_in_review, Tone.Yellow)
        SubmissionStatus.NEEDS_REVISION -> PillStyle(R.string.status_needs_revision, Tone.Orange)
        SubmissionStatus.NOT_SUBMITTED -> PillStyle(R.string.status_not_submitted, Tone.Gray)
    }

fun TaskStatus.pill(): PillStyle =
    when (this) {
        TaskStatus.TODO -> PillStyle(R.string.task_todo, Tone.Gray)
        TaskStatus.IN_PROGRESS -> PillStyle(R.string.task_in_progress, Tone.Yellow)
        TaskStatus.DONE -> PillStyle(R.string.task_done, Tone.Green)
        TaskStatus.CANCELLED -> PillStyle(R.string.task_cancelled, Tone.Red)
    }

fun NotificationType.pill(): PillStyle =
    when (this) {
        NotificationType.PROPOSAL -> PillStyle(R.string.type_proposal, Tone.Green)
        NotificationType.FEEDBACK -> PillStyle(R.string.type_feedback, Tone.Blue)
        NotificationType.DEADLINE -> PillStyle(R.string.type_deadline, Tone.Orange)
        NotificationType.TEAM -> PillStyle(R.string.type_team, Tone.Gray)
        NotificationType.TASK -> PillStyle(R.string.type_task, Tone.Green)
        NotificationType.SUBMISSION -> PillStyle(R.string.type_submission, Tone.Yellow)
    }

@DrawableRes
fun NotificationType.icon(): Int =
    when (this) {
        NotificationType.PROPOSAL -> R.drawable.ic_proposals
        NotificationType.FEEDBACK -> R.drawable.ic_feedback
        NotificationType.DEADLINE -> R.drawable.ic_clock
        NotificationType.TEAM -> R.drawable.ic_team
        NotificationType.TASK -> R.drawable.ic_check_circle
        NotificationType.SUBMISSION -> R.drawable.ic_submissions
    }

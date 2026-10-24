package com.gradtrack.app.ui.common

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import com.gradtrack.app.R
import com.gradtrack.app.data.model.MilestoneStatus
import com.gradtrack.app.data.model.NotificationType
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.SubmissionStatus
import com.gradtrack.app.data.model.TaskStatus

/** Label plus colours of a status pill (the rounded chips next to titles in the design). */
class PillStyle(@StringRes val label: Int, @ColorRes val foreground: Int, @ColorRes val background: Int)

private fun green(@StringRes label: Int) = PillStyle(label, R.color.pill_green_fg, R.color.pill_green_bg)

private fun yellow(@StringRes label: Int) = PillStyle(label, R.color.pill_yellow_fg, R.color.pill_yellow_bg)

private fun orange(@StringRes label: Int) = PillStyle(label, R.color.pill_orange_fg, R.color.pill_orange_bg)

private fun red(@StringRes label: Int) = PillStyle(label, R.color.pill_red_fg, R.color.pill_red_bg)

private fun gray(@StringRes label: Int) = PillStyle(label, R.color.pill_gray_fg, R.color.pill_gray_bg)

private fun blue(@StringRes label: Int) = PillStyle(label, R.color.pill_blue_fg, R.color.pill_blue_bg)

fun ProjectStatus.pill(): PillStyle =
    when (this) {
        ProjectStatus.DRAFT -> gray(R.string.status_draft)
        ProjectStatus.SUBMITTED -> blue(R.string.status_submitted)
        ProjectStatus.UNDER_REVIEW -> yellow(R.string.status_under_review)
        ProjectStatus.CHANGES_REQUESTED -> orange(R.string.status_changes_requested)
        ProjectStatus.APPROVED -> green(R.string.status_approved)
        ProjectStatus.IN_PROGRESS -> green(R.string.status_in_progress)
        ProjectStatus.REJECTED -> red(R.string.status_rejected)
        ProjectStatus.COMPLETED -> green(R.string.status_completed)
    }

fun MilestoneStatus.pill(): PillStyle =
    when (this) {
        MilestoneStatus.COMPLETED -> green(R.string.status_completed)
        MilestoneStatus.IN_PROGRESS -> yellow(R.string.status_in_progress)
        MilestoneStatus.UPCOMING -> gray(R.string.status_upcoming)
    }

fun SubmissionStatus.pill(): PillStyle =
    when (this) {
        SubmissionStatus.APPROVED -> green(R.string.status_approved)
        SubmissionStatus.REVIEW -> yellow(R.string.status_in_review)
        SubmissionStatus.NEEDS_REVISION -> orange(R.string.status_needs_revision)
        SubmissionStatus.NOT_SUBMITTED -> gray(R.string.status_not_submitted)
    }

fun TaskStatus.pill(): PillStyle =
    when (this) {
        TaskStatus.TODO -> gray(R.string.task_todo)
        TaskStatus.IN_PROGRESS -> yellow(R.string.task_in_progress)
        TaskStatus.DONE -> green(R.string.task_done)
        TaskStatus.CANCELLED -> red(R.string.task_cancelled)
    }

fun NotificationType.pill(): PillStyle =
    when (this) {
        NotificationType.PROPOSAL -> green(R.string.type_proposal)
        NotificationType.FEEDBACK -> blue(R.string.type_feedback)
        NotificationType.DEADLINE -> orange(R.string.type_deadline)
        NotificationType.TEAM -> gray(R.string.type_team)
        NotificationType.TASK -> green(R.string.type_task)
        NotificationType.SUBMISSION -> yellow(R.string.type_submission)
    }

fun NotificationType.icon(): Int =
    when (this) {
        NotificationType.PROPOSAL -> R.drawable.ic_proposals
        NotificationType.FEEDBACK -> R.drawable.ic_feedback
        NotificationType.DEADLINE -> R.drawable.ic_clock
        NotificationType.TEAM -> R.drawable.ic_team
        NotificationType.TASK -> R.drawable.ic_check_circle
        NotificationType.SUBMISSION -> R.drawable.ic_submissions
    }

fun TextView.applyPill(style: PillStyle) {
    if (style.label != 0) setText(style.label)
    setTextColor(ContextCompat.getColor(context, style.foreground))
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, style.background))
}

/** DiffUtil for lists of data classes with a stable id. */
class IdDiff<T : Any>(private val id: (T) -> Any) : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) = id(oldItem) == id(newItem)

    @android.annotation.SuppressLint("DiffUtilEquals") // every list item is a data class
    override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
}

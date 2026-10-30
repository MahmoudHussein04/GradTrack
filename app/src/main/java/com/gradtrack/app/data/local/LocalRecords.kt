package com.gradtrack.app.data.local

import com.gradtrack.app.data.model.MilestoneStatus
import com.gradtrack.app.data.model.NotificationType
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.SubmissionStatus
import com.gradtrack.app.data.model.TaskStatus
import com.gradtrack.app.data.model.User

data class AccountRecord(
    var user: User? = null,
    var salt: String = "",
    var passwordHash: String = "",
)

data class TeamRecord(
    var id: String = "",
    var name: String = "",
    var leaderId: String = "",
    var maxMembers: Int = 4,
    val memberIds: MutableList<String> = mutableListOf(),
    var createdAt: String = "",
    var supervisorId: String = "",
)

data class InvitationRecord(
    var id: String = "",
    var teamId: String = "",
    var userId: String = "",
    var status: String = STATUS_PENDING,
    var createdAt: String = "",
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_DECLINED = "declined"
    }
}

data class ProjectRecord(
    var id: String = "",
    var teamId: String = "",
    var supervisorId: String = "",
    var title: String = "",
    var category: String = "",
    var problemStatement: String = "",
    var objectives: String = "",
    var technologies: MutableList<String> = mutableListOf(),
    var targetUsers: String = "",
    var durationMonths: Int = 0,
    var proposalFileName: String? = null,
    var status: ProjectStatus = ProjectStatus.UNDER_REVIEW,
    var submittedAt: String = "",
    var updatedAt: String = "",
)

data class MilestoneRecord(
    var id: String = "",
    var projectId: String = "",
    var name: String = "",
    var dueDate: String = "",
    var status: MilestoneStatus = MilestoneStatus.UPCOMING,
    var order: Int = 0,
)

data class SubmissionRecord(
    var id: String = "",
    var projectId: String = "",
    var milestoneId: String = "",
    var fileName: String = "",
    var type: String = "",
    var submittedAt: String = "",
    var submittedBy: String = "",
    var status: SubmissionStatus = SubmissionStatus.REVIEW,
    var reviewedAt: String? = null,
)

data class FeedbackRecord(
    var id: String = "",
    var projectId: String = "",
    var authorId: String = "",
    var body: String = "",
    var createdAt: String = "",
)

data class TaskRecord(
    var id: String = "",
    var teamId: String = "",
    var title: String = "",
    var description: String? = null,
    var assigneeId: String = "",
    var status: TaskStatus = TaskStatus.TODO,
    var dueDate: String? = null,
    var completedAt: String? = null,
    var attachmentName: String? = null,
    var solutionNote: String? = null,
    var solutionFileName: String? = null,
)

data class NotificationRecord(
    var id: String = "",
    var userId: String = "",
    var type: NotificationType = NotificationType.TEAM,
    var title: String = "",
    var body: String = "",
    var createdAt: String = "",
    var read: Boolean = false,
)

data class LocalDb(
    var nextId: Long = 1,
    val accounts: MutableList<AccountRecord> = mutableListOf(),
    val teams: MutableList<TeamRecord> = mutableListOf(),
    val invitations: MutableList<InvitationRecord> = mutableListOf(),
    val projects: MutableList<ProjectRecord> = mutableListOf(),
    val milestones: MutableList<MilestoneRecord> = mutableListOf(),
    val submissions: MutableList<SubmissionRecord> = mutableListOf(),
    val feedback: MutableList<FeedbackRecord> = mutableListOf(),
    val tasks: MutableList<TaskRecord> = mutableListOf(),
    val notifications: MutableList<NotificationRecord> = mutableListOf(),
)

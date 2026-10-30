package com.gradtrack.app.data.model

import com.google.gson.annotations.SerializedName

enum class Role {
    @SerializedName("student") STUDENT,
    @SerializedName("leader") LEADER,
    @SerializedName("supervisor") SUPERVISOR,
}

enum class ProjectStatus {
    @SerializedName("draft") DRAFT,
    @SerializedName("submitted") SUBMITTED,
    @SerializedName("under_review") UNDER_REVIEW,
    @SerializedName("changes_requested") CHANGES_REQUESTED,
    @SerializedName("approved") APPROVED,
    @SerializedName("in_progress") IN_PROGRESS,
    @SerializedName("rejected") REJECTED,
    @SerializedName("completed") COMPLETED,
}

enum class MilestoneStatus {
    @SerializedName("upcoming") UPCOMING,
    @SerializedName("in_progress") IN_PROGRESS,
    @SerializedName("completed") COMPLETED,
}

enum class SubmissionStatus {
    @SerializedName("not_submitted") NOT_SUBMITTED,
    @SerializedName("review") REVIEW,
    @SerializedName("needs_revision") NEEDS_REVISION,
    @SerializedName("approved") APPROVED,
}

enum class TaskStatus {
    @SerializedName("todo") TODO,
    @SerializedName("in_progress") IN_PROGRESS,
    @SerializedName("done") DONE,
    @SerializedName("cancelled") CANCELLED,
}

enum class NotificationType {
    @SerializedName("proposal") PROPOSAL,
    @SerializedName("feedback") FEEDBACK,
    @SerializedName("deadline") DEADLINE,
    @SerializedName("team") TEAM,
    @SerializedName("task") TASK,
    @SerializedName("submission") SUBMISSION,
}

enum class ReviewDecision {
    @SerializedName("approve") APPROVE,
    @SerializedName("changes") CHANGES,
    @SerializedName("reject") REJECT,
}

enum class StepState {
    @SerializedName("done") DONE,
    @SerializedName("current") CURRENT,
    @SerializedName("waiting") WAITING,
}

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val universityId: String?,
    val department: String?,
    val academicYear: String?,
    val role: Role,
    val avatarUrl: String? = null,
)

data class LoginRequest(val identifier: String, val password: String, val role: Role)

data class AuthResponse(val token: String, val user: User)

data class UpdateProfileRequest(
    val fullName: String,
    val email: String,
    val department: String,
    val academicYear: String,
)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

data class Deadline(val id: String, val title: String, val date: String)

data class ActivityItem(val id: String, val title: String, val body: String, val createdAt: String)

data class StudentDashboard(
    val progress: Int,
    val upcomingDeadlines: Int,
    val supervisorFeedback: Int,
    val teamMembers: Int,
    val project: Project?,
    val deadlines: List<Deadline>,
    val activity: List<ActivityItem>,
)

data class SupervisorDashboard(
    val activeProjects: Int,
    val pendingProposals: Int,
    val submissionsToReview: Int,
    val upcomingDeadlines: Int,
    val proposals: List<ProposalSummary>,
    val deadlines: List<Deadline>,
    val activity: List<ActivityItem>,
)

data class Project(
    val id: String,
    val title: String,
    val category: String,
    val department: String?,
    val academicYear: String?,
    val status: ProjectStatus,
    val progress: Int,
    val completedMilestones: Int,
    val totalMilestones: Int,
    val supervisorId: String? = null,
    val supervisorName: String? = null,
    val problemStatement: String? = null,
    val objectives: String? = null,
    val technologies: List<String> = emptyList(),
    val targetUsers: String? = null,
    val durationMonths: Int? = null,
    val proposalFileName: String? = null,
)

data class Milestone(val id: String, val name: String, val dueDate: String, val status: MilestoneStatus)

data class Feedback(val id: String, val authorName: String, val createdAt: String, val body: String)

data class Submission(
    val id: String,
    val fileName: String,
    val teamName: String?,
    val type: String?,
    val submittedAt: String?,
    val reviewerName: String?,
    val status: SubmissionStatus,
    val milestoneId: String? = null,
)

data class ProjectOverview(
    val project: Project?,
    val milestones: List<Milestone>,
    val feedback: List<Feedback>,
    val latestSubmission: Submission?,
)

data class TimelineStep(val label: String, val state: StepState)

data class ProposalStatusInfo(
    val projectId: String,
    val title: String,
    val status: ProjectStatus,
    val submittedAt: String?,
    val updatedAt: String?,
    val steps: List<TimelineStep>,
    val feedback: List<Feedback>,
    val documentName: String?,
)

data class RegisterProjectRequest(
    val title: String,
    val category: String,
    val problemStatement: String,
    val objectives: String,
    val technologies: List<String>,
    val proposalFileName: String?,
    val targetUsers: String,
    val durationMonths: Int,
)

data class ProposalSummary(
    val id: String,
    val title: String,
    val teamName: String,
    val submittedAt: String,
    val status: ProjectStatus,
)

data class DocumentInfo(val name: String, val sizeLabel: String?)

data class ProposalDetail(
    val id: String,
    val title: String,
    val category: String,
    val teamName: String,
    val submittedAt: String,
    val status: ProjectStatus,
    val targetUsers: String,
    val duration: String,
    val description: String,
    val objectives: List<String>,
    val technologies: List<String>,
    val notes: String?,
    val members: List<TeamMember>,
    val documents: List<DocumentInfo>,
)

data class ReviewRequest(val decision: ReviewDecision, val comment: String?)

data class SubmissionsOverview(
    val awaitingReview: Int,
    val reviewedThisWeek: Int,
    val needRevision: Int,
    val submissions: List<Submission>,
)

data class TeamMember(
    val id: String,
    val fullName: String,
    val universityId: String?,
    val roleLabel: String,
    val isLeader: Boolean,
    val status: String,
    val avatarUrl: String? = null,
)

data class TeamInfo(
    val id: String,
    val name: String,
    val leaderId: String,
    val members: List<TeamMember>,
    val maxMembers: Int,
    val pendingInvites: Int,
    val projectTitle: String?,
    val supervisorName: String?,
    val semester: String?,
)

data class InviteRequest(val universityId: String)

data class CreateTeamRequest(val name: String)

data class Invitation(val id: String, val teamName: String, val leaderName: String, val createdAt: String)

data class MyTeam(val team: TeamInfo?, val invitations: List<Invitation>)

data class TaskItem(
    val id: String,
    val title: String,
    val description: String?,
    val assigneeId: String,
    val assigneeName: String,
    val status: TaskStatus,
    val dueDate: String?,
    val completedAt: String?,
    val attachmentName: String? = null,
    val solutionNote: String? = null,
    val solutionFileName: String? = null,
)

data class TaskRequest(
    val title: String,
    val description: String?,
    val assigneeId: String,
    val dueDate: String?,
)

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAt: String,
    val read: Boolean,
)

data class FeedbackRequest(val body: String)

data class MilestoneRequest(val name: String, val dueDate: String)

data class ApiErrorBody(val message: String?)

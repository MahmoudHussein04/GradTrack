package com.gradtrack.app.data.repository

import com.gradtrack.app.data.Resource
import com.gradtrack.app.data.SessionStore
import com.gradtrack.app.data.model.AuthResponse
import com.gradtrack.app.data.model.ChangePasswordRequest
import com.gradtrack.app.data.model.CreateTeamRequest
import com.gradtrack.app.data.model.Feedback
import com.gradtrack.app.data.model.FeedbackRequest
import com.gradtrack.app.data.model.InviteRequest
import com.gradtrack.app.data.model.LoginRequest
import com.gradtrack.app.data.model.Milestone
import com.gradtrack.app.data.model.MyTeam
import com.gradtrack.app.data.model.NotificationItem
import com.gradtrack.app.data.model.Project
import com.gradtrack.app.data.model.ProjectOverview
import com.gradtrack.app.data.model.ProposalDetail
import com.gradtrack.app.data.model.ProposalStatusInfo
import com.gradtrack.app.data.model.ProposalSummary
import com.gradtrack.app.data.model.RegisterProjectRequest
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.ReviewDecision
import com.gradtrack.app.data.model.ReviewRequest
import com.gradtrack.app.data.model.StudentDashboard
import com.gradtrack.app.data.model.Submission
import com.gradtrack.app.data.model.SubmissionsOverview
import com.gradtrack.app.data.model.SupervisorDashboard
import com.gradtrack.app.data.model.SupervisorOption
import com.gradtrack.app.data.model.TaskItem
import com.gradtrack.app.data.model.TaskRequest
import com.gradtrack.app.data.model.TaskStatus
import com.gradtrack.app.data.model.TaskStatusRequest
import com.gradtrack.app.data.model.TeamInfo
import com.gradtrack.app.data.model.UpdateProfileRequest
import com.gradtrack.app.data.model.User
import com.gradtrack.app.data.remote.ApiService
import com.gradtrack.app.data.safeCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** A file chosen by the user, already read into memory (deliverables are capped at 25 MB by the UI). */
class UploadFile(val name: String, val mimeType: String?, val bytes: ByteArray)

class AuthRepository(private val api: ApiService, private val session: SessionStore) {
    val currentUser: User? get() = session.user

    suspend fun login(identifier: String, password: String, role: Role): Resource<AuthResponse> {
        val result = safeCall { api.login(LoginRequest(identifier.trim(), password, role)) }
        if (result is Resource.Success) session.save(result.data.token, result.data.user)
        return result
    }

    suspend fun logout() {
        safeCall { api.logout() }
        session.clear()
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Resource<User> {
        val result = safeCall { api.updateProfile(request) }
        if (result is Resource.Success) session.user = result.data
        return result
    }

    suspend fun changePassword(current: String, new: String): Resource<Unit> = safeCall { api.changePassword(ChangePasswordRequest(current, new)) }
}

class DashboardRepository(private val api: ApiService) {
    suspend fun student(): Resource<StudentDashboard> = safeCall { api.studentDashboard() }

    suspend fun supervisor(): Resource<SupervisorDashboard> = safeCall { api.supervisorDashboard() }
}

class ProjectRepository(private val api: ApiService) {
    suspend fun overview(): Resource<ProjectOverview> = safeCall { api.currentProject() }

    suspend fun supervisors(): Resource<List<SupervisorOption>> = safeCall { api.supervisors() }

    suspend fun register(request: RegisterProjectRequest): Resource<Project> = safeCall { api.registerProject(request) }

    suspend fun proposalStatus(): Resource<ProposalStatusInfo> = safeCall { api.proposalStatus() }

    suspend fun reply(body: String): Resource<Feedback> = safeCall { api.replyToFeedback(FeedbackRequest(body.trim())) }

    suspend fun milestones(): Resource<List<Milestone>> = safeCall { api.milestones() }

    suspend fun submissions(): Resource<List<Submission>> = safeCall { api.submissions() }

    suspend fun upload(milestoneId: String, file: UploadFile): Resource<Submission> =
        safeCall {
            val body = file.bytes.toRequestBody((file.mimeType ?: "application/octet-stream").toMediaTypeOrNull())
            api.uploadDeliverable(milestoneId, MultipartBody.Part.createFormData("file", file.name, body))
        }
}

class TeamRepository(private val api: ApiService) {
    suspend fun myTeam(): Resource<MyTeam> = safeCall { api.myTeam() }

    suspend fun create(name: String): Resource<TeamInfo> = safeCall { api.createTeam(CreateTeamRequest(name.trim())) }

    suspend fun accept(invitationId: String): Resource<Unit> = safeCall { api.acceptInvitation(invitationId) }

    suspend fun decline(invitationId: String): Resource<Unit> = safeCall { api.declineInvitation(invitationId) }

    suspend fun invite(universityId: String): Resource<Unit> = safeCall { api.inviteMember(InviteRequest(universityId.trim())) }

    suspend fun remove(userId: String): Resource<Unit> = safeCall { api.removeMember(userId) }
}

class TaskRepository(private val api: ApiService) {
    suspend fun tasks(): Resource<List<TaskItem>> = safeCall { api.tasks() }

    suspend fun create(request: TaskRequest): Resource<TaskItem> = safeCall { api.createTask(request) }

    suspend fun update(id: String, request: TaskRequest): Resource<TaskItem> = safeCall { api.updateTask(id, request) }

    suspend fun setStatus(id: String, status: TaskStatus): Resource<TaskItem> = safeCall { api.updateTaskStatus(id, TaskStatusRequest(status)) }

    suspend fun cancel(id: String): Resource<TaskItem> = safeCall { api.cancelTask(id) }
}

class NotificationRepository(private val api: ApiService) {
    suspend fun list(): Resource<List<NotificationItem>> = safeCall { api.notifications() }

    suspend fun markAllRead(): Resource<Unit> = safeCall { api.markAllNotificationsRead() }
}

class ReviewRepository(private val api: ApiService) {
    suspend fun proposals(): Resource<List<ProposalSummary>> = safeCall { api.proposals() }

    suspend fun proposal(id: String): Resource<ProposalDetail> = safeCall { api.proposalDetail(id) }

    suspend fun reviewProposal(id: String, decision: ReviewDecision, comment: String): Resource<Unit> =
        safeCall { api.reviewProposal(id, ReviewRequest(decision, comment.trim().ifBlank { null })) }

    suspend fun submissions(): Resource<SubmissionsOverview> = safeCall { api.supervisorSubmissions() }

    suspend fun reviewSubmission(id: String, decision: ReviewDecision, comment: String): Resource<Submission> =
        safeCall { api.reviewSubmission(id, ReviewRequest(decision, comment.trim().ifBlank { null })) }
}

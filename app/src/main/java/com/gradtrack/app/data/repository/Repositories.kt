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
import com.gradtrack.app.data.model.MilestoneRequest
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
import com.gradtrack.app.data.model.TaskItem
import com.gradtrack.app.data.model.TaskRequest
import com.gradtrack.app.data.model.TeamInfo
import com.gradtrack.app.data.model.UpdateProfileRequest
import com.gradtrack.app.data.model.User
import com.gradtrack.app.data.remote.ApiService
import com.gradtrack.app.data.safeCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UploadFile(val name: String, val mimeType: String?, val bytes: ByteArray)

private fun UploadFile.part() = MultipartBody.Part.createFormData("file", name, bytes.toRequestBody((mimeType ?: "application/octet-stream").toMediaTypeOrNull()))

class AuthRepository(private val api: ApiService, private val session: SessionStore) {
    val currentUser: User? get() = session.user

    suspend fun login(identifier: String, password: String, role: Role): Resource<AuthResponse> {
        val result = safeCall { api.login(LoginRequest(identifier.trim(), password, role)) }
        if (result is Resource.Success) session.save(result.data.token, result.data.user)
        return result
    }

    /** Re-reads the signed-in user, e.g. after creating a team turns a student into its leader. */
    suspend fun refreshUser(): Resource<User> {
        val result = safeCall { api.me() }
        if (result is Resource.Success) session.user = result.data
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

    suspend fun supervisor(): Resource<SupervisorDashboard> = safeCall { api. supervisorDashboard() }
}

class ProjectRepository(private val api: ApiService) {
    suspend fun overview(): Resource<ProjectOverview> = safeCall { api.currentProject() }

    suspend fun register(request: RegisterProjectRequest): Resource<Project> = safeCall { api.registerProject(request) }

    suspend fun uploadProposal(file: UploadFile): Resource<Project> = safeCall { api.uploadProposalFile(file.part()) }

    suspend fun proposalStatus(): Resource<ProposalStatusInfo> = safeCall { api.proposalStatus() }

    suspend fun reply(body: String): Resource<Feedback> = safeCall { api.replyToFeedback(FeedbackRequest(body.trim())) }

    suspend fun submissions(): Resource<List<Submission>> = safeCall { api.submissions() }

    suspend fun addMilestone(name: String, dueDate: String): Resource<Milestone> = safeCall { api.createMilestone(MilestoneRequest(name.trim(), dueDate)) }

    suspend fun updateMilestone(id: String, name: String, dueDate: String): Resource<Milestone> = safeCall { api.updateMilestone(id, MilestoneRequest(name.trim(), dueDate)) }

    suspend fun deleteMilestone(id: String): Resource<Unit> = safeCall { api.deleteMilestone(id) }

    suspend fun upload(milestoneId: String, file: UploadFile): Resource<Submission> =
        safeCall {
            api.uploadDeliverable(milestoneId, file.part())
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

    suspend fun task(id: String): Resource<TaskItem> = safeCall { api.task(id) }

    suspend fun start(id: String): Resource<TaskItem> = safeCall { api.startTask(id) }

    suspend fun attach(id: String, file: UploadFile): Resource<TaskItem> = safeCall { api.uploadTaskAttachment(id, file.part()) }

    suspend fun submit(id: String, note: String, file: UploadFile?): Resource<TaskItem> =
        safeCall { api.submitTaskSolution(id, note.trim().ifEmpty { null }?.toRequestBody("text/plain".toMediaTypeOrNull()), file?.part()) }

    suspend fun delete(id: String): Resource<Unit> = safeCall { api.deleteTask(id) }
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

class FileRepository(private val api: ApiService, private val cacheDir: File) {
    suspend fun submissionFile(submission: Submission): Resource<File> = download(submission.id, submission.fileName) { api.submissionFile(submission.id) }

    suspend fun taskAttachment(task: TaskItem): Resource<File> = download("${task.id}-brief", task.attachmentName.orEmpty()) { api.taskAttachment(task.id) }

    suspend fun taskSolution(task: TaskItem): Resource<File> = download("${task.id}-solution", task.solutionFileName.orEmpty()) { api.taskSolutionFile(task.id) }

    suspend fun myProposal(name: String): Resource<File> = download("proposal-mine", name) { api.myProposalFile() }

    suspend fun proposal(projectId: String, name: String): Resource<File> = download("proposal-$projectId", name) { api.proposalFile(projectId) }

    private suspend fun download(key: String, name: String, fetch: suspend () -> ResponseBody): Resource<File> =
        safeCall {
            val body = fetch()
            withContext(Dispatchers.IO) {
                val dir = File(cacheDir, key).apply { mkdirs() }
                File(dir, name.substringAfterLast('/').ifBlank { "file" }).apply { body.byteStream().use { input -> outputStream().use { input.copyTo(it) } } }
            }
        }
}

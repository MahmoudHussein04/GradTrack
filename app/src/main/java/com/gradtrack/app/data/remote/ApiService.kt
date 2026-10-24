package com.gradtrack.app.data.remote

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
import com.gradtrack.app.data.model.ReviewRequest
import com.gradtrack.app.data.model.StudentDashboard
import com.gradtrack.app.data.model.Submission
import com.gradtrack.app.data.model.SubmissionsOverview
import com.gradtrack.app.data.model.SupervisorDashboard
import com.gradtrack.app.data.model.SupervisorOption
import com.gradtrack.app.data.model.TaskItem
import com.gradtrack.app.data.model.TaskRequest
import com.gradtrack.app.data.model.TaskStatusRequest
import com.gradtrack.app.data.model.TeamInfo
import com.gradtrack.app.data.model.UpdateProfileRequest
import com.gradtrack.app.data.model.User
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** The REST contract (docs/gradtrack §7). Implemented by Retrofit in production and by [LocalApiService] while the backend is not built. */
interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout()

    @PATCH("me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): User

    @PATCH("me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest)

    @GET("dashboard/student")
    suspend fun studentDashboard(): StudentDashboard

    @GET("dashboard/supervisor")
    suspend fun supervisorDashboard(): SupervisorDashboard

    @GET("projects/current")
    suspend fun currentProject(): ProjectOverview

    @GET("supervisors")
    suspend fun supervisors(): List<SupervisorOption>

    @POST("projects")
    suspend fun registerProject(@Body request: RegisterProjectRequest): Project

    @GET("projects/current/proposal")
    suspend fun proposalStatus(): ProposalStatusInfo

    @POST("projects/current/feedback")
    suspend fun replyToFeedback(@Body request: FeedbackRequest): Feedback

    @GET("projects/current/milestones")
    suspend fun milestones(): List<Milestone>

    @GET("projects/current/submissions")
    suspend fun submissions(): List<Submission>

    @Multipart
    @POST("milestones/{id}/submissions")
    suspend fun uploadDeliverable(@Path("id") milestoneId: String, @Part file: MultipartBody.Part): Submission

    @GET("teams/me")
    suspend fun myTeam(): MyTeam

    @POST("teams")
    suspend fun createTeam(@Body request: CreateTeamRequest): TeamInfo

    @POST("invitations/{id}/accept")
    suspend fun acceptInvitation(@Path("id") id: String)

    @POST("invitations/{id}/decline")
    suspend fun declineInvitation(@Path("id") id: String)

    @POST("teams/me/invitations")
    suspend fun inviteMember(@Body request: InviteRequest)

    @DELETE("teams/me/members/{userId}")
    suspend fun removeMember(@Path("userId") userId: String)

    @GET("tasks")
    suspend fun tasks(): List<TaskItem>

    @POST("tasks")
    suspend fun createTask(@Body request: TaskRequest): TaskItem

    @PATCH("tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body request: TaskRequest): TaskItem

    @PATCH("tasks/{id}/status")
    suspend fun updateTaskStatus(@Path("id") id: String, @Body request: TaskStatusRequest): TaskItem

    @POST("tasks/{id}/cancel")
    suspend fun cancelTask(@Path("id") id: String): TaskItem

    @GET("notifications")
    suspend fun notifications(): List<NotificationItem>

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead()

    @GET("supervisor/proposals")
    suspend fun proposals(): List<ProposalSummary>

    @GET("proposals/{id}")
    suspend fun proposalDetail(@Path("id") id: String): ProposalDetail

    @POST("proposals/{id}/review")
    suspend fun reviewProposal(@Path("id") id: String, @Body request: ReviewRequest)

    @GET("supervisor/submissions")
    suspend fun supervisorSubmissions(): SubmissionsOverview

    @POST("submissions/{id}/review")
    suspend fun reviewSubmission(@Path("id") id: String, @Body request: ReviewRequest): Submission
}

package com.gradtrack.app.data.remote

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
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout()

    @GET("me")
    suspend fun me(): User

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

    @POST("projects")
    suspend fun registerProject(@Body request: RegisterProjectRequest): Project

    @GET("projects/current/proposal")
    suspend fun proposalStatus(): ProposalStatusInfo

    @Multipart
    @POST("projects/current/proposal/file")
    suspend fun uploadProposalFile(@Part file: MultipartBody.Part): Project

    @Streaming
    @GET("projects/current/proposal/file")
    suspend fun myProposalFile(): ResponseBody

    @Streaming
    @GET("proposals/{id}/file")
    suspend fun proposalFile(@Path("id") id: String): ResponseBody

    @POST("projects/current/feedback")
    suspend fun replyToFeedback(@Body request: FeedbackRequest): Feedback


    @GET("projects/current/submissions")
    suspend fun submissions(): List<Submission>

    @Multipart
    @POST("milestones/{id}/submissions")
    suspend fun uploadDeliverable(@Path("id") milestoneId: String, @Part file: MultipartBody.Part): Submission

    @POST("projects/current/milestones")
    suspend fun createMilestone(@Body request: MilestoneRequest): Milestone

    @PATCH("milestones/{id}")
    suspend fun updateMilestone(@Path("id") id: String, @Body request: MilestoneRequest): Milestone

    @DELETE("milestones/{id}")
    suspend fun deleteMilestone(@Path("id") id: String)

    @Streaming
    @GET("submissions/{id}/file")
    suspend fun submissionFile(@Path("id") submissionId: String): ResponseBody

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

    @GET("tasks/{id}")
    suspend fun task(@Path("id") id: String): TaskItem

    @POST("tasks/{id}/start")
    suspend fun startTask(@Path("id") id: String): TaskItem

    @Multipart
    @POST("tasks/{id}/attachment")
    suspend fun uploadTaskAttachment(@Path("id") id: String, @Part file: MultipartBody.Part): TaskItem

    @Streaming
    @GET("tasks/{id}/attachment")
    suspend fun taskAttachment(@Path("id") id: String): ResponseBody

    @Multipart
    @POST("tasks/{id}/solution")
    suspend fun submitTaskSolution(@Path("id") id: String, @Part("note") note: RequestBody?, @Part file: MultipartBody.Part?): TaskItem

    @Streaming
    @GET("tasks/{id}/solution/file")
    suspend fun taskSolutionFile(@Path("id") id: String): ResponseBody

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String)

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

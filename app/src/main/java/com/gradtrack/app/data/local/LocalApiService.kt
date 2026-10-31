package com.gradtrack.app.data.local

import android.content.Context
import com.google.gson.Gson
import com.gradtrack.app.data.SessionStore
import com.gradtrack.app.data.model.ActivityItem
import com.gradtrack.app.data.model.AuthResponse
import com.gradtrack.app.data.model.ChangePasswordRequest
import com.gradtrack.app.data.model.CreateTeamRequest
import com.gradtrack.app.data.model.Deadline
import com.gradtrack.app.data.model.DocumentInfo
import com.gradtrack.app.data.model.Feedback
import com.gradtrack.app.data.model.FeedbackRequest
import com.gradtrack.app.data.model.InviteRequest
import com.gradtrack.app.data.model.Invitation
import com.gradtrack.app.data.model.LoginRequest
import com.gradtrack.app.data.model.Milestone
import com.gradtrack.app.data.model.MilestoneRequest
import com.gradtrack.app.data.model.MilestoneStatus
import com.gradtrack.app.data.model.MyTeam
import com.gradtrack.app.data.model.NotificationItem
import com.gradtrack.app.data.model.NotificationType
import com.gradtrack.app.data.model.Project
import com.gradtrack.app.data.model.ProjectOverview
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.data.model.ProposalDetail
import com.gradtrack.app.data.model.ProposalStatusInfo
import com.gradtrack.app.data.model.ProposalSummary
import com.gradtrack.app.data.model.RegisterProjectRequest
import com.gradtrack.app.data.model.ReviewDecision
import com.gradtrack.app.data.model.ReviewRequest
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.StepState
import com.gradtrack.app.data.model.StudentDashboard
import com.gradtrack.app.data.model.Submission
import com.gradtrack.app.data.model.SubmissionStatus
import com.gradtrack.app.data.model.SubmissionsOverview
import com.gradtrack.app.data.model.SupervisorDashboard
import com.gradtrack.app.data.model.TaskItem
import com.gradtrack.app.data.model.TaskRequest
import com.gradtrack.app.data.model.TaskStatus
import com.gradtrack.app.data.model.TeamInfo
import com.gradtrack.app.data.model.TeamMember
import com.gradtrack.app.data.model.TimelineStep
import com.gradtrack.app.data.model.UpdateProfileRequest
import com.gradtrack.app.data.model.User
import com.gradtrack.app.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Base64

class LocalApiService(
    context: Context,
    private val session: SessionStore,
    private val openLogin: Boolean,
    private val store: StateStore,
) : ApiService {
    private val appContext = context.applicationContext
    private val gson = Gson()
    private val mutex = Mutex()
    private var db = LocalDb()
    private var version = 0L

    override suspend fun login(request: LoginRequest): AuthResponse =
        write {
            val id = request.identifier.trim()
            if (id.isEmpty()) fail(422, "Enter your university ID.")
            val account = accounts.firstOrNull { it.user?.universityId == id }
            val user =
                when {
                    account != null -> {
                        if (!openLogin && account.passwordHash != hash(request.password, account.salt)) fail(401, "Incorrect university ID or password.")
                        account.user!!
                    }
                    // Everyone starts as a student; creating a team is what makes someone a leader.
                    openLogin -> createTestAccount(id, if (request.role == Role.SUPERVISOR) Role.SUPERVISOR else Role.STUDENT, request.password)
                    else -> fail(401, "Incorrect university ID or password.")
                }
            if ((user.role == Role.SUPERVISOR) != (request.role == Role.SUPERVISOR)) {
                fail(403, if (user.role == Role.SUPERVISOR) "This university ID belongs to a supervisor. Use the supervisor login." else "This university ID belongs to a student. Use the student login.")
            }
            AuthResponse(token(user), user)
        }

    private fun LocalDb.createTestAccount(universityId: String, role: Role, password: String): User {
        val name = "${roleName(role).replaceFirstChar { it.uppercase() }} $universityId"
        val user = User(newId("u"), name, "", universityId, null, null, role)
        val salt = newSalt()
        accounts += AccountRecord(user, salt, hash(password, salt))
        return user
    }

    override suspend fun logout() = Unit

    override suspend fun me(): User = read { me() }

    override suspend fun updateProfile(request: UpdateProfileRequest): User =
        write {
            val me = me()
            val email = request.email.trim()
            if (request.fullName.isBlank()) fail(422, "Enter your full name.")
            if (email.isNotEmpty() && !EMAIL.matches(email)) fail(422, "Enter a valid email address.")
            if (email.isNotEmpty() && accounts.any { it.user?.id != me.id && it.user?.email.equals(email, ignoreCase = true) }) fail(409, "Another account already uses this email.")
            val updated =
                me.copy(
                    fullName = request.fullName.trim(),
                    email = email,
                    department = request.department.trim().ifEmpty { null },
                    academicYear = request.academicYear.trim().ifEmpty { null }.takeIf { me.role != Role.SUPERVISOR },
                )
            account(me.id).user = updated
            updated
        }

    override suspend fun changePassword(request: ChangePasswordRequest): Unit =
        write {
            val account = account(me().id)
            if (account.passwordHash != hash(request.currentPassword, account.salt)) fail(403, "The current password is incorrect.")
            if (request.newPassword.length < 8) fail(422, "The new password must be at least 8 characters.")
            account.salt = newSalt()
            account.passwordHash = hash(request.newPassword, account.salt)
        }

    override suspend fun studentDashboard(): StudentDashboard =
        read {
            val me = me()
            val team = teamOf(me.id)
            val project = team?.let { projectOf(it.id) }
            val open = project?.let { milestonesOf(it.id) }.orEmpty().filter { it.status != MilestoneStatus.COMPLETED }
            val today = LocalDate.now()
            StudentDashboard(
                progress = project?.let { progressOf(it) } ?: 0,
                upcomingDeadlines = open.count { dueWithin(it.dueDate, today, 14) },
                supervisorFeedback = project?.let { p -> feedback.count { it.projectId == p.id && it.authorId == p.supervisorId } } ?: 0,
                teamMembers = team?.memberIds?.size ?: 0,
                project = project?.let { toModel(it) },
                deadlines = open.take(3).map { Deadline(it.id, it.name, it.dueDate) },
                activity = activityOf(me.id),
            )
        }

    override suspend fun supervisorDashboard(): SupervisorDashboard =
        read {
            val me = requireRole(Role.SUPERVISOR, "Only supervisors have a supervisor dashboard.")
            val mine = projects.filter { it.supervisorId == me.id }
            val active = mine.filter { it.status == ProjectStatus.IN_PROGRESS || it.status == ProjectStatus.APPROVED }
            val today = LocalDate.now()
            val openMilestones =
                active.flatMap { p -> milestonesOf(p.id).filter { it.status != MilestoneStatus.COMPLETED }.map { p to it } }.sortedBy { it.second.dueDate }
            SupervisorDashboard(
                activeProjects = active.size,
                pendingProposals = mine.count { it.status == ProjectStatus.UNDER_REVIEW },
                submissionsToReview = submissions.count { s -> s.status == SubmissionStatus.REVIEW && mine.any { it.id == s.projectId } },
                upcomingDeadlines = openMilestones.count { dueWithin(it.second.dueDate, today, 14) },
                proposals = mine.filter { it.status == ProjectStatus.UNDER_REVIEW }.map { summaryOf(it) },
                deadlines = openMilestones.take(3).map { (p, m) -> Deadline(m.id, "${teamName(p.teamId)} · ${m.name}", m.dueDate) },
                activity = activityOf(me.id),
            )
        }

    override suspend fun currentProject(): ProjectOverview =
        read {
            val me = me()
            val project = teamOf(me.id)?.let { projectOf(it.id) } ?: return@read ProjectOverview(null, emptyList(), emptyList(), null)
            ProjectOverview(
                project = toModel(project),
                milestones = milestonesOf(project.id).map { it.toModel() },
                feedback = feedbackOf(project.id),
                latestSubmission = submissions.filter { it.projectId == project.id }.maxByOrNull { it.submittedAt }?.let { toModel(it) },
            )
        }

    override suspend fun registerProject(request: RegisterProjectRequest): Project =
        write {
            val me = requireRole(Role.LEADER, "Only the team leader can register the project.")
            val team = teamOf(me.id) ?: fail(409, "Create your team before registering the project.")
            if (request.title.trim().length < 5) fail(422, "The title must be at least 5 characters.")
            val now = Instant.now().toString()
            val existing = projectOf(team.id)
            if (existing != null && existing.status !in setOf(ProjectStatus.CHANGES_REQUESTED, ProjectStatus.REJECTED)) {
                fail(409, "The proposal was already sent to your supervisor and can't be edited.")
            }
            assignSupervisor(team)
            val supervisor = accounts.mapNotNull { it.user }.firstOrNull { it.id == team.supervisorId && it.role == Role.SUPERVISOR }
                ?: fail(422, "No supervisor has been assigned to your team yet. Try again once a supervisor account exists.")
            val project = existing ?: ProjectRecord(id = newId("p"), teamId = team.id, submittedAt = now)
            val wasDecided = existing != null
            project.apply {
                supervisorId = supervisor.id
                title = request.title.trim()
                category = request.category
                problemStatement = request.problemStatement
                objectives = request.objectives
                technologies = request.technologies.toMutableList()
                targetUsers = request.targetUsers
                durationMonths = request.durationMonths
                proposalFileName = request.proposalFileName ?: proposalFileName
                updatedAt = now
                if (existing == null || wasDecided) {
                    status = ProjectStatus.UNDER_REVIEW
                    submittedAt = now
                }
            }
            if (existing == null) {
                projects += project
                notify(listOf(supervisor.id), NotificationType.PROPOSAL, "New proposal to review", "${team.name} submitted \"${project.title}\".")
                notify(team.memberIds - me.id, NotificationType.PROPOSAL, "Proposal submitted", "\"${project.title}\" was sent to ${supervisor.fullName}.")
            } else if (wasDecided) {
                notify(listOf(supervisor.id), NotificationType.PROPOSAL, "Proposal resubmitted", "${team.name} revised \"${project.title}\".")
            }
            toModel(project)
        }

    override suspend fun proposalStatus(): ProposalStatusInfo =
        read {
            val me = me()
            val project = teamOf(me.id)?.let { projectOf(it.id) } ?: fail(404, "Your team has not registered a project yet.")
            val underReview = project.status == ProjectStatus.UNDER_REVIEW
            ProposalStatusInfo(
                projectId = project.id,
                title = project.title,
                status = project.status,
                submittedAt = project.submittedAt,
                updatedAt = project.updatedAt,
                steps =
                    listOf(
                        TimelineStep("Draft created", StepState.DONE),
                        TimelineStep("Submitted for review", StepState.DONE),
                        TimelineStep("Supervisor review", if (underReview) StepState.CURRENT else StepState.DONE),
                        TimelineStep("Final decision", if (underReview) StepState.WAITING else StepState.DONE),
                    ),
                feedback = feedbackOf(project.id),
                documentName = project.proposalFileName,
            )
        }

    override suspend fun uploadProposalFile(file: MultipartBody.Part): Project =
        write {
            val me = requireRole(Role.LEADER, "Only the team leader can upload the proposal.")
            val project = teamOf(me.id)?.let { projectOf(it.id) } ?: fail(404, "Register the project first.")
            if (project.status != ProjectStatus.UNDER_REVIEW) fail(409, "The proposal can only be changed while it is being sent for review.")
            project.proposalFileName = storeUpload(file, "${project.id}-proposal", "proposal.pdf")
            toModel(project)
        }

    override suspend fun myProposalFile(): ResponseBody =
        read {
            val project = teamOf(me().id)?.let { projectOf(it.id) } ?: fail(404, "Your team has no project yet.")
            storedUpload("${project.id}-proposal")
        }

    override suspend fun proposalFile(id: String): ResponseBody =
        read {
            val me = requireRole(Role.SUPERVISOR, "Only supervisors review proposals.")
            val project = projects.firstOrNull { it.id == id && it.supervisorId == me.id } ?: fail(404, "Proposal not found.")
            storedUpload("${project.id}-proposal")
        }

    override suspend fun replyToFeedback(request: FeedbackRequest): Feedback =
        write {
            val me = me()
            val project = teamOf(me.id)?.let { projectOf(it.id) } ?: fail(404, "Your team has no project yet.")
            if (request.body.isBlank()) fail(422, "Write a reply first.")
            val record = FeedbackRecord(newId("f"), project.id, me.id, request.body.trim(), Instant.now().toString())
            feedback += record
            notify(listOf(project.supervisorId), NotificationType.FEEDBACK, "New reply from ${teamName(project.teamId)}", record.body.take(80))
            Feedback(record.id, me.fullName, record.createdAt, record.body)
        }

    override suspend fun submissions(): List<Submission> =
        read {
            val project = teamOf(me().id)?.let { projectOf(it.id) } ?: return@read emptyList()
            submissions.filter { it.projectId == project.id }.sortedByDescending { it.submittedAt }.map { toModel(it) }
        }

    override suspend fun uploadDeliverable(milestoneId: String, file: MultipartBody.Part): Submission =
        write {
            val me = requireRole(Role.LEADER, "Only the team leader can submit deliverables.")
            val project = teamOf(me.id)?.let { projectOf(it.id) } ?: fail(404, "Your team has no project yet.")
            if (project.status != ProjectStatus.IN_PROGRESS) fail(409, "Deliverables open once the proposal is approved.")
            val milestone = milestones.firstOrNull { it.id == milestoneId && it.projectId == project.id } ?: fail(404, "Milestone not found.")
            if (milestone.status != MilestoneStatus.IN_PROGRESS) fail(409, "You can only submit to the current milestone.")
            if (submissions.any { it.milestoneId == milestoneId && it.status == SubmissionStatus.REVIEW }) {
                fail(409, "A submission for this milestone is already waiting for review.")
            }
            val name = file.headers?.get("Content-Disposition")?.substringAfter("filename=\"", "")?.substringBefore('"')?.ifBlank { null } ?: "deliverable"
            val record = SubmissionRecord(newId("s"), project.id, milestone.id, name, milestone.name, Instant.now().toString(), me.id)
            val bytes = Buffer().also { file.body.writeTo(it) }.readByteArray()
            store.putFile(record.id, bytes)
            submissions += record
            notify(listOf(project.supervisorId), NotificationType.SUBMISSION, "New submission", "${teamName(project.teamId)} submitted $name for ${milestone.name}.")
            toModel(record)
        }

    override suspend fun createMilestone(request: MilestoneRequest): Milestone =
        write {
            val (me, project) = editableProject()
            val (name, due) = validMilestone(project, request, null)
            val order = (milestones.filter { it.projectId == project.id }.maxOfOrNull { it.order } ?: -1) + 1
            val record = MilestoneRecord(newId("m"), project.id, name, due, order = order)
            milestones += record
            syncMilestones(project)
            notifyMilestone(project, me, "Milestone added", "$name · due $due")
            record.toModel()
        }

    override suspend fun updateMilestone(id: String, request: MilestoneRequest): Milestone =
        write {
            val (me, project) = editableProject()
            val record = milestones.firstOrNull { it.id == id && it.projectId == project.id } ?: fail(404, "Milestone not found.")
            if (record.status == MilestoneStatus.COMPLETED) fail(409, "Completed milestones can't be changed.")
            val (name, due) = validMilestone(project, request, record)
            val moved = record.dueDate != due
            record.name = name
            record.dueDate = due
            syncMilestones(project)
            notifyMilestone(project, me, if (moved) "Deadline changed" else "Milestone renamed", "$name · due $due")
            record.toModel()
        }

    override suspend fun deleteMilestone(id: String): Unit =
        write {
            val (me, project) = editableProject()
            val record = milestones.firstOrNull { it.id == id && it.projectId == project.id } ?: fail(404, "Milestone not found.")
            if (record.status == MilestoneStatus.COMPLETED) fail(409, "Completed milestones can't be deleted.")
            if (submissions.any { it.milestoneId == id }) fail(409, "This milestone already has submissions, so it can't be deleted.")
            milestones.remove(record)
            syncMilestones(project)
            notifyMilestone(project, me, "Milestone removed", record.name)
        }

    override suspend fun submissionFile(submissionId: String): ResponseBody =
        read {
            val me = me()
            val submission = submissions.firstOrNull { it.id == submissionId } ?: fail(404, "Submission not found.")
            val project = projects.first { it.id == submission.projectId }
            val allowed = project.supervisorId == me.id || teams.firstOrNull { it.id == project.teamId }?.memberIds?.contains(me.id) == true
            if (!allowed) fail(403, "You can't open this file.")
            val bytes = store.getFile(submission.id) ?: fail(404, "This file is no longer available. Ask the team to upload it again.")
            bytes.toResponseBody("application/octet-stream".toMediaType())
        }

    override suspend fun myTeam(): MyTeam =
        write {
            val me = me()
            val team = teamOf(me.id)?.also { assignSupervisor(it) }
            val invites =
                if (team != null) {
                    emptyList()
                } else {
                    invitations.filter { it.userId == me.id && it.status == InvitationRecord.STATUS_PENDING }.map { invite ->
                        val t = teams.first { it.id == invite.teamId }
                        Invitation(invite.id, t.name, userName(t.leaderId), invite.createdAt)
                    }
                }
            MyTeam(team?.let { toModel(it) }, invites)
        }

    override suspend fun createTeam(request: CreateTeamRequest): TeamInfo =
        write {
            val me = me()
            if (me.role == Role.SUPERVISOR) fail(403, "Supervisors can't create teams.")
            if (teamOf(me.id) != null) fail(409, "You are already in a team.")
            val name = request.name.trim()
            if (name.length < 3) fail(422, "The team name must be at least 3 characters.")
            if (teams.any { it.name.equals(name, ignoreCase = true) }) fail(409, "Another team already uses this name.")
            val team = TeamRecord(newId("t"), name, me.id, MAX_MEMBERS, mutableListOf(me.id), Instant.now().toString())
            assignSupervisor(team)
            teams += team
            // Starting a team makes this student its leader; their pending invitations no longer apply.
            account(me.id).user = me.copy(role = Role.LEADER)
            invitations.filter { it.userId == me.id && it.status == InvitationRecord.STATUS_PENDING }.forEach { it.status = InvitationRecord.STATUS_DECLINED }
            toModel(team)
        }

    override suspend fun inviteMember(request: InviteRequest): Unit =
        write {
            val me = requireRole(Role.LEADER, "Only the team leader can invite members.")
            val team = teamOf(me.id) ?: fail(409, "Create your team first.")
            val target = accounts.mapNotNull { it.user }.firstOrNull { it.universityId == request.universityId.trim() }
                ?: fail(404, "No registered student has this university ID.")
            if (target.role != Role.STUDENT) fail(409, "Only students can be invited to a team.")
            if (teamOf(target.id) != null) fail(409, "${target.fullName} is already in a team.")
            val pending = invitations.filter { it.teamId == team.id && it.status == InvitationRecord.STATUS_PENDING }
            if (pending.any { it.userId == target.id }) fail(409, "${target.fullName} already has a pending invitation.")
            if (team.memberIds.size + pending.size >= team.maxMembers) fail(409, "The team is full (${team.maxMembers} members).")
            invitations += InvitationRecord(newId("i"), team.id, target.id, InvitationRecord.STATUS_PENDING, Instant.now().toString())
            notify(listOf(target.id), NotificationType.TEAM, "Team invitation", "${me.fullName} invited you to join ${team.name}.")
        }

    override suspend fun acceptInvitation(id: String): Unit =
        write {
            val me = me()
            val invite = invitations.firstOrNull { it.id == id && it.userId == me.id && it.status == InvitationRecord.STATUS_PENDING }
                ?: fail(404, "This invitation is no longer available.")
            if (teamOf(me.id) != null) fail(409, "You are already in a team.")
            val team = teams.first { it.id == invite.teamId }
            if (team.memberIds.size >= team.maxMembers) fail(409, "This team is already full.")
            team.memberIds += me.id
            invite.status = InvitationRecord.STATUS_ACCEPTED
            invitations.filter { it.userId == me.id && it.status == InvitationRecord.STATUS_PENDING }.forEach { it.status = InvitationRecord.STATUS_DECLINED }
            notify(listOf(team.leaderId), NotificationType.TEAM, "Team member joined", "${me.fullName} joined ${team.name}.")
        }

    override suspend fun declineInvitation(id: String): Unit =
        write {
            val me = me()
            val invite = invitations.firstOrNull { it.id == id && it.userId == me.id && it.status == InvitationRecord.STATUS_PENDING }
                ?: fail(404, "This invitation is no longer available.")
            invite.status = InvitationRecord.STATUS_DECLINED
            val team = teams.first { it.id == invite.teamId }
            notify(listOf(team.leaderId), NotificationType.TEAM, "Invitation declined", "${me.fullName} declined the invitation to ${team.name}.")
        }

    override suspend fun removeMember(userId: String): Unit =
        write {
            val me = requireRole(Role.LEADER, "Only the team leader can remove members.")
            val team = teamOf(me.id) ?: fail(404, "You have no team.")
            if (userId == team.leaderId) fail(409, "The team leader cannot be removed.")
            if (!team.memberIds.remove(userId)) fail(404, "This person is not in your team.")
            tasks.filter { it.teamId == team.id && it.assigneeId == userId && it.status != TaskStatus.DONE }.forEach { it.status = TaskStatus.CANCELLED }
            notify(listOf(userId), NotificationType.TEAM, "Removed from team", "You are no longer a member of ${team.name}.")
        }

    override suspend fun tasks(): List<TaskItem> =
        read {
            val me = me()
            val team = teamOf(me.id) ?: return@read emptyList()
            // The leader sees the whole board; everyone else sees only what is assigned to them.
            tasks.filter { it.teamId == team.id && it.status != TaskStatus.CANCELLED && (me.id == team.leaderId || it.assigneeId == me.id) }.sortedWith(compareBy({ it.status == TaskStatus.DONE }, { it.dueDate ?: "9999" })).map { toModel(it) }
        }

    override suspend fun createTask(request: TaskRequest): TaskItem =
        write {
            val me = requireRole(Role.LEADER, "Only the team leader can create tasks.")
            val team = teamOf(me.id) ?: fail(409, "Create your team first.")
            if (request.title.isBlank()) fail(422, "Give the task a title.")
            if (request.assigneeId !in team.memberIds) fail(422, "Assign the task to a team member.")
            val task = TaskRecord(newId("k"), team.id, request.title.trim(), request.description, request.assigneeId, TaskStatus.TODO, request.dueDate)
            tasks += task
            if (task.assigneeId != me.id) notify(listOf(task.assigneeId), NotificationType.TASK, "New task assigned", "${me.fullName} assigned you \"${task.title}\".")
            toModel(task)
        }

    override suspend fun updateTask(id: String, request: TaskRequest): TaskItem =
        write {
            val me = requireRole(Role.LEADER, "Only the team leader can edit tasks.")
            val team = teamOf(me.id) ?: fail(409, "Create your team first.")
            val task = tasks.firstOrNull { it.id == id && it.teamId == team.id } ?: fail(404, "Task not found.")
            if (request.assigneeId !in team.memberIds) fail(422, "Assign the task to a team member.")
            val reassigned = task.assigneeId != request.assigneeId
            task.title = request.title.trim()
            task.description = request.description
            task.assigneeId = request.assigneeId
            task.dueDate = request.dueDate
            if (reassigned && task.assigneeId != me.id) notify(listOf(task.assigneeId), NotificationType.TASK, "Task assigned to you", "\"${task.title}\" is now yours.")
            toModel(task)
        }

    override suspend fun task(id: String): TaskItem = read { toModel(visibleTask(id).second) }

    override suspend fun startTask(id: String): TaskItem =
        write {
            val (me, task) = visibleTask(id)
            if (task.assigneeId != me.id) fail(403, "Only the person assigned to this task can start it.")
            if (task.status == TaskStatus.TODO) task.status = TaskStatus.IN_PROGRESS
            toModel(task)
        }

    override suspend fun uploadTaskAttachment(id: String, file: MultipartBody.Part): TaskItem =
        write {
            val me = requireRole(Role.LEADER, "Only the team leader can attach files to tasks.")
            val team = teamOf(me.id) ?: fail(404, "You have no team.")
            val task = tasks.firstOrNull { it.id == id && it.teamId == team.id } ?: fail(404, "Task not found.")
            task.attachmentName = storeUpload(file, "${task.id}-brief", "attachment")
            toModel(task)
        }

    override suspend fun taskAttachment(id: String): ResponseBody =
        read {
            val task = visibleTask(id).second
            if (task.attachmentName == null) fail(404, "This task has no attachment.")
            storedUpload("${task.id}-brief")
        }

    override suspend fun submitTaskSolution(id: String, note: RequestBody?, file: MultipartBody.Part?): TaskItem =
        write {
            val (me, task) = visibleTask(id)
            if (task.assigneeId != me.id) fail(403, "Only the person assigned to this task can submit a solution.")
            if (task.status == TaskStatus.DONE) fail(409, "A solution was already submitted for this task.")
            val text = note?.let { Buffer().also(it::writeTo).readUtf8().trim() }?.ifEmpty { null }
            if (text == null && file == null) fail(422, "Add a note or attach a file with your solution.")
            task.solutionNote = text
            task.solutionFileName = file?.let { storeUpload(it, "${task.id}-solution", "solution") }
            task.status = TaskStatus.DONE
            task.completedAt = LocalDate.now().toString()
            val team = teams.first { it.id == task.teamId }
            if (me.id != team.leaderId) notify(listOf(team.leaderId), NotificationType.TASK, "Task completed", "${me.fullName} submitted a solution for \"${task.title}\".")
            toModel(task)
        }

    override suspend fun taskSolutionFile(id: String): ResponseBody =
        read {
            val task = visibleTask(id).second
            if (task.solutionFileName == null) fail(404, "No file was submitted with this solution.")
            storedUpload("${task.id}-solution")
        }

    override suspend fun deleteTask(id: String): Unit =
        write {
            val me = requireRole(Role.LEADER, "Only the team leader can delete tasks.")
            val team = teamOf(me.id) ?: fail(404, "You have no team.")
            val task = tasks.firstOrNull { it.id == id && it.teamId == team.id } ?: fail(404, "Task not found.")
            tasks.remove(task)
            store.deleteFile("${task.id}-brief")
            store.deleteFile("${task.id}-solution")
            if (task.assigneeId != me.id) notify(listOf(task.assigneeId), NotificationType.TASK, "Task removed", "${me.fullName} deleted \"${task.title}\".")
        }

    override suspend fun notifications(): List<NotificationItem> =
        read {
            val me = me()
            notifications.filter { it.userId == me.id }.sortedByDescending { it.createdAt }.map {
                NotificationItem(it.id, it.type, it.title, it.body, it.createdAt, it.read)
            }
        }

    override suspend fun markAllNotificationsRead(): Unit =
        write {
            val me = me()
            notifications.filter { it.userId == me.id }.forEach { it.read = true }
        }

    override suspend fun proposals(): List<ProposalSummary> =
        read {
            val me = requireRole(Role.SUPERVISOR, "Only supervisors review proposals.")
            projects.filter { it.supervisorId == me.id }.sortedByDescending { it.submittedAt }.map { summaryOf(it) }
        }

    override suspend fun proposalDetail(id: String): ProposalDetail =
        read {
            val me = requireRole(Role.SUPERVISOR, "Only supervisors review proposals.")
            val project = projects.firstOrNull { it.id == id && it.supervisorId == me.id } ?: fail(404, "Proposal not found.")
            val team = teams.first { it.id == project.teamId }
            ProposalDetail(
                id = project.id,
                title = project.title,
                category = project.category,
                teamName = team.name,
                submittedAt = project.submittedAt,
                status = project.status,
                targetUsers = project.targetUsers,
                duration = "${project.durationMonths} months",
                description = project.problemStatement,
                objectives = project.objectives.lines().map { it.trim().trimStart('-', '•', '*').trim() }.filter { it.isNotEmpty() },
                technologies = project.technologies,
                notes = null,
                members = toModel(team).members,
                documents = listOfNotNull(project.proposalFileName?.let { DocumentInfo(it, null) }),
            )
        }

    override suspend fun reviewProposal(id: String, request: ReviewRequest): Unit =
        write {
            val me = requireRole(Role.SUPERVISOR, "Only supervisors review proposals.")
            val project = projects.firstOrNull { it.id == id && it.supervisorId == me.id } ?: fail(404, "Proposal not found.")
            if (project.status != ProjectStatus.UNDER_REVIEW) fail(409, "This proposal already has a decision.")
            if (request.decision != ReviewDecision.APPROVE && request.comment.isNullOrBlank()) fail(422, "Explain your decision to the team.")
            val now = Instant.now().toString()
            project.updatedAt = now
            project.status =
                when (request.decision) {
                    ReviewDecision.APPROVE -> ProjectStatus.IN_PROGRESS
                    ReviewDecision.CHANGES -> ProjectStatus.CHANGES_REQUESTED
                    ReviewDecision.REJECT -> ProjectStatus.REJECTED
                }
            syncMilestones(project)
            request.comment?.takeIf { it.isNotBlank() }?.let { feedback += FeedbackRecord(newId("f"), project.id, me.id, it.trim(), now) }
            val verdict =
                when (request.decision) {
                    ReviewDecision.APPROVE -> "approved"
                    ReviewDecision.CHANGES -> "returned for changes"
                    ReviewDecision.REJECT -> "rejected"
                }
            notify(teams.first { it.id == project.teamId }.memberIds, NotificationType.PROPOSAL, "Proposal $verdict", "${me.fullName} $verdict \"${project.title}\".")
        }

    override suspend fun supervisorSubmissions(): SubmissionsOverview =
        read {
            val me = requireRole(Role.SUPERVISOR, "Only supervisors review submissions.")
            val mine = projects.filter { it.supervisorId == me.id }.map { it.id }.toSet()
            val list = submissions.filter { it.projectId in mine }.sortedByDescending { it.submittedAt }
            val weekAgo = Instant.now().minus(7, ChronoUnit.DAYS).toString()
            SubmissionsOverview(
                awaitingReview = list.count { it.status == SubmissionStatus.REVIEW },
                reviewedThisWeek = list.count { (it.reviewedAt ?: "") >= weekAgo },
                needRevision = list.count { it.status == SubmissionStatus.NEEDS_REVISION },
                submissions = list.map { toModel(it) },
            )
        }

    override suspend fun reviewSubmission(id: String, request: ReviewRequest): Submission =
        write {
            val me = requireRole(Role.SUPERVISOR, "Only supervisors review submissions.")
            val submission = submissions.firstOrNull { it.id == id } ?: fail(404, "Submission not found.")
            val project = projects.firstOrNull { it.id == submission.projectId && it.supervisorId == me.id } ?: fail(404, "Submission not found.")
            if (submission.status != SubmissionStatus.REVIEW) fail(409, "This submission was already reviewed.")
            if (request.decision != ReviewDecision.APPROVE && request.comment.isNullOrBlank()) fail(422, "Explain what needs to change.")
            val now = Instant.now().toString()
            submission.reviewedAt = now
            submission.status = if (request.decision == ReviewDecision.APPROVE) SubmissionStatus.APPROVED else SubmissionStatus.NEEDS_REVISION
            if (submission.status == SubmissionStatus.APPROVED) milestones.firstOrNull { it.id == submission.milestoneId }?.let { completeMilestone(project, it) }
            request.comment?.takeIf { it.isNotBlank() }?.let { feedback += FeedbackRecord(newId("f"), project.id, me.id, it.trim(), now) }
            val verdict = if (submission.status == SubmissionStatus.APPROVED) "approved" else "needs revision"
            notify(teams.first { it.id == project.teamId }.memberIds, NotificationType.SUBMISSION, "Submission $verdict", "${submission.fileName} — $verdict.")
            toModel(submission)
        }

    private fun LocalDb.editableProject(): Pair<User, ProjectRecord> {
        val me = requireRole(Role.LEADER, "Only the team leader can manage milestones.")
        val project = teamOf(me.id)?.let { projectOf(it.id) } ?: fail(404, "Register your project first.")
        if (project.status == ProjectStatus.COMPLETED) fail(409, "The project is completed, so its milestones are final.")
        return me to project
    }

    private fun LocalDb.validMilestone(project: ProjectRecord, request: MilestoneRequest, editing: MilestoneRecord?): Pair<String, String> {
        val name = request.name.trim()
        if (name.isEmpty()) fail(422, "Give the milestone a name.")
        if (name.length > 60) fail(422, "Keep the name under 60 characters.")
        val due = runCatching { LocalDate.parse(request.dueDate) }.getOrNull()?.toString() ?: fail(422, "Pick a deadline.")
        val others = milestonesOf(project.id).filter { it.id != editing?.id }
        if (others.any { it.name.equals(name, ignoreCase = true) }) fail(409, "Another milestone already has this name.")
        if (editing?.dueDate == due) return name to due
        if (due < LocalDate.now().toString()) fail(422, "The deadline can't be in the past.")
        val lastDone = others.filter { it.status == MilestoneStatus.COMPLETED }.maxOfOrNull { it.dueDate }
        if (lastDone != null && due <= lastDone) fail(422, "The deadline must come after the last completed milestone ($lastDone).")
        val waiting = milestonesOf(project.id).firstOrNull { m -> m.status == MilestoneStatus.IN_PROGRESS && submissions.any { it.milestoneId == m.id && it.status == SubmissionStatus.REVIEW } }
        if (waiting != null) {
            val jumpsAhead = if (waiting.id == editing?.id) others.any { it.status != MilestoneStatus.COMPLETED && it.dueDate <= due } else due < waiting.dueDate
            if (jumpsAhead) fail(409, "${waiting.name} is waiting for the supervisor's review, so it has to stay the current milestone.")
        }
        return name to due
    }

    private fun LocalDb.syncMilestones(project: ProjectRecord) {
        milestonesOf(project.id).filter { it.status != MilestoneStatus.COMPLETED }.forEachIndexed { index, milestone ->
            milestone.status = if (index == 0 && project.status == ProjectStatus.IN_PROGRESS) MilestoneStatus.IN_PROGRESS else MilestoneStatus.UPCOMING
        }
    }

    private fun LocalDb.completeMilestone(project: ProjectRecord, milestone: MilestoneRecord) {
        milestone.status = MilestoneStatus.COMPLETED
        if (milestonesOf(project.id).all { it.status == MilestoneStatus.COMPLETED }) project.status = ProjectStatus.COMPLETED else syncMilestones(project)
    }

    private fun LocalDb.notifyMilestone(project: ProjectRecord, me: User, title: String, body: String) {
        val team = teams.first { it.id == project.teamId }
        // Stage planning is the team's business; the supervisor hears about submissions, not schedule edits.
        notify(team.memberIds - me.id, NotificationType.DEADLINE, title, "${team.name}: $body")
    }

    /** Earlier builds also sent milestone edits to the supervisor; clear those out of their inbox. */
    private fun LocalDb.dropSupervisorMilestoneNotices() {
        val supervisors = projects.map { it.supervisorId }.toSet()
        notifications.removeAll { it.type == NotificationType.DEADLINE && it.userId in supervisors }
    }

    private fun LocalDb.notify(userIds: Collection<String>, type: NotificationType, title: String, body: String) {
        val now = Instant.now().toString()
        userIds.distinct().forEach { notifications += NotificationRecord(newId("n"), it, type, title, body, now) }
    }

    private fun LocalDb.me(): User {
        val id = session.user?.id ?: fail(401, "Please sign in again.")
        return accounts.firstOrNull { it.user?.id == id }?.user ?: fail(401, "Your account no longer exists. Please sign in again.")
    }

    private fun LocalDb.requireRole(role: Role, message: String): User = me().also { if (it.role != role) fail(403, message) }

    private fun LocalDb.account(userId: String) = accounts.first { it.user?.id == userId }

    private fun LocalDb.userName(userId: String) = accounts.firstOrNull { it.user?.id == userId }?.user?.fullName ?: "Unknown"

    private fun LocalDb.teamOf(userId: String) = teams.firstOrNull { userId in it.memberIds }

    private fun LocalDb.teamName(teamId: String) = teams.firstOrNull { it.id == teamId }?.name ?: "Team"

    private fun LocalDb.projectOf(teamId: String) = projects.firstOrNull { it.teamId == teamId }

    private fun LocalDb.milestonesOf(projectId: String) = milestones.filter { it.projectId == projectId }.sortedWith(compareBy({ it.dueDate }, { it.order }))

    private fun LocalDb.progressOf(project: ProjectRecord): Int {
        val list = milestonesOf(project.id)
        return if (list.isEmpty()) 0 else list.count { it.status == MilestoneStatus.COMPLETED } * 100 / list.size
    }

    private fun LocalDb.feedbackOf(projectId: String) =
        feedback.filter { it.projectId == projectId }.sortedBy { it.createdAt }.map { Feedback(it.id, userName(it.authorId), it.createdAt, it.body) }

    private fun LocalDb.activityOf(userId: String) =
        notifications.filter { it.userId == userId }.sortedByDescending { it.createdAt }.take(3).map { ActivityItem(it.id, it.title, it.body, it.createdAt) }

    private fun LocalDb.summaryOf(project: ProjectRecord) = ProposalSummary(project.id, project.title, teamName(project.teamId), project.submittedAt, project.status)

    private fun LocalDb.toModel(project: ProjectRecord): Project {
        val list = milestonesOf(project.id)
        val leader = teams.firstOrNull { it.id == project.teamId }?.leaderId
        return Project(
            id = project.id,
            title = project.title,
            category = project.category,
            department = leader?.let { id -> accounts.firstOrNull { it.user?.id == id }?.user?.department },
            academicYear = academicYearOf(project.submittedAt),
            status = project.status,
            progress = progressOf(project),
            completedMilestones = list.count { it.status == MilestoneStatus.COMPLETED },
            totalMilestones = list.size,
            supervisorId = project.supervisorId,
            supervisorName = userName(project.supervisorId),
            problemStatement = project.problemStatement,
            objectives = project.objectives,
            technologies = project.technologies,
            targetUsers = project.targetUsers,
            durationMonths = project.durationMonths,
            proposalFileName = project.proposalFileName,
        )
    }

    private fun LocalDb.assignSupervisor(team: TeamRecord) {
        val supervisors = accounts.mapNotNull { it.user }.filter { it.role == Role.SUPERVISOR }
        if (supervisors.none { it.id == team.supervisorId }) team.supervisorId = supervisors.randomOrNull()?.id.orEmpty()
    }

    private fun LocalDb.toModel(team: TeamRecord): TeamInfo {
        val project = projectOf(team.id)
        return TeamInfo(
            id = team.id,
            name = team.name,
            leaderId = team.leaderId,
            members =
                team.memberIds.mapNotNull { id -> accounts.firstOrNull { it.user?.id == id }?.user }.map {
                    val leader = it.id == team.leaderId
                    TeamMember(it.id, it.fullName, it.universityId, if (leader) "Team Leader" else "Student", leader, "Active", it.avatarUrl)
                },
            maxMembers = team.maxMembers,
            pendingInvites = invitations.count { it.teamId == team.id && it.status == InvitationRecord.STATUS_PENDING },
            projectTitle = project?.title,
            supervisorName = (project?.supervisorId ?: team.supervisorId).takeIf { it.isNotEmpty() }?.let { userName(it) },
            semester = semesterOf(team.createdAt),
        )
    }

    private fun LocalDb.toModel(submission: SubmissionRecord): Submission {
        val project = projects.firstOrNull { it.id == submission.projectId }
        return Submission(
            id = submission.id,
            fileName = submission.fileName,
            teamName = project?.let { teamName(it.teamId) },
            type = submission.type,
            submittedAt = submission.submittedAt,
            reviewerName = project?.let { userName(it.supervisorId) },
            status = submission.status,
            milestoneId = submission.milestoneId,
        )
    }

    private fun LocalDb.toModel(task: TaskRecord) =
        TaskItem(
            task.id, task.title, task.description, task.assigneeId, userName(task.assigneeId).substringBefore(' '), task.status, task.dueDate, task.completedAt,
            task.attachmentName, task.solutionNote, task.solutionFileName,
        )

    /** The leader can open any task on the team's board; anyone else only the tasks assigned to them. */
    private fun LocalDb.visibleTask(id: String): Pair<User, TaskRecord> {
        val me = me()
        val team = teamOf(me.id) ?: fail(404, "You have no team.")
        val task = tasks.firstOrNull { it.id == id && it.teamId == team.id && it.status != TaskStatus.CANCELLED } ?: fail(404, "Task not found.")
        if (me.id != team.leaderId && task.assigneeId != me.id) fail(403, "This task is assigned to someone else.")
        return me to task
    }

    private fun storeUpload(file: MultipartBody.Part, key: String, fallback: String): String {
        val name = file.headers?.get("Content-Disposition")?.substringAfter("filename=\"", "")?.substringBefore('"')?.ifBlank { null } ?: fallback
        store.putFile(key, Buffer().also { file.body.writeTo(it) }.readByteArray())
        return name
    }

    private fun storedUpload(key: String): ResponseBody {
        val bytes = store.getFile(key) ?: fail(404, "This file was never uploaded or is no longer available. Ask the team to upload it again.")
        return bytes.toResponseBody("application/octet-stream".toMediaType())
    }

    private fun MilestoneRecord.toModel() = Milestone(id, name, dueDate, status)

    private fun importAccounts(context: Context, db: LocalDb) {
        val json = runCatching { context.assets.open(ACCOUNTS_FILE).bufferedReader().use { it.readText() } }.getOrNull() ?: return
        val listing = runCatching { gson.fromJson(json, AccountsFile::class.java) }.getOrNull() ?: return
        listing.accounts.orEmpty().forEach { entry ->
            val role = entry.role?.let { name -> Role.entries.firstOrNull { it.name.equals(name, ignoreCase = true) || (name.equals("leader", true) && it == Role.LEADER) } }
            val email = entry.email?.trim().orEmpty()
            val universityId = entry.universityId?.trim()?.ifEmpty { null }
            val valid =
                role != null && !entry.fullName.isNullOrBlank() && (email.isEmpty() || EMAIL.matches(email)) && (entry.password?.length ?: 0) >= 8 &&
                    universityId != null && UNIVERSITY_ID.matches(universityId)
            if (!valid) return@forEach
            val exists = db.accounts.any { it.user?.universityId == universityId }
            if (exists) return@forEach
            val user =
                User(
                    id = "u_${db.nextId++}",
                    fullName = entry.fullName!!.trim(),
                    email = email,
                    universityId = universityId,
                    department = entry.department?.trim()?.ifEmpty { null },
                    academicYear = entry.academicYear?.trim()?.ifEmpty { null }.takeIf { role != Role.SUPERVISOR },
                    role = role!!,
                )
            val salt = newSalt()
            db.accounts += AccountRecord(user, salt, hash(entry.password!!, salt))
        }
    }

    private class AccountsFile(val accounts: List<AccountEntry>?)

    private class AccountEntry(
        val role: String?,
        val fullName: String?,
        val universityId: String?,
        val email: String?,
        val department: String?,
        val academicYear: String?,
        val password: String?,
    )

    private fun roleName(role: Role) =
        when (role) {
            Role.STUDENT -> "student"
            Role.LEADER -> "team leader"
            Role.SUPERVISOR -> "supervisor"
        }

    private suspend fun <T> read(block: LocalDb.() -> T): T =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                refresh()
                db.block()
            }
        }

    /**
     * Runs [block] on the latest saved state and saves the result. If another phone saved in between,
     * the block runs again on its data, so nothing is overwritten and a failed block leaves no half-done changes.
     */
    private suspend fun <T> write(block: LocalDb.() -> T): T =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                repeat(WRITE_ATTEMPTS) {
                    refresh()
                    val result = db.block()
                    if (store.save(gson.toJson(db), version)) return@withContext result
                }
                fail(503, "Someone else is updating the data right now. Please try again.")
            }
        }

    private fun refresh() {
        val snapshot = store.load()
        db = snapshot?.let { runCatching { gson.fromJson(it.json, LocalDb::class.java) }.getOrNull() } ?: LocalDb()
        version = snapshot?.version ?: 0
        importAccounts(appContext, db)
        db.projects.forEach { db.syncMilestones(it) }
        db.dropSupervisorMilestoneNotices()
    }

    private fun LocalDb.newId(prefix: String) = "${prefix}_${nextId++}"

    private fun token(user: User) = "local-${user.id}"

    private fun newSalt(): String = Base64.getEncoder().encodeToString(ByteArray(16).also { SecureRandom().nextBytes(it) })

    private fun hash(password: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest("$salt:$password".toByteArray()).joinToString("") { "%02x".format(it) }

    private fun fail(code: Int, message: String): Nothing {
        val body = gson.toJson(mapOf("message" to message)).toResponseBody("application/json".toMediaType())
        throw HttpException(Response.error<Any>(code, body))
    }

    private fun dueWithin(date: String, today: LocalDate, days: Long): Boolean {
        val due = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
        return !due.isBefore(today) && !due.isAfter(today.plusDays(days))
    }

    private fun academicYearOf(instant: String): String {
        val date = runCatching { Instant.parse(instant).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }.getOrDefault(LocalDate.now())
        val start = if (date.monthValue >= 9) date.year else date.year - 1
        return "$start–${start + 1}"
    }

    private fun semesterOf(instant: String): String {
        val date = runCatching { Instant.parse(instant).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }.getOrDefault(LocalDate.now())
        return when (date.monthValue) {
            in 2..6 -> "Spring ${date.year}"
            7, 8 -> "Summer ${date.year}"
            1 -> "Fall ${date.year - 1}"
            else -> "Fall ${date.year}"
        }
    }

    private companion object {
        const val WRITE_ATTEMPTS = 5
        const val ACCOUNTS_FILE = "accounts.json"
        const val MAX_MEMBERS = 4
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$")
        val UNIVERSITY_ID = Regex("^\\d{5,12}$")
    }
}

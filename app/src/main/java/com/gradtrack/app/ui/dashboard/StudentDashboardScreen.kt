package com.gradtrack.app.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.StudentDashboard
import com.gradtrack.app.data.model.User
import com.gradtrack.app.data.repository.DashboardRepository
import com.gradtrack.app.ui.Routes
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.LocalContainer
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.common.greetingName
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.components.GlassPill
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.HeroButton
import com.gradtrack.app.ui.components.HeroCard
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.MutedText
import com.gradtrack.app.ui.components.NextStepHero
import com.gradtrack.app.ui.components.PageHeader
import com.gradtrack.app.ui.components.ProgressRing
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.components.StatCard
import com.gradtrack.app.ui.components.StatRow
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import java.time.LocalTime

class StudentDashboardViewModel(repository: DashboardRepository) : LoadableViewModel<StudentDashboard>({ repository.student() })

@Composable
fun greeting(user: User?, doctor: Boolean = false): String {
    val name = user?.greetingName()
    val hour = LocalTime.now().hour
    return when {
        name == null -> stringResource(if (hour < 12) R.string.greeting_morning_plain else if (hour < 18) R.string.greeting_afternoon_plain else R.string.greeting_evening_plain)
        doctor -> stringResource(if (hour < 12) R.string.greeting_morning_dr else if (hour < 18) R.string.greeting_afternoon_dr else R.string.greeting_evening_dr, name)
        else -> stringResource(if (hour < 12) R.string.greeting_morning else if (hour < 18) R.string.greeting_afternoon else R.string.greeting_evening, name)
    }
}

@Composable
fun StudentDashboardScreen(onNavigate: (String) -> Unit) {
    val vm = appViewModel { StudentDashboardViewModel(it.dashboard) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val user by LocalContainer.current.session.currentUser.collectAsStateWithLifecycle()
    EventsEffect(vm)
    LifecycleResumeEffect(vm) {
        if (vm.state.value is UiState.Success) vm.refresh()
        onPauseOrDispose {}
    }

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { data ->
        item {
            PageHeader(
                title = greeting(user),
                overline = stringResource(if (user?.role == Role.LEADER) R.string.leader_workspace else R.string.student_workspace),
                subtitle = stringResource(R.string.dashboard_subtitle_student),
            )
        }
        item { StudentHero(data, user, onNavigate) }
        item {
            StatRow {
                StatCard(stringResource(R.string.stat_deadlines_short), data.upcomingDeadlines.toString(), Modifier.weight(1f), stringResource(R.string.stat_next_14_days), R.drawable.ic_calendar)
                StatCard(stringResource(R.string.stat_feedback_short), data.supervisorFeedback.toString(), Modifier.weight(1f), stringResource(R.string.stat_from_supervisor), R.drawable.ic_feedback)
                StatCard(stringResource(R.string.stat_team_short), data.teamMembers.toString(), Modifier.weight(1f), stringResource(R.string.stat_members), R.drawable.ic_team, onClick = { onNavigate(Routes.TEAM) })
            }
        }
        deadlinesAndActivity(data.deadlines.isEmpty(), { data.deadlines.forEach { DeadlineRow(it) } }, data.activity.isEmpty(), { data.activity.forEach { ActivityRow(it) } })
    }
}

fun LazyListScope.deadlinesAndActivity(
    noDeadlines: Boolean,
    deadlines: @Composable () -> Unit,
    noActivity: Boolean,
    activity: @Composable () -> Unit,
    deadlinesTitle: Int = R.string.upcoming_deadlines,
) {
    item {
        GtCard {
            SectionTitle(stringResource(deadlinesTitle))
            if (noDeadlines) MutedText(stringResource(R.string.no_deadlines), Modifier.padding(top = 8.dp)) else deadlines()
        }
    }
    item {
        GtCard {
            SectionTitle(stringResource(R.string.recent_activity))
            if (noActivity) MutedText(stringResource(R.string.no_activity), Modifier.padding(top = 8.dp)) else activity()
        }
    }
}

@Composable
private fun StudentHero(data: StudentDashboard, user: User?, onNavigate: (String) -> Unit) {
    val project = data.project
    val isLeader = user?.role == Role.LEADER
    val hasTeam = data.teamMembers > 0
    when {
        project != null ->
            HeroCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        GlassPill(stringResource(project.status.pill().label))
                        Text(project.title, style = GtType.Title.copy(color = Color.White, fontSize = 19.sp), maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
                        Text(listOfNotNull(project.category, project.supervisorName).joinToString(" · "), style = GtType.Caption.copy(color = Gt.Muted), modifier = Modifier.padding(top = 4.dp))
                    }
                    ProgressRing(project.progress, Modifier.padding(start = 16.dp))
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.milestones_completed, project.completedMilestones, project.totalMilestones), style = GtType.Caption.copy(color = Gt.Muted), modifier = Modifier.weight(1f))
                    HeroButton(stringResource(R.string.open_project), onClick = { onNavigate(Routes.PROJECT) })
                }
            }
        !hasTeam ->
            NextStepHero(R.drawable.ic_team, stringResource(R.string.start_team_needed_title), stringResource(R.string.start_team_needed_body), stringResource(R.string.get_started)) {
                onNavigate(Routes.TEAM_START)
            }
        // A brand-new leader is the only member: building the team comes before the proposal.
        isLeader && data.teamMembers <= 1 ->
            NextStepHero(R.drawable.ic_person_add, stringResource(R.string.next_invite_title), stringResource(R.string.next_invite_body), stringResource(R.string.invite_member)) {
                onNavigate(Routes.TEAM)
            }
        isLeader ->
            NextStepHero(R.drawable.ic_proposals, stringResource(R.string.next_register_title), stringResource(R.string.next_register_body), stringResource(R.string.register_project)) {
                onNavigate(Routes.REGISTER_PROJECT)
            }
        else ->
            NextStepHero(R.drawable.ic_clock, stringResource(R.string.next_wait_project_title), stringResource(R.string.next_wait_project_body), stringResource(R.string.view_team)) {
                onNavigate(Routes.TEAM)
            }
    }
}

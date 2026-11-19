package com.gradtrack.app.ui.team

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.model.TeamInfo
import com.gradtrack.app.data.model.TeamMember
import com.gradtrack.app.data.repository.TeamRepository
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.components.Avatar
import com.gradtrack.app.ui.components.ConfirmDialog
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.MutedText
import com.gradtrack.app.ui.components.PageHeader
import com.gradtrack.app.ui.components.Pill
import com.gradtrack.app.ui.components.PrimaryButton
import com.gradtrack.app.ui.components.SecondaryButton
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.components.TextPromptDialog
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import com.gradtrack.app.ui.theme.Tone

class SupervisorTeamViewModel(private val teams: TeamRepository, private val id: String) : LoadableViewModel<TeamInfo>({ teams.team(id) }) {
    fun add(universityId: String) = act({ teams.addMember(id, universityId) }) { updated -> patch { updated }; send(UiEvent.Message(R.string.member_added)) }

    fun remove(member: TeamMember) = act({ teams.removeMember(id, member.id) }) { updated -> patch { updated }; send(UiEvent.Message(R.string.member_removed)) }

    fun makeLeader(member: TeamMember) = act({ teams.changeLeader(id, member.id) }) { updated -> patch { updated }; send(UiEvent.Message(R.string.leader_changed)) }

    fun delete() = act({ teams.delete(id) }) { send(UiEvent.Done(R.string.team_deleted)) }
}

private sealed interface TeamAction {
    data object Add : TeamAction

    data class Remove(val member: TeamMember) : TeamAction

    data class MakeLeader(val member: TeamMember) : TeamAction

    data object Delete : TeamAction
}

/** A supervisor managing one of the teams they supervise: members, leader, and deleting the team. */
@Composable
fun SupervisorTeamScreen(teamId: String, onDeleted: () -> Unit) {
    val vm = appViewModel(key = "supervised-team-$teamId") { SupervisorTeamViewModel(it.team, teamId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var action by remember { mutableStateOf<TeamAction?>(null) }
    EventsEffect(vm) { onDeleted() }

    when (val a = action) {
        TeamAction.Add ->
            TextPromptDialog(stringResource(R.string.add_student), stringResource(R.string.hint_university_id), stringResource(R.string.add), { action = null }, {
                action = null
                vm.add(it)
            }, numeric = true)
        is TeamAction.Remove ->
            ConfirmDialog(stringResource(R.string.remove_member), stringResource(R.string.remove_member_question, a.member.fullName), stringResource(R.string.remove), { action = null }, {
                action = null
                vm.remove(a.member)
            }, danger = true)
        is TeamAction.MakeLeader ->
            ConfirmDialog(stringResource(R.string.make_leader), stringResource(R.string.make_leader_confirm, a.member.fullName), stringResource(R.string.make_leader), { action = null }, {
                action = null
                vm.makeLeader(a.member)
            })
        TeamAction.Delete ->
            ConfirmDialog(stringResource(R.string.delete_team), stringResource(R.string.delete_team_confirm), stringResource(R.string.delete), { action = null }, {
                action = null
                vm.delete()
            }, danger = true)
        null -> Unit
    }

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { team ->
        item {
            PageHeader(
                title = team.name,
                overline = stringResource(R.string.crumb_supervised_team),
                subtitle = team.projectTitle ?: stringResource(R.string.not_registered_yet),
            )
        }
        item {
            GtCard(padding = 18.dp) {
                SectionTitle(stringResource(R.string.team_members_count, team.members.size, team.maxMembers))
                team.members.forEach { member ->
                    ManagedMemberRow(
                        member = member,
                        enabled = !busy,
                        onMakeLeader = { action = TeamAction.MakeLeader(member) },
                        onRemove = { action = TeamAction.Remove(member) },
                    )
                }
                if (team.members.size < team.maxMembers) {
                    PrimaryButton(stringResource(R.string.add_student), { action = TeamAction.Add }, Modifier.fillMaxWidth().padding(top = 10.dp), enabled = !busy, icon = R.drawable.ic_person_add)
                } else {
                    MutedText(stringResource(R.string.team_complete), Modifier.padding(top = 8.dp))
                }
            }
        }
        item { SecondaryButton(stringResource(R.string.delete_team), { action = TeamAction.Delete }, Modifier.fillMaxWidth(), enabled = !busy, danger = true) }
    }
}

@Composable
private fun ManagedMemberRow(member: TeamMember, enabled: Boolean, onMakeLeader: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(member.fullName, member.avatarUrl)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(member.fullName, style = GtType.Label)
            member.universityId?.let { Text(it, style = GtType.Caption) }
        }
        if (member.isLeader) {
            Pill(stringResource(R.string.role_team_leader), Tone.Green, Modifier.padding(start = 8.dp))
        } else {
            Box {
                IconButton(onClick = { menu = true }, enabled = enabled) {
                    Icon(painterResource(R.drawable.ic_more), stringResource(R.string.member_options), tint = Gt.TextSecondary, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.make_leader)) }, onClick = {
                        menu = false
                        onMakeLeader()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.remove_member), color = Gt.Error) }, onClick = {
                        menu = false
                        onRemove()
                    })
                }
            }
        }
    }
}

/** One supervised team on the supervisor's home screen. */
@Composable
fun SupervisedTeamRow(team: TeamInfo, onClick: () -> Unit) {
    val leader = team.members.firstOrNull { it.isLeader }?.fullName
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(team.name)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(team.name, style = GtType.Label)
            Text(
                listOfNotNull(stringResource(R.string.members_of, team.members.size, team.maxMembers), leader?.let { stringResource(R.string.led_by, it) }).joinToString(" · "),
                style = GtType.Caption,
            )
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Gt.TextSecondary, modifier = Modifier.size(20.dp))
    }
}

package com.gradtrack.app.ui.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Invitation
import com.gradtrack.app.data.model.MyTeam
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.TeamInfo
import com.gradtrack.app.data.model.TeamMember
import com.gradtrack.app.data.repository.TeamRepository
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.LocalContainer
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.common.asRelativeTime
import com.gradtrack.app.ui.components.Avatar
import com.gradtrack.app.ui.components.ConfirmDialog
import com.gradtrack.app.ui.components.EmptyState
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.HeroCard
import com.gradtrack.app.ui.components.LabeledValue
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.MemberRow
import com.gradtrack.app.ui.components.PageHeader
import com.gradtrack.app.ui.components.PrimaryButton
import com.gradtrack.app.ui.components.SecondaryButton
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.components.StatCard
import com.gradtrack.app.ui.components.StatRow
import com.gradtrack.app.ui.components.TextPromptDialog
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType

class TeamViewModel(private val repository: TeamRepository) : LoadableViewModel<MyTeam>({ repository.myTeam() }) {
    fun create(name: String) = act({ repository.create(name) }) { send(UiEvent.Message(R.string.team_created)); refresh() }

    fun accept(invitation: Invitation) = act({ repository.accept(invitation.id) }) { send(UiEvent.Message(R.string.joined_team)); refresh() }

    fun decline(invitation: Invitation) = act({ repository.decline(invitation.id) }) { refresh() }

    fun invite(universityId: String) =
        act({ repository.invite(universityId) }) {
            patch { it.copy(team = it.team?.copy(pendingInvites = it.team.pendingInvites + 1)) }
            send(UiEvent.Message(R.string.invitation_sent))
        }

    fun remove(member: TeamMember) =
        act({ repository.remove(member.id) }) {
            patch { mine -> mine.copy(team = mine.team?.let { team -> team.copy(members = team.members.filterNot { it.id == member.id }) }) }
            send(UiEvent.Message(R.string.member_removed))
        }
}

private sealed interface TeamDialog {
    data object Create : TeamDialog

    data object Invite : TeamDialog

    data class Remove(val member: TeamMember) : TeamDialog
}

@Composable
fun TeamScreen() {
    val vm = appViewModel { TeamViewModel(it.team) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val user by LocalContainer.current.session.currentUser.collectAsStateWithLifecycle()
    val isLeader = user?.role == Role.LEADER
    var dialog by remember { mutableStateOf<TeamDialog?>(null) }
    EventsEffect(vm)

    when (val d = dialog) {
        TeamDialog.Create ->
            TextPromptDialog(stringResource(R.string.create_team), stringResource(R.string.hint_team_name), stringResource(R.string.create), { dialog = null }, {
                dialog = null
                vm.create(it)
            })
        TeamDialog.Invite ->
            TextPromptDialog(stringResource(R.string.invite_member), stringResource(R.string.hint_university_id), stringResource(R.string.send_invitation), { dialog = null }, {
                dialog = null
                vm.invite(it)
            }, numeric = true)
        is TeamDialog.Remove ->
            ConfirmDialog(stringResource(R.string.remove_member), stringResource(R.string.remove_member_confirm, d.member.fullName), stringResource(R.string.remove), { dialog = null }, {
                dialog = null
                vm.remove(d.member)
            }, danger = true)
        null -> Unit
    }

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { data ->
        val team = data.team
        item {
            PageHeader(
                title = team?.name ?: stringResource(R.string.team_heading),
                overline = stringResource(R.string.crumb_team),
                subtitle = stringResource(R.string.team_subtitle),
            )
        }
        if (team == null) {
            if (isLeader) {
                item {
                    EmptyState(
                        R.drawable.ic_team,
                        stringResource(R.string.empty_create_team_title),
                        stringResource(R.string.empty_create_team_body),
                        primary = stringResource(R.string.create_team),
                        onPrimary = { dialog = TeamDialog.Create },
                    )
                }
            } else {
                item {
                    HeroCard {
                        Text(stringResource(R.string.your_university_id), style = GtType.Caption.copy(color = Gt.Muted))
                        Text(user?.universityId.orEmpty(), style = GtType.Display.copy(color = Color.White, fontSize = 28.sp, letterSpacing = 2.sp), modifier = Modifier.padding(top = 4.dp))
                        Text(stringResource(R.string.share_id_hint), style = GtType.Caption.copy(color = Gt.Muted), modifier = Modifier.padding(top = 6.dp))
                    }
                }
                if (data.invitations.isEmpty()) {
                    item { EmptyState(R.drawable.ic_mail, stringResource(R.string.empty_invitations_title), stringResource(R.string.empty_invitations_body)) }
                } else {
                    item { SectionTitle(stringResource(R.string.invitations)) }
                    data.invitations.forEach { invitation ->
                        item(key = invitation.id) { InvitationCard(invitation, busy, onAccept = { vm.accept(invitation) }, onDecline = { vm.decline(invitation) }) }
                    }
                }
            }
            return@LoadableScreen
        }
        teamContent(team, isLeader, busy, onInvite = { dialog = TeamDialog.Invite }, onRemove = { dialog = TeamDialog.Remove(it) })
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.teamContent(
    team: TeamInfo,
    isLeader: Boolean,
    busy: Boolean,
    onInvite: () -> Unit,
    onRemove: (TeamMember) -> Unit,
) {
    item {
        StatRow {
            StatCard(
                stringResource(R.string.stat_members),
                "${team.members.size}/${team.maxMembers}",
                Modifier.weight(1f),
                stringResource(if (team.members.size >= team.maxMembers) R.string.team_complete else R.string.team_has_room),
                R.drawable.ic_team,
            )
            StatCard(stringResource(R.string.stat_your_role), stringResource(if (isLeader) R.string.role_team_leader else R.string.role_team_member), Modifier.weight(1f), icon = R.drawable.ic_badge, textValue = true)
            StatCard(stringResource(R.string.stat_invites), team.pendingInvites.toString(), Modifier.weight(1f), icon = R.drawable.ic_mail)
        }
    }
    if (isLeader && team.members.size + team.pendingInvites < team.maxMembers) {
        item { PrimaryButton(stringResource(R.string.invite_member), onInvite, Modifier.fillMaxWidth(), enabled = !busy, icon = R.drawable.ic_person_add) }
    }
    item {
        GtCard(padding = 18.dp) {
            SectionTitle(stringResource(R.string.team_members))
            team.members.forEach { MemberRow(it, if (isLeader) onRemove else null) }
        }
    }
    item {
        GtCard {
            SectionTitle(stringResource(R.string.team_details))
            LabeledValue(stringResource(R.string.detail_project), team.projectTitle ?: stringResource(R.string.not_registered_yet))
            LabeledValue(stringResource(R.string.detail_supervisor), team.supervisorName ?: "—")
            LabeledValue(stringResource(R.string.detail_semester), team.semester ?: "—")
        }
    }
}

@Composable
private fun InvitationCard(invitation: Invitation, busy: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    val context = LocalContext.current
    GtCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(invitation.teamName, size = 44.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(invitation.teamName, style = GtType.Title)
                Text(stringResource(R.string.invited_by, invitation.leaderName, invitation.createdAt.asRelativeTime(context)), style = GtType.Caption)
            }
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(stringResource(R.string.decline), onDecline, Modifier.weight(1f), enabled = !busy)
            PrimaryButton(stringResource(R.string.accept), onAccept, Modifier.weight(1f), enabled = !busy)
        }
    }
}

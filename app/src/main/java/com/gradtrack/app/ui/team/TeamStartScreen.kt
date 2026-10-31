package com.gradtrack.app.ui.team

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.Resource
import com.gradtrack.app.data.model.Invitation
import com.gradtrack.app.data.model.MyTeam
import com.gradtrack.app.data.repository.AuthRepository
import com.gradtrack.app.data.repository.TeamRepository
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.LocalContainer
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.IconTile
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.EmptyState
import com.gradtrack.app.ui.components.PageHeader
import com.gradtrack.app.ui.components.SecondaryButton
import com.gradtrack.app.ui.components.TextAction
import com.gradtrack.app.ui.components.TextPromptDialog
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import kotlinx.coroutines.launch

/** First stop for a student without a team: start one (and lead it) or accept an invitation. */
class TeamStartViewModel(private val teams: TeamRepository, private val auth: AuthRepository) : LoadableViewModel<MyTeam>({ teams.myTeam() }) {
    fun create(name: String) =
        act({
            when (val created = teams.create(name)) {
                is Resource.Success -> auth.refreshUser()
                is Resource.Failure -> created
            }
        }) { send(UiEvent.Done(R.string.team_created)) }

    fun accept(invitation: Invitation) = act({ teams.accept(invitation.id) }) { send(UiEvent.Done(R.string.joined_team)) }

    fun decline(invitation: Invitation) = act({ teams.decline(invitation.id) }) { refresh() }
}

@Composable
fun TeamStartScreen(onReady: () -> Unit, onSignedOut: () -> Unit) {
    val container = LocalContainer.current
    val vm = appViewModel { TeamStartViewModel(it.team, it.auth) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val user by container.session.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var naming by remember { mutableStateOf(false) }
    var joining by rememberSaveable { mutableStateOf(false) }
    EventsEffect(vm) { onReady() }

    if (naming) {
        TextPromptDialog(stringResource(R.string.create_team), stringResource(R.string.hint_team_name), stringResource(R.string.create), { naming = false }, {
            naming = false
            vm.create(it)
        })
    }

    BackHandler(enabled = joining) { joining = false }

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { data ->
        if (joining) {
            item { PageHeader(stringResource(R.string.start_join_title), stringResource(R.string.start_overline), stringResource(R.string.start_invitations_subtitle)) }
            if (data.invitations.isEmpty()) {
                item {
                    GtCard {
                        EmptyState(R.drawable.ic_mail, stringResource(R.string.start_no_invitations_title), null)
                    }
                }
            } else {
                data.invitations.forEach { invitation ->
                    item(key = invitation.id) { InvitationCard(invitation, busy, onAccept = { vm.accept(invitation) }, onDecline = { vm.decline(invitation) }) }
                }
            }
            item { SecondaryButton(stringResource(R.string.back), { joining = false }, Modifier.fillMaxWidth(), icon = R.drawable.ic_arrow_back) }
            return@LoadableScreen
        }
        item {
            PageHeader(
                title = stringResource(R.string.start_title, user?.fullName?.substringBefore(' ').orEmpty()),
                overline = stringResource(R.string.start_overline),
                subtitle = stringResource(R.string.start_subtitle),
            )
        }
        item {
            ChoiceCard(R.drawable.ic_team, stringResource(R.string.start_create_title), stringResource(R.string.create_team)) {
                if (!busy) naming = true
            }
        }
        item {
            ChoiceCard(R.drawable.ic_mail, stringResource(R.string.start_join_title), stringResource(R.string.view_invitations)) {
                joining = true
                vm.refresh()
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                TextAction(stringResource(R.string.log_out), {
                    scope.launch {
                        container.auth.logout()
                        onSignedOut()
                    }
                })
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    @DrawableRes icon: Int,
    title: String,
    action: String,
    onClick: () -> Unit,
) {
    GtCard(padding = 20.dp, onClick = onClick) {
        IconTile(icon, size = 52.dp)
        Text(title, style = GtType.Headline, modifier = Modifier.padding(top = 16.dp))
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(action, style = GtType.Label.copy(color = Gt.Primary))
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Gt.Primary, modifier = Modifier.size(18.dp))
        }
    }
}

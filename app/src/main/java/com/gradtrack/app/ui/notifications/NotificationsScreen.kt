package com.gradtrack.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.model.NotificationItem
import com.gradtrack.app.data.repository.NotificationRepository
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.common.asRelativeTime
import com.gradtrack.app.ui.common.icon
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.components.EmptyState
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.IconTile
import com.gradtrack.app.ui.components.LoadableScreen
import com.gradtrack.app.ui.components.Pill
import com.gradtrack.app.ui.components.TextAction
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType

class NotificationsViewModel(private val repository: NotificationRepository) : LoadableViewModel<List<NotificationItem>>({ repository.list() }) {
    fun markAllRead() = act({ repository.markAllRead() }) { patch { list -> list.map { it.copy(read = true) } } }
}

@Composable
fun NotificationsScreen() {
    val vm = appViewModel { NotificationsViewModel(it.notifications) }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    EventsEffect(vm)

    LoadableScreen(state, onRetry = vm::load, refreshing = refreshing, onRefresh = vm::refresh) { list ->
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.title_notifications), style = GtType.Display, modifier = Modifier.weight(1f))
                if (list.any { !it.read }) TextAction(stringResource(R.string.mark_all_read), vm::markAllRead)
            }
        }
        if (list.isEmpty()) {
            item { EmptyState(R.drawable.ic_notifications, stringResource(R.string.empty_notifications_title), stringResource(R.string.no_notifications)) }
        }
        list.forEach { item(key = it.id) { NotificationCard(it) } }
    }
}

@Composable
private fun NotificationCard(item: NotificationItem) {
    val context = LocalContext.current
    GtCard(padding = 14.dp) {
        Row {
            IconTile(item.type.icon())
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = GtType.Label, modifier = Modifier.weight(1f))
                    if (!item.read) Box(Modifier.padding(start = 8.dp).size(8.dp).background(Gt.Primary, CircleShape))
                }
                Text(item.body, style = GtType.Body.copy(color = Gt.TextSecondary), modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(item.type.pill())
                    Text(item.createdAt.asRelativeTime(context), style = GtType.Caption, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}

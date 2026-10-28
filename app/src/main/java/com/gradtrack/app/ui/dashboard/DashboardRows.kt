package com.gradtrack.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gradtrack.app.R
import com.gradtrack.app.data.model.ActivityItem
import com.gradtrack.app.data.model.Deadline
import com.gradtrack.app.data.model.ProposalSummary
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.asRelativeTime
import com.gradtrack.app.ui.common.dueLabel
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.components.IconTile
import com.gradtrack.app.ui.components.Pill
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import com.gradtrack.app.ui.theme.Tone

@Composable
fun DeadlineRow(deadline: Deadline) {
    val context = LocalContext.current
    val label = deadline.date.dueLabel(context)
    val tone =
        when (label) {
            stringResource(R.string.due_overdue) -> Tone.Red
            stringResource(R.string.due_soon) -> Tone.Orange
            else -> Tone.Gray
        }
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(R.drawable.ic_calendar)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(deadline.title, style = GtType.Label)
            Text(deadline.date.asDate(), style = GtType.Caption)
        }
        if (label.isNotEmpty()) Pill(label, tone)
    }
}

@Composable
fun ActivityRow(item: ActivityItem) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Box(Modifier.padding(top = 6.dp).size(8.dp).background(Gt.Primary, CircleShape))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.title, style = GtType.Label)
            Text(item.body, style = GtType.Caption)
        }
        Text(item.createdAt.asRelativeTime(context), style = GtType.Caption, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun ProposalRow(proposal: ProposalSummary, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(proposal.title, style = GtType.Label)
            Text(stringResource(R.string.proposal_meta, proposal.teamName, proposal.submittedAt.asDate()), style = GtType.Caption)
        }
        Pill(proposal.status.pill(), Modifier.padding(start = 8.dp))
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Gt.TextSecondary, modifier = Modifier.padding(start = 4.dp).size(20.dp))
    }
}

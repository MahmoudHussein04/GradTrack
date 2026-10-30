package com.gradtrack.app.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Milestone
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.components.GtTextField
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneDialog(
    milestone: Milestone?,
    onDismiss: () -> Unit,
    onSave: (name: String, dueDate: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(milestone?.name.orEmpty()) }
    var due by remember { mutableStateOf(milestone?.dueDate) }
    var picking by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var dueError by remember { mutableStateOf<String?>(null) }
    val required = stringResource(R.string.error_required)
    val pickDeadline = stringResource(R.string.error_pick_deadline)

    if (picking) {
        val today = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = due?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= today

                        override fun isSelectableYear(year: Int) = year >= LocalDate.now().year
                    },
            )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        due = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        dueError = null
                    }
                    picking = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = pickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (milestone == null) R.string.add_milestone else R.string.edit_milestone), style = GtType.Title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GtTextField(name, {
                    name = it
                    nameError = null
                }, stringResource(R.string.field_milestone_name), error = nameError)
                Box {
                    GtTextField(due?.asDate().orEmpty(), {}, stringResource(R.string.field_deadline), enabled = false, error = dueError, leadingIcon = R.drawable.ic_calendar)
                    Box(Modifier.matchParentSize().padding(bottom = 4.dp)) {
                        TextButton(onClick = { picking = true }, modifier = Modifier.fillMaxSize()) {}
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete_milestone), style = GtType.Label.copy(color = Gt.Error)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                nameError = if (name.isBlank()) required else null
                dueError = if (due == null) pickDeadline else null
                val date = due
                if (nameError == null && date != null) onSave(name.trim(), date)
            }) { Text(stringResource(R.string.save), style = GtType.Label.copy(color = Gt.Primary)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), style = GtType.Label.copy(color = Gt.TextSecondary)) } },
        containerColor = Gt.Surface,
    )
}

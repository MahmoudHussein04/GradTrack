package com.gradtrack.app.ui.profile

import android.util.Patterns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.UpdateProfileRequest
import com.gradtrack.app.data.repository.AuthRepository
import com.gradtrack.app.ui.common.BaseViewModel
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LocalContainer
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.components.Avatar
import com.gradtrack.app.ui.components.ConfirmDialog
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.GtTextField
import com.gradtrack.app.ui.components.Pill
import com.gradtrack.app.ui.components.PrimaryButton
import com.gradtrack.app.ui.components.ScreenList
import com.gradtrack.app.ui.components.SecondaryButton
import com.gradtrack.app.ui.components.SectionTitle
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import com.gradtrack.app.ui.theme.Tone
import kotlinx.coroutines.launch

class ProfileViewModel(private val auth: AuthRepository) : BaseViewModel() {
    fun save(request: UpdateProfileRequest) = act({ auth.updateProfile(request) }) { send(UiEvent.Message(R.string.profile_saved)) }

    fun changePassword(current: String, new: String) = act({ auth.changePassword(current, new) }) { send(UiEvent.Done(R.string.password_updated)) }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.logout()
            onDone()
        }
    }
}

@Composable
fun ProfileScreen(onSignedOut: () -> Unit) {
    val vm = appViewModel { ProfileViewModel(it.auth) }
    val busy by vm.busy.collectAsStateWithLifecycle()
    val user by LocalContainer.current.session.currentUser.collectAsStateWithLifecycle()
    val me = user ?: return
    val supervisor = me.role == Role.SUPERVISOR

    var fullName by rememberSaveable { mutableStateOf(me.fullName) }
    var email by rememberSaveable { mutableStateOf(me.email) }
    var department by rememberSaveable { mutableStateOf(me.department.orEmpty()) }
    var academicYear by rememberSaveable { mutableStateOf(me.academicYear.orEmpty()) }
    var current by rememberSaveable { mutableStateOf("") }
    var new by rememberSaveable { mutableStateOf("") }
    var showPasswords by rememberSaveable { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var currentError by remember { mutableStateOf<String?>(null) }
    var newError by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    val required = stringResource(R.string.error_required)
    val badEmail = stringResource(R.string.error_email)
    val shortPassword = stringResource(R.string.error_password_short)

    EventsEffect(vm) { event ->
        if (event.message == R.string.password_updated) {
            current = ""
            new = ""
        }
    }

    if (confirmLogout) {
        ConfirmDialog(stringResource(R.string.log_out), stringResource(R.string.log_out_confirm), stringResource(R.string.log_out), { confirmLogout = false }, {
            confirmLogout = false
            vm.logout(onSignedOut)
        }, danger = true)
    }

    ScreenList {
        item {
            GtCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(me.fullName, me.avatarUrl, size = 64.dp)
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(me.fullName, style = GtType.Title)
                        if (me.email.isNotBlank()) Text(me.email, style = GtType.Caption)
                        Pill(
                            stringResource(
                                when (me.role) {
                                    Role.STUDENT -> R.string.role_student
                                    Role.LEADER -> R.string.role_team_leader
                                    Role.SUPERVISOR -> R.string.role_supervisor_full
                                },
                            ),
                            Tone.Green,
                            Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
        item {
            GtCard {
                SectionTitle(stringResource(R.string.personal_information), Modifier.padding(bottom = 8.dp))
                GtTextField(fullName, {
                    fullName = it
                    nameError = null
                }, stringResource(R.string.field_full_name), error = nameError, leadingIcon = R.drawable.ic_person)
                GtTextField(me.universityId.orEmpty(), {}, stringResource(R.string.hint_university_id_label), enabled = false, leadingIcon = R.drawable.ic_badge)
                GtTextField(email, {
                    email = it
                    emailError = null
                }, stringResource(R.string.field_email), error = emailError, keyboardType = KeyboardType.Email, leadingIcon = R.drawable.ic_mail)
                GtTextField(department, { department = it }, stringResource(R.string.field_department))
                if (!supervisor) GtTextField(academicYear, { academicYear = it }, stringResource(R.string.field_academic_year))
                PrimaryButton(
                    stringResource(R.string.save_changes),
                    {
                        nameError = if (fullName.isBlank()) required else null
                        emailError = if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) badEmail else null
                        if (nameError == null && emailError == null) {
                            vm.save(UpdateProfileRequest(fullName.trim(), email.trim(), department.trim(), if (supervisor) "" else academicYear.trim()))
                        }
                    },
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    loading = busy,
                )
            }
        }
        item {
            GtCard {
                SectionTitle(stringResource(R.string.security))
                Text(stringResource(R.string.security_hint), style = GtType.Caption, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                val transformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation()
                val toggle: @Composable () -> Unit = {
                    IconButton(onClick = { showPasswords = !showPasswords }) {
                        Icon(painterResource(if (showPasswords) R.drawable.ic_visibility_off else R.drawable.ic_visibility), null, tint = Gt.TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
                GtTextField(current, {
                    current = it
                    currentError = null
                }, stringResource(R.string.field_current_password), error = currentError, keyboardType = KeyboardType.Password, visualTransformation = transformation, leadingIcon = R.drawable.ic_lock, trailing = toggle)
                GtTextField(new, {
                    new = it
                    newError = null
                }, stringResource(R.string.field_new_password), error = newError, keyboardType = KeyboardType.Password, visualTransformation = transformation, leadingIcon = R.drawable.ic_lock)
                PrimaryButton(
                    stringResource(R.string.update_password),
                    {
                        currentError = if (current.isEmpty()) required else null
                        newError = if (new.length < 8) shortPassword else null
                        if (currentError == null && newError == null) vm.changePassword(current, new)
                    },
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !busy,
                )
            }
        }
        item { SecondaryButton(stringResource(R.string.log_out), { confirmLogout = true }, Modifier.fillMaxWidth(), icon = R.drawable.ic_logout, danger = true) }
    }
}

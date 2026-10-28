package com.gradtrack.app.ui.login

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.repository.AuthRepository
import com.gradtrack.app.ui.common.BaseViewModel
import com.gradtrack.app.ui.common.EventsEffect
import com.gradtrack.app.ui.common.LocalSnackbar
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.appViewModel
import com.gradtrack.app.ui.components.GtCard
import com.gradtrack.app.ui.components.GtTextField
import com.gradtrack.app.ui.components.HeroCard
import com.gradtrack.app.ui.components.IconTile
import com.gradtrack.app.ui.components.PrimaryButton
import com.gradtrack.app.ui.components.TextAction
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(private val auth: AuthRepository) : BaseViewModel() {
    private val _role = MutableStateFlow(Role.STUDENT)

    val role: StateFlow<Role> = _role.asStateFlow()

    fun select(role: Role) {
        _role.value = role
    }

    fun login(identifier: String, password: String) = act({ auth.login(identifier, password, role.value) }) { send(UiEvent.Done()) }
}

@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
    val vm = appViewModel { LoginViewModel(it.auth) }
    val role by vm.role.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var idError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    val required = stringResource(R.string.error_required)
    val forgotText = stringResource(R.string.forgot_password_soon)

    EventsEffect(vm) { onSignedIn() }

    fun submit() {
        idError = if (identifier.isBlank()) required else null
        passwordError = if (password.isEmpty()) required else null
        if (idError == null && passwordError == null) vm.login(identifier.trim(), password)
    }

    Column(Modifier.fillMaxSize().background(Gt.Background).verticalScroll(scroll)) {
        Box(Modifier.fillMaxWidth().height(380.dp).background(Gt.Hero)) {
            Box(Modifier.size(260.dp).offset(x = 220.dp, y = (-60).dp).background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(50)))
            Box(Modifier.size(160.dp).offset(x = (-70).dp, y = 200.dp).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(50)))
            Column(Modifier.statusBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).background(Gt.GlassFill, RoundedCornerShape(12.dp)).border(1.dp, Gt.GlassStroke, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(painterResource(R.drawable.ic_school), null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                    Text(stringResource(R.string.app_name), style = GtType.Title.copy(color = Color.White, fontSize = 22.sp), modifier = Modifier.padding(start = 12.dp))
                }
                Text(stringResource(R.string.login_hero), style = GtType.Display.copy(color = Color.White, fontSize = 26.sp, lineHeight = 32.sp), modifier = Modifier.padding(top = 24.dp))
                Text(stringResource(R.string.login_hero_body), style = GtType.Caption.copy(color = Gt.Muted, fontSize = 13.sp, lineHeight = 18.sp), modifier = Modifier.padding(top = 8.dp))
                Row(
                    Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth()
                        .background(Gt.GlassFill, RoundedCornerShape(18.dp))
                        .border(1.dp, Gt.GlassStroke, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(32.dp).background(Gt.GlassFill, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_flag), null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text(stringResource(R.string.login_hero_card), style = GtType.Body.copy(color = Color.White, fontSize = 13.sp), modifier = Modifier.padding(start = 12.dp))
                }
            }
        }

        Column(
            Modifier
                .offset(y = (-30).dp)
                .fillMaxWidth()
                .background(Gt.Background, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(24.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.login_welcome), style = GtType.Display)
            Text(stringResource(R.string.login_subtitle), style = GtType.PageSubtitle, modifier = Modifier.padding(top = 4.dp))

            Text(stringResource(role.loginTitle()), style = GtType.Title, modifier = Modifier.padding(top = 22.dp))
            Text(stringResource(role.loginHelper()), style = GtType.Caption, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))

            GtTextField(
                value = identifier,
                onValueChange = {
                    identifier = it
                    idError = null
                },
                label = stringResource(R.string.hint_university_id),
                error = idError,
                keyboardType = KeyboardType.Number,
                leadingIcon = R.drawable.ic_badge,
            )
            Spacer(Modifier.height(8.dp))
            GtTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = stringResource(R.string.hint_password),
                error = passwordError,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = R.drawable.ic_lock,
                trailing = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(painterResource(if (showPassword) R.drawable.ic_visibility_off else R.drawable.ic_visibility), stringResource(R.string.hint_password), tint = Gt.TextSecondary, modifier = Modifier.size(20.dp))
                    }
                },
            )
            TextAction(stringResource(R.string.forgot_password), onClick = { scope.launch { snackbar.showSnackbar(forgotText) } }, modifier = Modifier.align(Alignment.End))
            PrimaryButton(stringResource(role.loginButton()), ::submit, modifier = Modifier.fillMaxWidth(), loading = busy)
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp).alpha(if (busy) 1f else 0f), color = Gt.Primary, trackColor = Gt.Track)

            Text(stringResource(R.string.or_continue_as), style = GtType.Title, modifier = Modifier.padding(top = 18.dp))
            Text(
                stringResource(if (role == Role.STUDENT) R.string.or_continue_as_hint else R.string.or_continue_as_switch),
                style = GtType.Caption,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Role.entries.filter { it != role }.forEach { other ->
                    RoleOption(other, Modifier.weight(1f)) {
                        identifier = ""
                        password = ""
                        idError = null
                        passwordError = null
                        vm.select(other)
                        scope.launch { scroll.animateScrollTo(0) }
                    }
                }
            }
            Text(
                stringResource(R.string.login_footer),
                style = GtType.Caption,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
        }
    }
}

@Composable
private fun RoleOption(role: Role, modifier: Modifier, onClick: () -> Unit) {
    val (icon, title) = role.option()
    GtCard(modifier, padding = 14.dp, onClick = onClick) {
        IconTile(icon, size = 36.dp)
        Text(stringResource(title), style = GtType.Title, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sign_in), style = GtType.Label.copy(color = Gt.Primary))
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Gt.Primary, modifier = Modifier.size(18.dp))
        }
    }
}

private fun Role.option(): Pair<Int, Int> =
    when (this) {
        Role.STUDENT -> R.drawable.ic_school to R.string.role_student
        Role.LEADER -> R.drawable.ic_team to R.string.role_team_leader
        Role.SUPERVISOR -> R.drawable.ic_proposals to R.string.role_supervisor
    }

@StringRes
private fun Role.loginTitle() =
    when (this) {
        Role.STUDENT -> R.string.login_title_student
        Role.LEADER -> R.string.login_title_leader
        Role.SUPERVISOR -> R.string.login_title_supervisor
    }

@StringRes
private fun Role.loginHelper() =
    when (this) {
        Role.STUDENT -> R.string.login_helper_student
        Role.LEADER -> R.string.login_helper_leader
        Role.SUPERVISOR -> R.string.login_helper_supervisor
    }

@StringRes
private fun Role.loginButton() =
    when (this) {
        Role.STUDENT -> R.string.login_as_student
        Role.LEADER -> R.string.login_as_leader
        Role.SUPERVISOR -> R.string.login_as_supervisor
    }

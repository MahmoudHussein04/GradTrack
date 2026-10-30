package com.gradtrack.app.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gradtrack.app.AppContainer
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.User
import com.gradtrack.app.ui.common.LocalContainer
import com.gradtrack.app.ui.common.LocalSnackbar
import com.gradtrack.app.ui.common.LocalTabBarSpace
import com.gradtrack.app.ui.components.Avatar
import com.gradtrack.app.ui.dashboard.StudentDashboardScreen
import com.gradtrack.app.ui.dashboard.SupervisorDashboardScreen
import com.gradtrack.app.ui.glass.GlassTab
import com.gradtrack.app.ui.glass.GlassTabBar
import com.gradtrack.app.ui.login.LoginScreen
import com.gradtrack.app.ui.notifications.NotificationsScreen
import com.gradtrack.app.ui.profile.ProfileScreen
import com.gradtrack.app.ui.project.ProjectDetailsScreen
import com.gradtrack.app.ui.project.ProposalStatusScreen
import com.gradtrack.app.ui.project.RegisterProjectScreen
import com.gradtrack.app.ui.review.ProposalListScreen
import com.gradtrack.app.ui.review.ProposalReviewScreen
import com.gradtrack.app.ui.review.SubmissionReviewScreen
import com.gradtrack.app.ui.tasks.TaskDetailsScreen
import com.gradtrack.app.ui.tasks.TasksScreen
import com.gradtrack.app.ui.team.TeamScreen
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

object Routes {
    const val LOGIN = "login"
    const val STUDENT_HOME = "student/home"
    const val PROJECT = "project"
    const val TASKS = "tasks"
    const val ALERTS = "alerts"
    const val PROFILE = "profile"
    const val SUPERVISOR_HOME = "supervisor/home"
    const val PROPOSALS = "proposals"
    const val SUBMISSIONS = "submissions"
    const val REGISTER_PROJECT = "project/register"
    const val PROPOSAL_STATUS = "project/proposal"
    const val TEAM = "team"
    const val PROPOSAL_REVIEW = "proposals/{id}"
    const val TASK = "tasks/{id}"

    fun proposalReview(id: String) = "proposals/$id"

    fun task(id: String) = "tasks/$id"
}

private val studentTabs =
    listOf(
        GlassTab(Routes.STUDENT_HOME, R.drawable.ic_home, R.string.nav_home),
        GlassTab(Routes.PROJECT, R.drawable.ic_project, R.string.nav_project),
        GlassTab(Routes.TASKS, R.drawable.ic_check_circle, R.string.nav_tasks),
        GlassTab(Routes.ALERTS, R.drawable.ic_notifications, R.string.nav_alerts),
        GlassTab(Routes.PROFILE, R.drawable.ic_person, R.string.nav_profile),
    )

private val supervisorTabs =
    listOf(
        GlassTab(Routes.SUPERVISOR_HOME, R.drawable.ic_home, R.string.nav_home),
        GlassTab(Routes.PROPOSALS, R.drawable.ic_proposals, R.string.nav_proposals),
        GlassTab(Routes.SUBMISSIONS, R.drawable.ic_submissions, R.string.nav_submissions),
        GlassTab(Routes.ALERTS, R.drawable.ic_notifications, R.string.nav_alerts),
        GlassTab(Routes.PROFILE, R.drawable.ic_person, R.string.nav_profile),
    )

@StringRes
private fun titleOf(route: String?): Int? =
    when (route) {
        Routes.REGISTER_PROJECT -> R.string.title_register_project
        Routes.PROPOSAL_STATUS -> R.string.title_proposal_status
        Routes.TEAM -> R.string.title_team
        Routes.PROPOSAL_REVIEW -> R.string.title_proposal_review
        Routes.TASK -> R.string.title_task
        else -> null
    }

fun homeRoute(user: User?) =
    when (user?.role) {
        null -> Routes.LOGIN
        Role.SUPERVISOR -> Routes.SUPERVISOR_HOME
        else -> Routes.STUDENT_HOME
    }

private fun NavHostController.openTab(route: String, home: String) {
    if (route == home) {
        popBackStack(home, inclusive = false, saveState = true)
        return
    }
    navigate(route) {
        popUpTo(home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavHostController.restartAt(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
    (studentTabs + supervisorTabs).map { it.route }.distinct().filter { it != route }.forEach { clearBackStack(it) }
}

@Composable
fun GradTrackApp(container: AppContainer) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backdrop = rememberLayerBackdrop()
    val resources = LocalResources.current
    val user by container.session.currentUser.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val start = remember { homeRoute(container.session.user) }

    val tabs = if (user?.role == Role.SUPERVISOR) supervisorTabs else studentTabs
    val tabIndex = tabs.indexOfFirst { it.route == route }
    val showTabs = user != null && tabIndex >= 0
    val go: (String) -> Unit = { target ->
        if (tabs.any { it.route == target }) nav.openTab(target, home = tabs.first().route) else nav.navigate(target)
    }
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(Unit) {
        container.session.expired.collect {
            nav.restartAt(Routes.LOGIN)
            snackbar.showSnackbar(resources.getString(R.string.error_session_expired))
        }
    }

    CompositionLocalProvider(
        LocalContainer provides container,
        LocalSnackbar provides snackbar,
        LocalTabBarSpace provides if (showTabs) 94.dp + navBar else navBar,
    ) {
        Column(Modifier.fillMaxSize().background(Gt.Background)) {
            if (route != null && route != Routes.LOGIN) {
                TopBar(
                    title = titleOf(route)?.let { stringResource(it) },
                    canGoBack = titleOf(route) != null,
                    onBack = { nav.popBackStack() },
                    user = user,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop)
                        .background(Gt.Background)
                        .imePadding(),
                ) {
                    NavHost(
                        navController = nav,
                        startDestination = start,
                        enterTransition = { fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.98f) },
                        exitTransition = { fadeOut(tween(150)) },
                        popEnterTransition = { fadeIn(tween(220)) },
                        popExitTransition = { fadeOut(tween(150)) },
                    ) {
                        composable(Routes.LOGIN) { LoginScreen(onSignedIn = { nav.restartAt(homeRoute(container.session.user)) }) }
                        composable(Routes.STUDENT_HOME) { StudentDashboardScreen(onNavigate = go) }
                        composable(Routes.PROJECT) { ProjectDetailsScreen(onNavigate = go) }
                        composable(Routes.TASKS) { TasksScreen(onNavigate = go) }
                        composable(Routes.ALERTS) { NotificationsScreen() }
                        composable(Routes.PROFILE) { ProfileScreen(onSignedOut = { nav.restartAt(Routes.LOGIN) }) }
                        composable(Routes.SUPERVISOR_HOME) { SupervisorDashboardScreen(onNavigate = go) }
                        composable(Routes.PROPOSALS) { ProposalListScreen(onOpen = { nav.navigate(Routes.proposalReview(it)) }) }
                        composable(Routes.SUBMISSIONS) { SubmissionReviewScreen() }
                        composable(Routes.REGISTER_PROJECT) {
                            RegisterProjectScreen(
                                onSubmitted = {
                                    nav.navigate(Routes.PROPOSAL_STATUS) { popUpTo(Routes.REGISTER_PROJECT) { inclusive = true } }
                                },
                                onNavigate = go,
                            )
                        }
                        composable(Routes.PROPOSAL_STATUS) { ProposalStatusScreen(onNavigate = go) }
                        composable(Routes.TEAM) { TeamScreen() }
                        composable(Routes.PROPOSAL_REVIEW) { backStack ->
                            ProposalReviewScreen(proposalId = backStack.arguments?.getString("id").orEmpty())
                        }
                        composable(Routes.TASK) { backStack -> TaskDetailsScreen(taskId = backStack.arguments?.getString("id").orEmpty()) }
                    }
                }

                TabBarHost(showTabs, Modifier.align(Alignment.BottomCenter)) {
                    GlassTabBar(
                        tabs = tabs,
                        selectedIndex = tabIndex,
                        onSelect = { nav.openTab(tabs[it].route, home = tabs.first().route) },
                        backdrop = backdrop,
                        modifier = Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                    )
                }

                SnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (showTabs) 94.dp + navBar else navBar + 8.dp),
                ) { data ->
                    Snackbar(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Gt.PrimaryDark,
                        contentColor = Color.White,
                    ) { Text(data.visuals.message, style = GtType.Body.copy(color = Color.White)) }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String?,
    canGoBack: Boolean,
    onBack: () -> Unit,
    user: User?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Gt.PrimaryDark)
            .statusBarsPadding()
            .height(64.dp)
            .padding(start = if (canGoBack) 4.dp else 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canGoBack) {
            IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back), tint = Color.White) }
            Text(title.orEmpty(), style = GtType.Title.copy(color = Color.White, fontSize = 19.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        } else {
            Icon(painterResource(R.drawable.ic_school), null, tint = Gt.PrimaryContainer, modifier = Modifier.size(30.dp))
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(stringResource(R.string.app_name), style = GtType.Title.copy(color = Color.White, fontSize = 19.sp))
                Text(stringResource(R.string.brand_tagline), style = GtType.Caption.copy(color = Gt.Muted, fontSize = 10.sp), maxLines = 1)
            }
        }
        if (user != null) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp).width(120.dp)) {
                Text(user.fullName, style = GtType.Label.copy(color = Color.White, fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(
                        when (user.role) {
                            Role.STUDENT -> R.string.role_student
                            Role.LEADER -> R.string.role_team_leader
                            Role.SUPERVISOR -> R.string.role_supervisor_full
                        },
                    ),
                    style = GtType.Caption.copy(color = Gt.Muted, fontSize = 10.sp),
                    maxLines = 1,
                )
            }
            Avatar(user.fullName, user.avatarUrl, size = 36.dp)
        } else {
            Spacer(Modifier.width(1.dp))
        }
    }
}

@Composable
private fun TabBarHost(visible: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) { content() }
}

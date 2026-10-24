package com.gradtrack.app.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions
import com.gradtrack.app.ui.glass.GlassTab
import com.gradtrack.app.ui.glass.GlassTabBar
import com.gradtrack.app.ui.glass.ViewBackdrop
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.gradtrack.app.GradTrackApp
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.User
import com.gradtrack.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/** Single activity: toolbar + NavHost + role-dependent liquid glass tab bar. Every screen is a Fragment in nav_graph. */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfig: AppBarConfiguration

    // Liquid glass tab bar state, read by the Compose bar.
    private val tabs = mutableStateOf<List<GlassTab>>(emptyList())
    private val selectedTab = mutableIntStateOf(-1)
    private val tabBarVisible = mutableStateOf(false)
    private val screenFrame = mutableIntStateOf(0)

    private val studentTabs =
        listOf(
            GlassTab(R.id.studentDashboardFragment, R.drawable.ic_home, R.string.nav_home),
            GlassTab(R.id.projectDetailsFragment, R.drawable.ic_project, R.string.nav_project),
            GlassTab(R.id.tasksFragment, R.drawable.ic_check_circle, R.string.nav_tasks),
            GlassTab(R.id.notificationsFragment, R.drawable.ic_notifications, R.string.nav_alerts),
            GlassTab(R.id.profileFragment, R.drawable.ic_person, R.string.nav_profile),
        )
    private val supervisorTabs =
        listOf(
            GlassTab(R.id.supervisorDashboardFragment, R.drawable.ic_home, R.string.nav_home),
            GlassTab(R.id.proposalListFragment, R.drawable.ic_proposals, R.string.nav_proposals),
            GlassTab(R.id.submissionReviewFragment, R.drawable.ic_submissions, R.string.nav_submissions),
            GlassTab(R.id.notificationsFragment, R.drawable.ic_notifications, R.string.nav_alerts),
            GlassTab(R.id.profileFragment, R.drawable.ic_person, R.string.nav_profile),
        )

    private val topLevelIds =
        setOf(
            R.id.studentDashboardFragment,
            R.id.projectDetailsFragment,
            R.id.tasksFragment,
            R.id.notificationsFragment,
            R.id.profileFragment,
            R.id.supervisorDashboardFragment,
            R.id.proposalListFragment,
            R.id.submissionReviewFragment,
            R.id.loginFragment,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        // The top of every screen is dark green (header bar / login hero), so the status-bar icons are light.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val container = (application as GradTrackApp).container
        val navHost = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHost.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(
            when (container.session.user?.role) {
                null -> R.id.loginFragment
                Role.SUPERVISOR -> R.id.supervisorDashboardFragment
                else -> R.id.studentDashboardFragment
            },
        )
        navController.setGraph(graph, null)

        appBarConfig = AppBarConfiguration(topLevelIds)
        setupActionBarWithNavController(navController, appBarConfig)

        setUpGlassBar()

        navController.addOnDestinationChangedListener { controller, destination, _ ->
            val isLogin = destination.id == R.id.loginFragment
            if (destination.id == R.id.studentDashboardFragment || destination.id == R.id.supervisorDashboardFragment) {
                // Bottom-navigation switches pop back to the start destination: keep it pointing at the signed-in role's home.
                controller.graph.setStartDestination(destination.id)
            }
            tabs.value = if (container.session.user?.role == Role.SUPERVISOR) supervisorTabs else studentTabs
            val tabIndex = tabs.value.indexOfFirst { it.destination == destination.id }
            val topLevel = !isLogin && tabIndex >= 0
            // On login only the dark status-bar strip remains, so the hero below it reads as one block.
            binding.toolbar.isVisible = !isLogin
            val atRoot = destination.id in topLevelIds
            binding.brandBlock.isVisible = atRoot
            supportActionBar?.setDisplayShowTitleEnabled(!atRoot)
            bindUser(container.session.user)
            tabBarVisible.value = topLevel
            if (tabIndex >= 0) selectedTab.intValue = tabIndex
            applyInsets()
        }

        // Screens under the floating tab bar get extra bottom padding (clipToPadding off) so the last item can scroll
        // clear of the glass while content still passes beneath it; floating buttons are lifted above the bar.
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                    if (f is NavHostFragment) return
                    val underTabBar = tabs.value.any { it.destination == navController.currentDestination?.id }
                    if (underTabBar) padScrollables(v, dp(96) + (lastInsets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0))
                }
            },
            true,
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            lastInsets = insets
            applyInsets()
            insets
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.session.expired.collect {
                    Toast.makeText(this@MainActivity, R.string.error_session_expired, Toast.LENGTH_LONG).show()
                    navController.navigate(R.id.action_global_login)
                }
            }
        }
    }

    private var lastInsets: WindowInsetsCompat? = null

    private fun bindUser(user: User?) {
        binding.userChip.isVisible = user != null
        if (user == null) return
        binding.userName.text = user.fullName
        binding.userRole.setText(
            when (user.role) {
                Role.STUDENT -> R.string.role_student
                Role.LEADER -> R.string.role_team_leader
                Role.SUPERVISOR -> R.string.role_supervisor_full
            },
        )
        binding.userAvatar.bind(user.fullName, user.avatarUrl)
    }

    /** Edge-to-edge: the toolbar takes the status-bar inset, the nav bar its own; without them the content does. */
    private fun applyInsets() {
        val insets = lastInsets ?: return
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        binding.appBar.updatePadding(top = bars.top)
        binding.navHost.updatePadding(bottom = if (tabBarVisible.value) 0 else maxOf(bars.bottom, ime.bottom))
        binding.glassBar.updatePadding(bottom = bars.bottom)
        binding.root.updatePadding(left = bars.left, right = bars.right)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    /** Keeps a screen usable under the floating glass tab bar: scrollables get bottom room, floating buttons sit above it. */
    private fun padScrollables(view: View, extra: Int) {
        when (view) {
            is ExtendedFloatingActionButton, is FloatingActionButton ->
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += extra }
            is NestedScrollView, is ScrollView, is RecyclerView -> {
                (view as ViewGroup).clipToPadding = false
                view.updatePadding(bottom = view.paddingBottom + extra)
                return
            }
            is ViewGroup -> for (i in 0 until view.childCount) padScrollables(view.getChildAt(i), extra)
        }
    }

    private fun setUpGlassBar() {
        // Re-record the glass only when the screen under it actually changed (scroll, new content, animation);
        // redrawing the bar itself does not dirty the NavHost, so this cannot loop.
        binding.navHost.viewTreeObserver.addOnPreDrawListener {
            if (tabBarVisible.value && binding.navHost.isDirty) {
                screenFrame.intValue++
                Snapshot.sendApplyNotifications()
            }
            true
        }
        val pageColor = ContextCompat.getColor(this, R.color.background)
        binding.glassBar.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.glassBar.setContent {
            val backdrop = remember { ViewBackdrop(binding.navHost, pageColor, screenFrame) }
            AnimatedVisibility(
                visible = tabBarVisible.value,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                GlassTabBar(
                    tabs = tabs.value,
                    selectedIndex = selectedTab.intValue,
                    onSelect = { index -> openTab(tabs.value[index].destination) },
                    backdrop = backdrop,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                )
            }
        }
    }

    /** Same back-stack behaviour as NavigationUI for bottom navigation: one copy per tab, state saved and restored. */
    private fun openTab(destination: Int) {
        if (navController.currentDestination?.id == destination) return
        navController.navigate(
            destination,
            null,
            navOptions {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            },
        )
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
}

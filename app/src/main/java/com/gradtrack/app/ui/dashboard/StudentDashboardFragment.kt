package com.gradtrack.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.navigation.fragment.findNavController
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.StudentDashboard
import com.gradtrack.app.data.repository.DashboardRepository
import com.gradtrack.app.databinding.FragmentStudentDashboardBinding
import com.gradtrack.app.databinding.ViewHeroBinding
import com.gradtrack.app.ui.common.GradFragment
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.container
import com.gradtrack.app.ui.common.greetingName
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.setVisible
import com.gradtrack.app.ui.common.pill
import java.time.LocalTime

class StudentDashboardViewModel(repository: DashboardRepository) : LoadableViewModel<StudentDashboard>({ repository.student() })

/** Shows the "next step" variant of the dashboard hero: an icon, a reason and one call to action. */
fun ViewHeroBinding.showNextStep(@DrawableRes icon: Int, title: String, body: String, action: String, onAction: () -> Unit) {
    heroProject.setVisible(false)
    heroEmpty.setVisible(true)
    heroEmptyIcon.setImageResource(icon)
    heroEmptyTitle.text = title
    heroEmptyBody.text = body
    heroEmptyAction.text = action
    heroEmptyAction.setOnClickListener { onAction() }
}

class StudentDashboardFragment : GradFragment() {
    private var _binding: FragmentStudentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { StudentDashboardViewModel(it.dashboard) }
    private val deadlineAdapter = DeadlineAdapter()
    private val activityAdapter = ActivityAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val user = container.session.user
        val name = user?.greetingName()
        val hour = LocalTime.now().hour
        binding.greeting.text =
            when {
                name == null -> getString(if (hour < 12) R.string.greeting_morning_plain else if (hour < 18) R.string.greeting_afternoon_plain else R.string.greeting_evening_plain)
                hour < 12 -> getString(R.string.greeting_morning, name)
                hour < 18 -> getString(R.string.greeting_afternoon, name)
                else -> getString(R.string.greeting_evening, name)
            }
        binding.workspace.setText(if (user?.role == Role.LEADER) R.string.leader_workspace else R.string.student_workspace)

        binding.deadlines.adapter = deadlineAdapter
        binding.activity.adapter = activityAdapter
        binding.refresh.setOnRefreshListener { viewModel.refresh() }

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) bind(state.data)
        }
        collect(viewModel.refreshing) { binding.refresh.isRefreshing = it }
        observeEvents(viewModel)
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.state.value is UiState.Success) viewModel.refresh()
    }

    private fun bind(data: StudentDashboard) {
        val nav = findNavController()
        val user = container.session.user
        val isLeader = user?.role == Role.LEADER
        val hasTeam = data.teamMembers > 0
        val project = data.project
        val hero = binding.heroCard

        when {
            project != null -> {
                hero.heroProject.setVisible(true)
                hero.heroEmpty.setVisible(false)
                hero.heroStatus.setText(project.status.pill().label)
                hero.heroTitle.text = project.title
                hero.heroMeta.text = listOfNotNull(project.category, project.supervisorName).joinToString(" · ")
                hero.heroPercent.text = getString(R.string.percent, project.progress)
                hero.heroRing.setProgressCompat(project.progress, true)
                hero.heroMilestones.text = getString(R.string.milestones_completed, project.completedMilestones, project.totalMilestones)
                hero.heroAction.setText(R.string.open_project)
                hero.heroAction.setOnClickListener { nav.navigate(R.id.projectDetailsFragment) }
            }
            !hasTeam && isLeader ->
                hero.showNextStep(R.drawable.ic_team, getString(R.string.next_create_team_title), getString(R.string.next_create_team_body), getString(R.string.create_team)) {
                    nav.navigate(R.id.teamFragment)
                }
            !hasTeam ->
                hero.showNextStep(
                    R.drawable.ic_person_add,
                    getString(R.string.next_join_team_title),
                    getString(R.string.next_join_team_body, user?.universityId.orEmpty()),
                    getString(R.string.view_invitations),
                ) { nav.navigate(R.id.teamFragment) }
            isLeader ->
                hero.showNextStep(R.drawable.ic_proposals, getString(R.string.next_register_title), getString(R.string.next_register_body), getString(R.string.register_project)) {
                    nav.navigate(R.id.registerProjectFragment)
                }
            else ->
                hero.showNextStep(R.drawable.ic_clock, getString(R.string.next_wait_project_title), getString(R.string.next_wait_project_body), getString(R.string.view_team)) {
                    nav.navigate(R.id.teamFragment)
                }
        }

        binding.statDeadlines.bind(getString(R.string.stat_deadlines_short), data.upcomingDeadlines.toString(), getString(R.string.stat_next_14_days), R.drawable.ic_calendar)
        binding.statFeedback.bind(getString(R.string.stat_feedback_short), data.supervisorFeedback.toString(), getString(R.string.stat_from_supervisor), R.drawable.ic_feedback)
        binding.statTeam.bind(getString(R.string.stat_team_short), data.teamMembers.toString(), getString(R.string.stat_members), R.drawable.ic_team)

        deadlineAdapter.submitList(data.deadlines)
        binding.deadlinesEmpty.setVisible(data.deadlines.isEmpty())
        activityAdapter.submitList(data.activity)
        binding.activityEmpty.setVisible(data.activity.isEmpty())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

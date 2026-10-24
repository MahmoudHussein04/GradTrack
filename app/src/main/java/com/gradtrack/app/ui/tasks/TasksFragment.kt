package com.gradtrack.app.ui.tasks

import com.gradtrack.app.ui.common.GradFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import android.content.res.ColorStateList
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gradtrack.app.R
import com.gradtrack.app.data.Resource
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.TaskItem
import com.gradtrack.app.data.model.TaskRequest
import com.gradtrack.app.data.model.TaskStatus
import com.gradtrack.app.data.model.TeamMember
import com.gradtrack.app.data.model.User
import com.gradtrack.app.data.repository.TaskRepository
import com.gradtrack.app.data.repository.TeamRepository
import com.gradtrack.app.databinding.DialogTaskBinding
import com.gradtrack.app.databinding.FragmentTasksBinding
import com.gradtrack.app.databinding.ItemTaskBinding
import com.gradtrack.app.ui.common.IdDiff
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.applyPill
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.asShortDate
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.container
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.common.setVisible
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneOffset

data class TasksData(val tasks: List<TaskItem>, val members: List<TeamMember>)

class TasksViewModel(
    private val repository: TaskRepository,
    teams: TeamRepository,
    isLeader: Boolean,
) : LoadableViewModel<TasksData>({
        val tasks = repository.tasks()
        // Only the leader assigns tasks, so only the leader needs the member list.
        val members = if (isLeader) teams.myTeam() else null
        when {
            tasks is Resource.Failure -> tasks
            members is Resource.Failure -> members
            else -> Resource.Success(TasksData((tasks as Resource.Success).data, (members as? Resource.Success)?.data?.team?.members.orEmpty()))
        }
    }) {
    private val _filter = MutableStateFlow<TaskStatus?>(null)
    val filter: StateFlow<TaskStatus?> = _filter.asStateFlow()

    fun setFilter(status: TaskStatus?) {
        _filter.value = status
    }

    fun setStatus(task: TaskItem, status: TaskStatus) =
        act({ repository.setStatus(task.id, status) }) { updated -> replace(updated) }

    fun create(request: TaskRequest) =
        act({ repository.create(request) }) { created ->
            patch { it.copy(tasks = listOf(created) + it.tasks) }
            send(UiEvent.Message(R.string.task_created))
        }

    fun update(id: String, request: TaskRequest) =
        act({ repository.update(id, request) }) { updated ->
            replace(updated)
            send(UiEvent.Message(R.string.task_saved))
        }

    fun cancel(task: TaskItem) =
        act({ repository.cancel(task.id) }) {
            patch { data -> data.copy(tasks = data.tasks.filterNot { it.id == task.id }) }
            send(UiEvent.Message(R.string.task_cancelled_msg))
        }

    private fun replace(updated: TaskItem) = patch { data -> data.copy(tasks = data.tasks.map { if (it.id == updated.id) updated else it }) }
}

class TaskAdapter(
    private val canChange: (TaskItem) -> Boolean,
    private val isLeader: Boolean,
    private val onStatus: (TaskItem, TaskStatus) -> Unit,
    private val onEdit: (TaskItem) -> Unit,
    private val onCancel: (TaskItem) -> Unit,
) : ListAdapter<TaskItem, TaskAdapter.Holder>(IdDiff { it.id }) {
    class Holder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val task = getItem(position)
        val context = holder.binding.root.context
        with(holder.binding) {
            val done = task.status == TaskStatus.DONE
            title.text = task.title
            title.paint.isStrikeThruText = done
            meta.text =
                if (done) {
                    context.getString(R.string.task_meta_done, task.assigneeName, task.completedAt.asShortDate())
                } else {
                    context.getString(R.string.task_meta_due, task.assigneeName, task.dueDate?.asShortDate() ?: "—")
                }
            status.applyPill(task.status.pill())
            check.setImageResource(if (done) R.drawable.ic_check_circle else R.drawable.ic_circle_outline)
            ImageViewCompat.setImageTintList(check, ColorStateList.valueOf(ContextCompat.getColor(context, if (done) R.color.primary else R.color.text_secondary)))
            check.setOnClickListener { onStatus(task, if (done) TaskStatus.TODO else TaskStatus.DONE) }
            more.setOnClickListener { anchor ->
                PopupMenu(context, anchor).apply {
                    menu.add(0, 1, 0, R.string.task_todo).isEnabled = canChange(task)
                    menu.add(0, 2, 1, R.string.task_in_progress).isEnabled = canChange(task)
                    menu.add(0, 3, 2, R.string.task_done).isEnabled = canChange(task)
                    if (isLeader) {
                        menu.add(0, 4, 3, R.string.edit_task)
                        menu.add(0, 5, 4, R.string.cancel_task)
                    }
                    setOnMenuItemClickListener {
                        when (it.itemId) {
                            1 -> onStatus(task, TaskStatus.TODO)
                            2 -> onStatus(task, TaskStatus.IN_PROGRESS)
                            3 -> onStatus(task, TaskStatus.DONE)
                            4 -> onEdit(task)
                            5 -> onCancel(task)
                        }
                        true
                    }
                }.show()
            }
        }
    }
}

class TasksFragment : GradFragment() {
    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private val me: User? get() = container.session.user
    private val isLeader get() = me?.role == Role.LEADER
    private val viewModel by appViewModels { TasksViewModel(it.tasks, it.team, it.session.user?.role == Role.LEADER) }
    private var adapter: TaskAdapter? = null
    private var members: List<TeamMember> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter =
            TaskAdapter(
                canChange = { isLeader || it.assigneeId == me?.id },
                isLeader = isLeader,
                onStatus = { task, status -> if (task.status != status) viewModel.setStatus(task, status) },
                onEdit = { showTaskDialog(it) },
                onCancel = ::confirmCancel,
            )
        binding.tasks.adapter = adapter
        binding.refresh.setOnRefreshListener { viewModel.refresh() }
        binding.addTask.setVisible(false)
        binding.addTask.setOnClickListener { showTaskDialog(null) }
        binding.filters.setOnCheckedStateChangeListener { _, ids ->
            viewModel.setFilter(
                when (ids.firstOrNull()) {
                    R.id.filterTodo -> TaskStatus.TODO
                    R.id.filterProgress -> TaskStatus.IN_PROGRESS
                    R.id.filterDone -> TaskStatus.DONE
                    else -> null
                },
            )
        }

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) {
                members = state.data.members
                bind(state.data.tasks, viewModel.filter.value)
            }
        }
        collect(viewModel.filter) { filter -> (viewModel.state.value as? UiState.Success)?.let { bind(it.data.tasks, filter) } }
        collect(viewModel.refreshing) { binding.refresh.isRefreshing = it }
        observeEvents(viewModel)
    }

    private fun bind(tasks: List<TaskItem>, filter: TaskStatus?) {
        binding.statTotal.bind(getString(R.string.stat_total_tasks), tasks.size.toString(), getString(R.string.stat_for_this_project))
        val inProgress = tasks.count { it.status == TaskStatus.IN_PROGRESS }
        val done = tasks.count { it.status == TaskStatus.DONE }
        binding.statProgress.bind(getString(R.string.task_in_progress), inProgress.toString(), getString(R.string.stat_keep_moving))
        binding.statDone.bind(getString(R.string.stat_completed), done.toString(), getString(R.string.stat_percent_complete, if (tasks.isEmpty()) 0 else done * 100 / tasks.size))
        val shown = if (filter == null) tasks else tasks.filter { it.status == filter }
        adapter?.submitList(shown)
        binding.empty.setVisible(shown.isEmpty())
        // With no tasks yet the empty state carries the "Add task" button; the floating one appears from the first task.
        binding.addTask.setVisible(isLeader && members.isNotEmpty() && tasks.isNotEmpty())
        when {
            members.isEmpty() && isLeader ->
                binding.empty.bind(R.drawable.ic_team, getString(R.string.empty_tasks_no_team_title), getString(R.string.empty_tasks_no_team_body), getString(R.string.go_to_team), onPrimary = {
                    androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.teamFragment)
                })
            tasks.isEmpty() && isLeader ->
                binding.empty.bind(R.drawable.ic_check_circle, getString(R.string.empty_tasks_title), getString(R.string.empty_tasks_leader_body), getString(R.string.add_task), onPrimary = { showTaskDialog(null) })
            tasks.isEmpty() -> binding.empty.bind(R.drawable.ic_check_circle, getString(R.string.empty_tasks_title), getString(R.string.empty_tasks_member_body))
            else -> binding.empty.bind(R.drawable.ic_check_circle, getString(R.string.empty_filter_title), getString(R.string.empty_filter_body))
        }
    }

    private fun confirmCancel(task: TaskItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cancel_task)
            .setMessage(getString(R.string.cancel_task_confirm, task.title))
            .setNegativeButton(R.string.keep_task, null)
            .setPositiveButton(R.string.cancel_task) { _, _ -> viewModel.cancel(task) }
            .show()
    }

    /** Add (task == null) or edit a task. Leader only; the server enforces the same rule. */
    private fun showTaskDialog(task: TaskItem?) {
        val dialogBinding = DialogTaskBinding.inflate(LayoutInflater.from(requireContext()))
        val names = members.map { it.fullName }
        dialogBinding.assignee.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names))
        var dueDate: String? = task?.dueDate
        task?.let {
            dialogBinding.title.setText(it.title)
            dialogBinding.description.setText(it.description)
            members.firstOrNull { m -> m.id == it.assigneeId }?.let { m -> dialogBinding.assignee.setText(m.fullName, false) }
        }
        dialogBinding.dueDate.setText(dueDate?.asDate() ?: "")
        dialogBinding.dueDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.field_due_date).build()
            picker.addOnPositiveButtonClickListener { millis ->
                dueDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                dialogBinding.dueDate.setText(dueDate.asDate())
            }
            picker.show(childFragmentManager, "due_date")
        }

        val dialog =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(if (task == null) R.string.add_task else R.string.edit_task)
                .setView(dialogBinding.root)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = dialogBinding.title.text?.toString().orEmpty().trim()
                val assignee = members.firstOrNull { it.fullName == dialogBinding.assignee.text?.toString() }
                dialogBinding.titleLayout.error = if (title.isEmpty()) getString(R.string.error_required) else null
                dialogBinding.assigneeLayout.error = if (assignee == null) getString(R.string.error_required) else null
                if (title.isEmpty() || assignee == null) return@setOnClickListener
                val request = TaskRequest(title, dialogBinding.description.text?.toString()?.trim()?.ifEmpty { null }, assignee.id, dueDate)
                if (task == null) viewModel.create(request) else viewModel.update(task.id, request)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        _binding = null
    }
}

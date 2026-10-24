package com.gradtrack.app.ui.notifications

import com.gradtrack.app.ui.common.GradFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gradtrack.app.R
import com.gradtrack.app.data.model.NotificationItem
import com.gradtrack.app.data.repository.NotificationRepository
import com.gradtrack.app.databinding.FragmentNotificationsBinding
import com.gradtrack.app.databinding.ItemNotificationBinding
import com.gradtrack.app.ui.common.IdDiff
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.applyPill
import com.gradtrack.app.ui.common.asRelativeTime
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.icon
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.common.setVisible

class NotificationsViewModel(private val repository: NotificationRepository) : LoadableViewModel<List<NotificationItem>>({ repository.list() }) {
    fun markAllRead() =
        act({ repository.markAllRead() }) {
            patch { list -> list.map { it.copy(read = true) } }
        }
}

class NotificationAdapter : ListAdapter<NotificationItem, NotificationAdapter.Holder>(IdDiff { it.id }) {
    class Holder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            icon.setImageResource(item.type.icon())
            title.text = item.title
            body.text = item.body
            type.applyPill(item.type.pill())
            time.text = item.createdAt.asRelativeTime(root.context)
            unread.setVisible(!item.read)
        }
    }
}

class NotificationsFragment : GradFragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { NotificationsViewModel(it.notifications) }
    private val adapter = NotificationAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.adapter = adapter
        binding.empty.bind(R.drawable.ic_notifications, getString(R.string.empty_notifications_title), getString(R.string.no_notifications))
        binding.refresh.setOnRefreshListener { viewModel.refresh() }
        binding.markAllRead.setOnClickListener { viewModel.markAllRead() }

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) {
                adapter.submitList(state.data)
                binding.empty.setVisible(state.data.isEmpty())
                binding.markAllRead.setVisible(state.data.any { !it.read })
            }
        }
        collect(viewModel.refreshing) { binding.refresh.isRefreshing = it }
        collect(viewModel.busy) { binding.markAllRead.isEnabled = !it }
        observeEvents(viewModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

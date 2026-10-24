package com.gradtrack.app.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gradtrack.app.data.model.ActivityItem
import com.gradtrack.app.data.model.Deadline
import com.gradtrack.app.data.model.ProposalSummary
import com.gradtrack.app.R
import com.gradtrack.app.databinding.ItemActivityBinding
import com.gradtrack.app.databinding.ItemDeadlineBinding
import com.gradtrack.app.databinding.ItemProposalBinding
import com.gradtrack.app.ui.common.IdDiff
import com.gradtrack.app.ui.common.applyPill
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.asRelativeTime
import com.gradtrack.app.ui.common.dueLabel
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.data.model.ProjectStatus
import com.gradtrack.app.ui.common.PillStyle

class DeadlineAdapter : ListAdapter<Deadline, DeadlineAdapter.Holder>(IdDiff { it.id }) {
    class Holder(val binding: ItemDeadlineBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemDeadlineBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            title.text = item.title
            date.text = item.date.asDate()
            val label = item.date.dueLabel(root.context)
            due.text = label
            due.applyPill(
                when (label) {
                    root.context.getString(R.string.due_overdue) -> PillStyle(0, R.color.pill_red_fg, R.color.pill_red_bg)
                    root.context.getString(R.string.due_soon) -> PillStyle(0, R.color.pill_orange_fg, R.color.pill_orange_bg)
                    else -> PillStyle(0, R.color.pill_gray_fg, R.color.pill_gray_bg)
                },
            )
        }
    }
}

class ActivityAdapter : ListAdapter<ActivityItem, ActivityAdapter.Holder>(IdDiff { it.id }) {
    class Holder(val binding: ItemActivityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            title.text = item.title
            body.text = item.body
            time.text = item.createdAt.asRelativeTime(root.context)
        }
    }
}

class ProposalAdapter(private val onClick: (ProposalSummary) -> Unit) : ListAdapter<ProposalSummary, ProposalAdapter.Holder>(IdDiff { it.id }) {
    class Holder(val binding: ItemProposalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemProposalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            title.text = item.title
            meta.text = root.context.getString(R.string.proposal_meta, item.teamName, item.submittedAt.asDate())
            status.applyPill(item.status.pill())
            root.setOnClickListener { onClick(item) }
        }
    }
}

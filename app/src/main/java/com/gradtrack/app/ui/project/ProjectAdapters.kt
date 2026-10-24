package com.gradtrack.app.ui.project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.content.res.ColorStateList
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Feedback
import com.gradtrack.app.data.model.Milestone
import com.gradtrack.app.data.model.MilestoneStatus
import com.gradtrack.app.data.model.StepState
import com.gradtrack.app.data.model.TimelineStep
import com.gradtrack.app.databinding.ItemFeedbackBinding
import com.gradtrack.app.databinding.ItemTimelineBinding
import com.gradtrack.app.ui.common.IdDiff
import com.gradtrack.app.ui.common.applyPill
import com.gradtrack.app.ui.common.asDate
import com.gradtrack.app.ui.common.pill

/** One row of a vertical timeline: a state icon on a connecting line, a title and an optional pill. */
class TimelineHolder(val binding: ItemTimelineBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(title: String, subtitle: String?, state: StepState, isLast: Boolean) {
        val (icon, tint) =
            when (state) {
                StepState.DONE -> R.drawable.ic_check_circle to R.color.primary
                StepState.CURRENT -> R.drawable.ic_clock to R.color.pill_yellow_fg
                StepState.WAITING -> R.drawable.ic_circle_outline to R.color.text_secondary
            }
        binding.indicator.setImageResource(icon)
        ImageViewCompat.setImageTintList(binding.indicator, ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, tint)))
        binding.title.text = title
        binding.subtitle.text = subtitle
        binding.subtitle.isVisible = !subtitle.isNullOrBlank()
        binding.line.isVisible = !isLast
        binding.pill.isVisible = false
    }
}

class MilestoneAdapter : ListAdapter<Milestone, TimelineHolder>(IdDiff { it.id }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TimelineHolder(ItemTimelineBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TimelineHolder, position: Int) {
        val item = getItem(position)
        holder.bind(
            item.name,
            item.dueDate.asDate(),
            when (item.status) {
                MilestoneStatus.COMPLETED -> StepState.DONE
                MilestoneStatus.IN_PROGRESS -> StepState.CURRENT
                MilestoneStatus.UPCOMING -> StepState.WAITING
            },
            position == itemCount - 1,
        )
        holder.binding.pill.isVisible = true
        holder.binding.pill.applyPill(item.status.pill())
    }
}

class StepAdapter : ListAdapter<TimelineStep, TimelineHolder>(IdDiff { it.label }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TimelineHolder(ItemTimelineBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TimelineHolder, position: Int) {
        val item = getItem(position)
        val context = holder.binding.root.context
        holder.bind(
            item.label,
            context.getString(
                when (item.state) {
                    StepState.DONE -> R.string.step_completed
                    StepState.CURRENT -> R.string.step_in_progress
                    StepState.WAITING -> R.string.step_waiting
                },
            ),
            item.state,
            position == itemCount - 1,
        )
    }
}

class FeedbackAdapter : ListAdapter<Feedback, FeedbackAdapter.Holder>(IdDiff { it.id }) {
    class Holder(val binding: ItemFeedbackBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemFeedbackBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            avatar.bind(item.authorName, null)
            author.text = item.authorName
            date.text = item.createdAt.asDate()
            body.text = item.body
        }
    }
}

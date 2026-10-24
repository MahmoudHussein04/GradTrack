package com.gradtrack.app.ui.milestones

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Submission
import com.gradtrack.app.databinding.ItemSubmissionBinding
import com.gradtrack.app.ui.common.IdDiff
import com.gradtrack.app.ui.common.applyPill
import com.gradtrack.app.ui.common.asShortDate
import com.gradtrack.app.ui.common.pill

/** Deliverable rows. With [showTeam] (supervisor view) the row names the team; [onClick] makes rows tappable. */
class SubmissionAdapter(
    private val showTeam: Boolean = false,
    private val onClick: ((Submission) -> Unit)? = null,
) : ListAdapter<Submission, SubmissionAdapter.Holder>(IdDiff { it.id }) {
    class Holder(val binding: ItemSubmissionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemSubmissionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val context = holder.binding.root.context
        with(holder.binding) {
            name.text = item.fileName
            meta.text =
                if (item.submittedAt == null) {
                    context.getString(R.string.not_submitted_yet)
                } else if (showTeam) {
                    listOfNotNull(item.teamName, item.type, item.submittedAt.asShortDate()).joinToString(" · ")
                } else {
                    listOfNotNull(item.submittedAt.asShortDate(), item.reviewerName).joinToString(" · ")
                }
            status.applyPill(item.status.pill())
            root.isClickable = onClick != null
            root.setOnClickListener(if (onClick != null) { _ -> onClick.invoke(item) } else null)
        }
    }
}

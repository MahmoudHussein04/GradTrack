package com.gradtrack.app.ui.team

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gradtrack.app.R
import com.gradtrack.app.data.model.TeamMember
import com.gradtrack.app.databinding.ItemMemberBinding
import com.gradtrack.app.ui.common.IdDiff
import com.gradtrack.app.ui.common.PillStyle
import com.gradtrack.app.ui.common.applyPill

/** Member row. [onRemove] is non-null only for the team leader, and never offered for the leader's own row. */
class TeamMemberAdapter(private val onRemove: ((TeamMember) -> Unit)? = null) : ListAdapter<TeamMember, TeamMemberAdapter.Holder>(IdDiff { it.id }) {
    class Holder(val binding: ItemMemberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val member = getItem(position)
        with(holder.binding) {
            avatar.bind(member.fullName, member.avatarUrl)
            name.text = member.fullName
            meta.text = listOfNotNull(member.universityId, member.roleLabel).joinToString(" · ")
            status.applyPill(
                if (member.status.equals("Active", ignoreCase = true)) {
                    PillStyle(0, R.color.pill_green_fg, R.color.pill_green_bg)
                } else {
                    PillStyle(0, R.color.pill_gray_fg, R.color.pill_gray_bg)
                },
            )
            status.text = member.status
            remove.isVisible = onRemove != null && !member.isLeader
            remove.setOnClickListener { onRemove?.invoke(member) }
        }
    }
}

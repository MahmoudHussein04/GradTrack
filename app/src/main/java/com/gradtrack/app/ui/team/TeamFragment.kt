package com.gradtrack.app.ui.team

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Invitation
import com.gradtrack.app.data.model.MyTeam
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.TeamInfo
import com.gradtrack.app.data.model.TeamMember
import com.gradtrack.app.data.repository.TeamRepository
import com.gradtrack.app.databinding.FragmentTeamBinding
import com.gradtrack.app.databinding.ItemInvitationBinding
import com.gradtrack.app.ui.common.GradFragment
import com.gradtrack.app.ui.common.IdDiff
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.asRelativeTime
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.container
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.promptText
import com.gradtrack.app.ui.common.setVisible

class TeamViewModel(private val repository: TeamRepository) : LoadableViewModel<MyTeam>({ repository.myTeam() }) {
    fun create(name: String) = act({ repository.create(name) }) { send(UiEvent.Message(R.string.team_created)); refresh() }

    fun accept(invitation: Invitation) = act({ repository.accept(invitation.id) }) { send(UiEvent.Message(R.string.joined_team)); refresh() }

    fun decline(invitation: Invitation) = act({ repository.decline(invitation.id) }) { refresh() }

    fun invite(universityId: String) =
        act({ repository.invite(universityId) }) {
            patch { it.copy(team = it.team?.copy(pendingInvites = it.team.pendingInvites + 1)) }
            send(UiEvent.Message(R.string.invitation_sent))
        }

    fun remove(member: TeamMember) =
        act({ repository.remove(member.id) }) {
            patch { mine -> mine.copy(team = mine.team?.let { team -> team.copy(members = team.members.filterNot { it.id == member.id }) }) }
            send(UiEvent.Message(R.string.member_removed))
        }
}

class InvitationAdapter(
    private val onAccept: (Invitation) -> Unit,
    private val onDecline: (Invitation) -> Unit,
) : ListAdapter<Invitation, InvitationAdapter.Holder>(IdDiff { it.id }) {
    class Holder(val binding: ItemInvitationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemInvitationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            avatar.bind(item.teamName, null)
            teamName.text = item.teamName
            meta.text = root.context.getString(R.string.invited_by, item.leaderName, item.createdAt.asRelativeTime(root.context))
            accept.setOnClickListener { onAccept(item) }
            decline.setOnClickListener { onDecline(item) }
        }
    }
}

class TeamFragment : GradFragment() {
    private var _binding: FragmentTeamBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { TeamViewModel(it.team) }
    private var memberAdapter: TeamMemberAdapter? = null
    private val invitationAdapter = InvitationAdapter(onAccept = { viewModel.accept(it) }, onDecline = { viewModel.decline(it) })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isLeader = container.session.user?.role == Role.LEADER
        memberAdapter = TeamMemberAdapter(if (isLeader) ::confirmRemove else null)
        binding.members.adapter = memberAdapter
        binding.invitations.adapter = invitationAdapter
        binding.refresh.setOnRefreshListener { viewModel.refresh() }
        binding.invite.setVisible(isLeader)
        binding.invite.setOnClickListener {
            promptText(R.string.invite_member, R.string.hint_university_id, R.string.send_invitation, multiline = false) { viewModel.invite(it) }
        }

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) bind(state.data, isLeader)
        }
        collect(viewModel.refreshing) { binding.refresh.isRefreshing = it }
        collect(viewModel.busy) { binding.invite.isEnabled = !it }
        observeEvents(viewModel)
    }

    private fun bind(data: MyTeam, isLeader: Boolean) {
        val team = data.team
        binding.teamGroup.setVisible(team != null)
        binding.noTeamGroup.setVisible(team == null)
        if (team != null) {
            bindTeam(team, isLeader)
            return
        }
        binding.heading.setText(R.string.team_heading)
        // Leaders create a team; students wait for an invitation from one.
        binding.idCard.setVisible(!isLeader)
        binding.myUniversityId.text = container.session.user?.universityId
        binding.invitationsTitle.setVisible(!isLeader && data.invitations.isNotEmpty())
        binding.invitations.setVisible(!isLeader)
        invitationAdapter.submitList(if (isLeader) emptyList() else data.invitations)
        binding.noTeamEmpty.setVisible(isLeader || data.invitations.isEmpty())
        if (isLeader) {
            binding.noTeamEmpty.bind(
                R.drawable.ic_team,
                getString(R.string.empty_create_team_title),
                getString(R.string.empty_create_team_body),
                getString(R.string.create_team),
                onPrimary = ::promptCreateTeam,
            )
        } else {
            binding.noTeamEmpty.bind(R.drawable.ic_mail, getString(R.string.empty_invitations_title), getString(R.string.empty_invitations_body))
        }
    }

    private fun bindTeam(team: TeamInfo, isLeader: Boolean) {
        binding.heading.text = team.name
        binding.statMembers.bind(getString(R.string.stat_members), "${team.members.size}/${team.maxMembers}", getString(if (team.members.size >= team.maxMembers) R.string.team_complete else R.string.team_has_room), R.drawable.ic_team)
        binding.statRole.bind(getString(R.string.stat_your_role), getString(if (isLeader) R.string.role_leader_short else R.string.role_member_short), null, R.drawable.ic_badge)
        binding.statInvites.bind(getString(R.string.stat_invites), team.pendingInvites.toString(), null, R.drawable.ic_mail)
        binding.invite.setVisible(isLeader && team.members.size + team.pendingInvites < team.maxMembers)
        memberAdapter?.submitList(team.members)
        binding.detailProject.text = team.projectTitle ?: getString(R.string.not_registered_yet)
        binding.detailSupervisor.text = team.supervisorName ?: "—"
        binding.detailSemester.text = team.semester ?: "—"
    }

    private fun promptCreateTeam() {
        promptText(R.string.create_team, R.string.hint_team_name, R.string.create, multiline = false, numeric = false) { viewModel.create(it) }
    }

    private fun confirmRemove(member: TeamMember) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.remove_member)
            .setMessage(getString(R.string.remove_member_confirm, member.fullName))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.remove) { _, _ -> viewModel.remove(member) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        memberAdapter = null
        _binding = null
    }
}

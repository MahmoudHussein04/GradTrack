package com.gradtrack.app.ui.profile

import com.gradtrack.app.ui.common.GradFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.model.UpdateProfileRequest
import com.gradtrack.app.data.model.User
import com.gradtrack.app.data.repository.AuthRepository
import com.gradtrack.app.databinding.FragmentProfileBinding
import com.gradtrack.app.ui.common.BaseViewModel
import com.gradtrack.app.ui.common.PillStyle
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.applyPill
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.container
import com.gradtrack.app.ui.common.hideKeyboard
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.setVisible
import kotlinx.coroutines.launch

class ProfileViewModel(private val auth: AuthRepository) : BaseViewModel() {
    fun save(request: UpdateProfileRequest) = act({ auth.updateProfile(request) }) { send(UiEvent.Done(R.string.profile_saved)) }

    fun changePassword(current: String, new: String) = act({ auth.changePassword(current, new) }) { send(UiEvent.Done(R.string.password_updated)) }

    fun logout() {
        viewModelScope.launch {
            auth.logout()
            send(UiEvent.Done())
        }
    }
}

class ProfileFragment : GradFragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { ProfileViewModel(it.auth) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val user = container.session.user ?: return
        val supervisor = user.role == Role.SUPERVISOR
        // Everyone signs in with a university ID; only students have an academic year.
        binding.academicYearLayout.setVisible(!supervisor)
        bindHeader(user)
        if (savedInstanceState == null) fillForm(user)

        binding.saveProfile.setOnClickListener { saveProfile(user) }
        binding.updatePassword.setOnClickListener { updatePassword() }
        binding.logout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.log_out)
                .setMessage(R.string.log_out_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.log_out) { _, _ -> viewModel.logout() }
                .show()
        }

        collect(viewModel.busy) { busy ->
            binding.saveProfile.isEnabled = !busy
            binding.updatePassword.isEnabled = !busy
            binding.logout.isEnabled = !busy
        }
        observeEvents(viewModel) { event ->
            val user = container.session.user
            when {
                user == null -> findNavController().navigate(R.id.action_global_login)
                event.message == R.string.password_updated -> {
                    binding.currentPassword.text = null
                    binding.newPassword.text = null
                }
                else -> bindHeader(user)
            }
        }
    }

    private fun bindHeader(user: User) {
        binding.avatar.bind(user.fullName, user.avatarUrl)
        binding.name.text = user.fullName
        binding.email.text = user.email
        binding.role.setText(
            when (user.role) {
                Role.STUDENT -> R.string.role_student
                Role.LEADER -> R.string.role_team_leader
                Role.SUPERVISOR -> R.string.role_supervisor_full
            },
        )
        binding.role.applyPill(PillStyle(0, R.color.pill_green_fg, R.color.pill_green_bg))
    }

    private fun fillForm(user: User) {
        binding.fullName.setText(user.fullName)
        binding.universityId.setText(user.universityId)
        binding.emailInput.setText(user.email)
        binding.department.setText(user.department)
        binding.academicYear.setText(user.academicYear)
    }

    private fun saveProfile(user: User) {
        val name = binding.fullName.text?.toString().orEmpty().trim()
        val email = binding.emailInput.text?.toString().orEmpty().trim()
        binding.fullNameLayout.error = if (name.isEmpty()) getString(R.string.error_required) else null
        binding.emailLayout.error = if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) getString(R.string.error_email) else null
        if (binding.fullNameLayout.error != null || binding.emailLayout.error != null) return
        hideKeyboard()
        viewModel.save(
            UpdateProfileRequest(
                fullName = name,
                email = email,
                department = binding.department.text?.toString().orEmpty().trim(),
                academicYear = if (user.role == Role.SUPERVISOR) "" else binding.academicYear.text?.toString().orEmpty().trim(),
            ),
        )
    }

    private fun updatePassword() {
        val current = binding.currentPassword.text?.toString().orEmpty()
        val new = binding.newPassword.text?.toString().orEmpty()
        binding.currentPasswordLayout.error = if (current.isEmpty()) getString(R.string.error_required) else null
        binding.newPasswordLayout.error = if (new.length < 8) getString(R.string.error_password_short) else null
        if (binding.currentPasswordLayout.error != null || binding.newPasswordLayout.error != null) return
        hideKeyboard()
        viewModel.changePassword(current, new)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
